/**
* Various Methods to run external processes and scripts from the commandline.
* At its core, its a js-friendly process library leveraging functions from Java.
*/
var System = Java.type('java.lang.System');
var Thread = Java.type('java.lang.Thread');
var ProcessBuilder = Java.type('java.lang.ProcessBuilder');
var Redirect = ProcessBuilder.Redirect;
var Charset = Java.type('java.nio.charset.Charset');

// Streams
var Appendable = Java.type('java.lang.Appendable');
var AutoCloseable = Java.type('java.lang.AutoCloseable');
var InputStream = Java.type('java.io.InputStream');
var OutputStream = Java.type('java.io.OutputStream');
var Reader = Java.type('java.io.Reader');
var Writer = Java.type('java.io.Writer');
var InputStreamReader = Java.type('java.io.InputStreamReader');
var OutputStreamWriter = Java.type('java.io.OutputStreamWriter');
var BufferedWriter = Java.type('java.io.BufferedWriter');
var BufferedReader = Java.type('java.io.BufferedReader');
var StringReader = Java.type('java.io.StringReader');

// Files
var File = Java.type('java.io.File');
var Path = Java.type('java.nio.file.Path');
var Files = Java.type('java.nio.file.Files');
var Paths = Java.type('java.nio.file.Paths');

// Internal functions
var charset = Charset.defaultCharset();
function isString(string){
	return typeof string === 'string';
}

/**
* Copies all data from an input streamlike resource to the output streamlike resource.
* Used for piped-io with processes.<br/>
* Please note that this method currently is entirely focussed on char-data and cannot cope
* with binary data in any way. Neither line-endings on different os'es or charsets are supported,
* instead always the system default is used. That's by design for now...
* <p>
* Note as well that is implementation is horribly slow as it is, but this really shouldn't matter here,
* since this will rarely be the bottleneck in the IPC.
*/
function copyStream(input, output, lineSeparator){
	// convert the input in multiple steps until its a Reader
	if (Array.isArray(input) && input.every(isString)){	
		input = input.join(lineSeparator);
	}
	if (isString(input)){
		input = new StringReader(input);
	}
	if (input instanceof InputStream){
		input = new InputStreamReader(input);
	}
	if (!(input instanceof Reader)){
		throw new Error('Invalid Input: ' + input);
	}
	input = new BufferedReader(input);
	
	// convert and check the output for the best writing strategy
	if (output instanceof OutputStream){
		output = new OutputStreamWriter(output);
	}
	var writeMethod;
	var line;
	if (Array.isArray(output)){
		writeMethod = function(){
			output.push(line);
		}
	} else if (output instanceof Appendable){
		writeMethod = function(){
			output.append(line);
			output.append(lineSeparator);
			if (output instanceof Writer){
				output.flush();
			}
		}
	} else {
		throw new Error('Invalid Output: ' + output);
	}
	// IN -> OUT loop
	while (true){
		line = input.readLine();
		if (line == null){
			break;
		}
		writeMethod();
		// synchronized will make this slower than it has to be,
		// but is required on a line-basis to make sure
		// that multiple processes won't interfere with each other
		Java.synchronized(writeMethod, output);
	}
}

/**
* Primary Object: A ProcessExecutor Factory, simplified in many ways for shell-scripting.
* Its main properties are dir (workdir), in, out, err.
*/
function Shell() {

	// private members
	var dir = null;
	var input = null;
	var output = null;
	var error = null;
	
	/**
	* The line separator used by the external process.
	* Used especially for sending piped input to the process,
	* but ignored in most other cases.
	*/
	this.lineSeparator = System.getProperty('line.separator');
	
	// public members	
	Object.defineProperty(this, 'dir', {
		/**
		* the currently set directory, or null if the default workdir should be used.
		*/
		get: function(){
			return dir == null ? null : dir.toString();
		},
		
		/**
		* Modifies the working directory for all processes started afterwards. Setting it to null will restore the default.
		* @param the work dir
		*/
		set: function(newDir){
			if (newDir == null){
				dir = null;
			} else if (dir == null){				
				dir = Paths.get(newDir).toAbsolutePath();
			} else {
				dir = dir.resolve(newDir).toAbsolutePath();
			}
		}
	});
	Object.defineProperty(this, 'in', {
	
		/**
		* the default input or null if stdin should be used.
		*/
		get: function(){
			return input;
		},
	
		/**
		* Sets the input for the next processes started afterwards.
		* Note that the first process will usually consume this resource, so reusing this does not make much sense.
		* @param the default input as a Reader, Array or String. null if stdin should be used.
		*/
		set: function(value){
			if (value === null ||
				value instanceof File ||
				value instanceof Path ||
				value instanceof Reader ||
				Array.isArray(value) ||
				isString(value)){
				input = value;
			} else {
				throw new Error('Illegal input type: ' + value);
			}
		}
	});
	Object.defineProperty(this, 'out', {
	
		/**
		* the default output or null if stdout should be used.
		*/
		get: function(){
			return output;
		},
		
		/**
		* Sets the output for the next processes started afterwards.
		* @param the default output as an Appendable or Array (no String!). All writes will be synchronized. null if stdout should be used.
		*/
		set: function(value){
			if (value === null ||
				value instanceof File ||
				value instanceof Path ||
				value instanceof OutputStream ||
				value instanceof Writer ||
				Array.isArray(value) ||
				isString(value)){
				output = value;
			} else {
				throw new Error('Illegal output type: ' + value);
			}
		}
	});
	Object.defineProperty(this, 'err', {
	
		/**
		* the default error or null if stderr should be used.
		*/
		get: function(){
			return error;
		},
		
		/**
		* Sets the error for the next processes started afterwards.
		* @param the default error as an Appendable or Array (no String!). All writes will be synchronized. null if stderr should be used.
		*/
		set: function(value){
			if (value === null ||
				value instanceof File ||
				value instanceof Path ||
				value instanceof OutputStream ||
				value instanceof Writer ||
				Array.isArray(value) ||
				isString(value)){
				error = value;
			} else {
				throw new Error('Illegal output type: ' + value);
			}
		}
	});
	
	/**
	* Creates the appropiate redirect, dependening on the target type.
	*/
	var createRedirect = function(target, read){
		if (target instanceof Path){
			target = target.toFile();
		}
		if (target instanceof File){
			return read ? Redirect.from(target) : Redirect.appendTo(target);			
		}
		return target ? Redirect.PIPE : Redirect.INHERIT;
	}
	
	/**
	* Launches the given command, which can be a String or a String[] directly from the commandline.
	* IN, OUT, and ERR are redirected according to the configuration of this object, so by default all is sent to stdin, -out and -err.
	* @return a new process instance which allows waitFor() and destroy().
	*/
	this.start = function(commandline){
		var args;
		if (Array.isArray(commandline)){
			args = Java.to(commandline, 'java.lang.String[]');
		}else {
			args = commandline.toString();
		}
		/*if (exec.contains('/') || exec.contains('\\')){
			exec = resolve(exec).toRealPath().toString();
		}
		var args = [exec].concat(args);*/
		var processBuilder = new ProcessBuilder(args);
		if (dir){
			processBuilder.directory(dir.toFile());
		}
		
		var rIn = createRedirect(input, true);
		var rOut = createRedirect(output, false);
		var rErr = createRedirect(error, false);
		
		processBuilder.redirectInput(rIn).redirectOutput(rOut).redirectError(rErr);
		
		var process = processBuilder.start();
		var lineSeparator = this.lineSeparator;
		if (rIn === Redirect.PIPE){
			var procIn = input;
			new Thread(function(){
				copyStream(procIn, process.getOutputStream(), lineSeparator);
			}).start();
			input = null;
		}
		if (rOut === Redirect.PIPE){
			new Thread(function(){
				copyStream(process.getInputStream(), output, lineSeparator);
			}).start();			
		}
		if (rErr === Redirect.PIPE){
			new Thread(function(){
				copyStream(process.getErrorStream(), error, lineSeparator);
			}).start();
		}
		return {
			destroy: function(){
				process.destroy();
			},
			waitFor: function(){
				return process.waitFor();
			}
		};
	}
	
	/**
	* Launches the given command, which can be a String or a String[], directly from the commandline and waits for its completion.
	* IN, OUT, and ERR are redirected according to the configuration of this object, so by default all is sent to stdin, -out and -err.
	* @return the exit code of the command.
	*/
	this.exec = function(exec, args){
		var process = this.start(exec, args);
		return process.waitFor();
	}
	
	/**
	* Executes multiple commands as a script file from the commandline.
	* @param script the command script as String, String[], File oder Path.
	* @return a new process instance which allows waitFor() and destroy().
	*/
	this.startScript = function(script){	
		if (isString(script)){
			script = [script];
		}
		if (Array.isArray(script) && script.every(isString)){
			var file = Files.createTempFile(null, null);
			file.deleteOnExit();
			Files.write(file, Java.to(script), charset);			
			script = file;
		}
		if (script instanceof File){
			script = file.toPath();
		}
		if (!(script instanceof Path)){
			throw new Error('Invalid script: ' + script);
		}
		script = script.toAbsolutePath();
		if (!Files.isRegularFile(script)){
			throw new Error('File does not exist: ' + script);
		}
		// windows-only variant!
		return start('cmd', '/c', script);
	}
	
	/**
	* Executes multiple commands as a script file from the commandline.
	* @param script the command script as String, String[], File oder Path.
	* @return the exit code of the command.
	*/
	this.execScript = function(script){
		var process = this.startScript(script);
		return process.waitFor();
	}
};

module.exports = Shell;
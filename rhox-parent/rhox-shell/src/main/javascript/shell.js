/**
 * Various Methods to run external processes and scripts from the commandline.
 * At its core, its a js-friendly process library leveraging functions from
 * Java.
 */

var classpath = require('rhox-classpath');
var execModule = classpath.createModule();
execModule.include(__dirname + '/lib/*.jar');

var RhoxShell = execModule.type('com.rhox.shell.RhoxShell');
var ProcessConfig = execModule.type('com.rhox.shell.ProcessConfig');
var JsProcessContext = execModule.type('com.rhox.shell.MapProcessContext');
var Redirect = execModule.type('com.rhox.shell.ProcessRedirect');

/**
 * Binds all given String properties from this object to the other.
 */
function bindProperties(_this, _that, properties) {
	properties.forEach(function(property) {
		Object.defineProperty(_this, property, {
			get : function() {
				return _that[property];
			},
			set : function(value) {
				_that[property] = value;
			}
		});
	});
}

/**
 * Binds all given String properties for reading from this object to the other.
 */
function bindReadonlyProperties(_this, _that, properties) {
	properties.forEach(function(property) {
		Object.defineProperty(_this, property, {
			get : function() {
				return _that[property];
			}
		});
	});
}

/**
 * A thin jswrapper around a java process object.
 */
function Process(javaProcess) {

	// Properties
	bindReadonlyProperties(this, javaProcess, [ 'alive', 'in', 'out', 'err' ]);
	Object.defineProperty(this, 'exitValue', {
		get : function() {
			return javaProcess.exitValue();
		}
	});

	/**
	 * Destroys the process.
	 */
	this.destroy = function() {
		javaProcess.destroy();
	};

	/**
	 * Waits for the proess to terminate.
	 * 
	 * @return the exit code.
	 */
	this.waitFor = function() {
		return javaProcess.waitFor();
	};

	/**
	 * Waits for the proess to terminate or does it by itself.
	 * 
	 * @return the exit code.
	 */
	this.waitForOrKill = function(millis) {
		return javaProcess.waitForOrKill(millis);
	};
}

/**
 * Primary Object: A ProcessExecutor Factory, simplified in many ways for
 * shell-scripting. Its main properties are dir (workdir), in, out, err.
 */
function Shell() {

	var javaShell = new RhoxShell();

	bindProperties(this, javaShell, [ 'dir', 'in', 'out', 'err', 'redirectErr',
			'lineSeparator', 'charset' ]);

	// Additional setters as in MapProcessContext
	Object.defineProperty(this, '&out', {
		set : function(out) {
			this.out = out;
			this.redirectErr = true;
		}
	});
	Object.defineProperty(this, '>>', {
		set : function(file) {
			this.out = Redirect.appendTo(file);
		}
	});
	Object.defineProperty(this, '&>>', {
		set : function(file){
			this['>>'] = file;
			this.redirectErr = true;
		}
	});
	Object.defineProperty(this, '>', {
		set : function(file){
			this.out = Redirect.to(file);
		}
	});
	Object.defineProperty(this, '&>', {
		set : function(file){
			this['>'] = file;
			this.redirectErr = true;
		}
	});
	Object.defineProperty(this, '<', {
		set : function(file){
			this['in'] = Redirect.from(file);
		}
	});
	
	/**
	 * Launches the given command, which can be a String or a String[] directly
	 * from the commandline. IN, OUT, and ERR are redirected according to the
	 * configuration of this object, so by default all is sent to stdin, -out
	 * and -err.
	 * 
	 * @return a new process instance which allows waitFor() and destroy().
	 */
	this.start = function(command, config) {
		config = config ? new JsProcessContext(config) : null;
		var javaProcess = javaShell.start(command, config);
		return new Process(javaProcess);
	};

	/**
	 * Launches the given command, which can be a String or a String[], directly
	 * from the commandline and waits for its completion. IN, OUT, and ERR are
	 * redirected according to the configuration of this object, so by default
	 * all is sent to stdin, -out and -err.
	 * 
	 * @return the exit code of the command.
	 */
	this.exec = function(command, config) {
		var process = this.start(command, config);
		return process.waitFor();
	};
}

/**
 * Internal default system shell - used for the global functions
 */
var SystemShell = new Shell();

// Exports

/**
* Shell class to create a shell with custom values
*/
exports.Shell = Shell;

/**
* Direct access to the redirect options.
*/
exports.Redirect = Redirect;

/**
 * Starts a new Process using the System defaults.
 */
exports.start = SystemShell.start.bind(SystemShell);

/**
 * Executes an waits for a Process using the System defaults.
 */
exports.exec =  SystemShell.exec.bind(SystemShell);;
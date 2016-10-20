/**
 * Various Methods to run external processes and scripts from the commandline.
 * At its core, its a js-friendly process library leveraging functions from
 * Java.
 */

var classpath = require('rhox-classpath');
var execModule = classpath.createModule();
execModule.include(__dirname + '/lib/*.jar');

var JsRhoxShell = Java.type('com.rhox.exec.JsRhoxShell');

/**
 * A thin jswrapper around a java process object.
 */
function Process(javaProcess) {

	/**
	 * Destroys the process.
	 */
	this.destroy = function() {
		javaProcess.destroy();
	};

	/**
	 * Waits for the proess to terminate and returns the exit code.
	 */
	this.waitFor = function() {
		return javaProcess.waitFor();
	};
}

/**
 * Primary Object: A ProcessExecutor Factory, simplified in many ways for
 * shell-scripting. Its main properties are dir (workdir), in, out, err.
 */
function Shell() {

	var rhoxShell = new JsRhoxShell();

	// delegating public members
	Object.defineProperty(this, 'dir', {
		get : function() {
			return rhoxShell.getDir();
		},
		set : function(value) {
			rhoxShell.setDir(value);
		}
	});
	Object.defineProperty(this, 'lineSeparator', {
		get : function() {
			return rhoxShell.getLineSeparator();
		},
		set : function(value) {
			rhoxShell.setLineSeparator(value);
		}
	});
	Object.defineProperty(this, 'charset', {
		get : function() {
			return rhoxShell.getCharset();
		},
		set : function(value) {
			rhoxShell.setCharset(value);
		}
	});
	var input = null;
	Object.defineProperty(this, 'in', {
		get : function() {
			return input;
		},
		set : function(value) {
			input = value;
			rhoxShell.setIn(value);
		}
	});
	var output = null;
	Object.defineProperty(this, 'out', {
		get : function() {
			return output;
		},
		set : function(value) {
			output = value;
			rhoxShell.setOut(value);
		}
	});
	var error = null;
	Object.defineProperty(this, 'err', {
		get : function() {
			return error;
		},
		set : function(value) {
			error = value;
			rhoxShell.setErr(value);
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
	this.start = function(command) {
		var javaProcess = rhoxShell.start(commandline);
		return new Process(javaProcess);
	}

	/**
	 * Launches the given command, which can be a String or a String[], directly
	 * from the commandline and waits for its completion. IN, OUT, and ERR are
	 * redirected according to the configuration of this object, so by default
	 * all is sent to stdin, -out and -err.
	 * 
	 * @return the exit code of the command.
	 */
	this.exec = function(command) {
		var process = this.start(command);
		return process.waitFor();
	}

	/**
         * Copies some kind of input into some kind of output.
	 */
	this.copy = function(input, output) {
            return rhoxShell.writeFile(input, file);
	}
};

module.exports = Shell;
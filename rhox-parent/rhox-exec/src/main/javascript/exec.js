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
    this.destroy = function () {
        javaProcess.destroy();
    };

    /**
     * Waits for the proess to terminate and returns the exit code.
     */
    this.waitFor = function () {
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
        get: function () {
            return rhoxShell.dir;
        },
        set: function (value) {
            rhoxShell.dir = value;
        }
    });
    Object.defineProperty(this, 'lineSeparator', {
        get: function () {
            return rhoxShell.lineSeparator;
        },
        set: function (value) {
            rhoxShell.lineSeparator = value;
        }
    });
    Object.defineProperty(this, 'charset', {
        get: function () {
            return rhoxShell.charset;
        },
        set: function (value) {
            rhoxShell.charset = value;
        }
    });
    Object.defineProperty(this, 'in', {
        get: function () {
            return rhoxShell.in;
        },
        set: function (value) {
            rhoxShell.in = value;
        }
    });
    Object.defineProperty(this, 'out', {
        get: function () {
            return rhoxShell.out;
        },
        set: function (value) {
            rhoxShell.out = value;
        }
    });
    Object.defineProperty(this, 'err', {
        get: function () {
            return rhoxShell.err;
        },
        set: function (value) {
            rhoxShell.err = value;
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
    this.start = function (command) {
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
    this.exec = function (command) {
        var process = this.start(command);
        return process.waitFor();
    }

    /**
     * Copies some kind of input into some kind of output.
     */
    this.copy = function (input, output) {
        return rhoxShell.writeFile(input, output);
    }
}
;

module.exports = Shell;
/**
 * Various Methods to run external processes and scripts from the commandline.
 * At its core, its a js-friendly process library leveraging functions from
 * Java.
 */

var classpath = require('rhox-classpath');
var execModule = classpath.createModule();
execModule.include(__dirname + '/lib/*.jar');

var RhoxShell = Java.type('com.rhox.exec.RhoxShell');
var ProcessConfig = Java.type('com.rhox.exec.ProcessConfig');
var MapProcessContext = Java.type('com.rhox.exec.MapProcessContext');
var ProcessRedirect = Java.type('com.rhox.exec.ProcessRedirect');

/**
 * Binds all given String properties from this object to the other.
 */
function bindProperties(_this, _that, properties) {
    properties.forEach(function (property) {
        Object.defineProperty(_this, property, {
            get: function () {
                return _that[property];
            },
            set: function (value) {
                _that[property] = value;
            }
        });
    });
}

/**
 * Binds all given String properties for reading from this object to the other.
 */
function bindReadonlyProperties(_this, _that, properties) {
    properties.forEach(function (property) {
        Object.defineProperty(_this, property, {
            get: function () {
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
    bindReadonlyProperties(this, javaProcess, ['alive', 'in', 'out', 'err']);
    Object.defineProperty(this, 'exitValue', {
        get: function () {
            return javaProcess.exitValue();
        }
    });

    /**
     * Destroys the process.
     */
    this.destroy = function () {
        javaProcess.destroy();
    };

    /**
     * Waits for the proess to terminate.
     * 
     * @return the exit code.
     */
    this.waitFor = function () {
        return javaProcess.waitFor();
    };

    /**
     * Waits for the proess to terminate or does it by itself.
     * 
     * @return the exit code.
     */
    this.waitForOrKill = function (millis) {
        return javaProcess.waitForOrKill(millis);
    };
}

/**
 * Primary Object: A ProcessExecutor Factory, simplified in many ways for
 * shell-scripting. Its main properties are dir (workdir), in, out, err.
 */
function Shell() {

    var javaShell = new RhoxShell();

    bindProperties(this, javaShell, ['dir', 'in', 'out', 'err', 'redirectErr',
        'lineSeparator', 'charset']);

    /**
     * Launches the given command, which can be a String or a String[] directly
     * from the commandline. IN, OUT, and ERR are redirected according to the
     * configuration of this object, so by default all is sent to stdin, -out
     * and -err.
     * 
     * @return a new process instance which allows waitFor() and destroy().
     */
    this.start = function (command, config) {
        config = config ? new MapProcessContext(config) : null;
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
    this.exec = function (command, config) {
        var process = this.start(command, config);
        return process.waitFor();
    };
}

// Internal singleton shell implementation
var SystemShell = new Shell();
var System = {
    exec: function (command, config) {
        return SystemShell.exec(command, config);
    },
    start: function (command, config) {
        return SystemShell.start(command, config);
    }
};

// Global functions and objects
exports.Shell = Shell;
exports.Redirect = ProcessRedirect;
exports.System = System;
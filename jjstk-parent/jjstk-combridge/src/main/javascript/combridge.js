var jclasspath = require('jjstk-jclasspath');

var libfiles = jclasspath.requirePath.resolve(__dirname, 'lib/*.jar');
var javaModule = new jclasspath.JavaModule(libfiles);
var JsCom = javaModule.type('com.jjstk.combridge.JsCom');

exports.newComObject = function (name) {
    return JsCom.connect(name);
};
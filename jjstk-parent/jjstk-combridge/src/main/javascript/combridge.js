var jclasspath = require('jclasspath');

jclasspath.requirePath('lib/*.jar');

var JsCom = Java.type('com.jjstk.combridge.JsCom');

exports.create = function (name) {
    return JsCom.connect(name);
};
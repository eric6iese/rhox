var jclasspath = require('jjstk-jclasspath');

jclasspath.requirePath(__dirname, 'lib/*.jar');

var JsCom = Java.type('com.jjstk.combridge.JsCom');

exports.newComObject = function (name) {
    return JsCom.connect(name);
};
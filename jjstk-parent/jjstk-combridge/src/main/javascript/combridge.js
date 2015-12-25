var jclasspath = require('jjstk-jclasspath');
jclasspath.requireArtifact('net.java.dev.jna:jna-platform:4.2.1');
jclasspath.requirePath(__dirname, 'lib/*.jar');

var JsCom = Java.type('com.jjstk.combridge.JsCom');

exports.newComObject = function (name) {
    return JsCom.connect(name);
};
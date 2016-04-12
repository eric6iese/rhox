var jclasspath = require('rhox-classpath');
jclasspath.requireArtifact('net.java.dev.jna:jna-platform:4.2.1');
jclasspath.requirePath(__dirname, 'lib/*.jar');

exports.ComObject = Java.type('com.rhox.combridge.ComObject');
exports.ComDispatch = Java.type('com.rhox.combridge.ComDispatch');
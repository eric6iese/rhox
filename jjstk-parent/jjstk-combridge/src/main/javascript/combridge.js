var jclasspath = require('jjstk-jclasspath');
jclasspath.requireArtifact('net.java.dev.jna:jna-platform:4.2.1');
jclasspath.requirePath(__dirname, 'lib/*.jar');

exports.ComObject = Java.type('com.jjstk.combridge.ComObject');
exports.ComDispatch = Java.type('com.jjstk.combridge.ComDispatch');
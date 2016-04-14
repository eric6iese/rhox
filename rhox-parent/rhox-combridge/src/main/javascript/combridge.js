var classpath = require('rhox-classpath');
var module = new classpath.JavaModule(__dirname + '/lib/*.jar');

exports.ComObject = module.type('com.rhox.combridge.ComObject');
exports.ComDispatch = module.type('com.rhox.combridge.ComDispatch');
var classpath = require('rhox-classpath');

var comModule = classpath.createModule();
comModule.include(__dirname + '/lib/*.jar');


exports.toString = function () {
    return "Native{}";
};
exports.ComObject = comModule.type('com.rhox.combridge.ComObject');
exports.ComDispatch = comModule.type('com.rhox.combridge.ComDispatch');
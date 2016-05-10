var classpath = require('rhox-classpath');

var nativeModule = classpath.createModule();
nativeModule.include(__dirname + '/lib/*.jar');

exports.win32 = {
    ComObject: nativeModule.type('com.rhox.natives.win32.ComObject'),
    ComDispatch: nativeModule.type('com.rhox.natives.win32.ComDispatch')
};

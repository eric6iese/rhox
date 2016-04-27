load(java.lang.System.getenv('NASHORN_GLOBALS'));

var classpath = require("rhox-classpath");
var maven = require("rhox-maven");
var native = require("rhox-native");


var out = Java.type('java.lang.System').out;

function printType(type) {
    out.println("TYPE: " + type);
    for (var x in type) {
        out.println("--" + x + ": " + type[x]);
    }
    out.println();
}

[classpath, maven, native].forEach(printType);
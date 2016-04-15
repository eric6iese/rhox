load(java.lang.System.getProperty('user.home') + '/rhox/bootstrap.js');


var Logger = Java.type('java.util.logging.Logger');
var Level = Java.type('java.util.logging.Level');
// Set the root logger with its handlers
var rootLogger = Logger.getLogger("");
rootLogger.setLevel(Level.ALL);
Java.from(rootLogger.getHandlers()).forEach(function (h) {
    h.setLevel(Level.ALL);
});
Logger.getLogger("com.rhox.classpath").setLevel(Level.ALL);

// Enable http wire logging by setting its logger to info!
// does not work? i dont get anything out?
Logger.getLogger("org.apache.http.wire").setLevel(Level.FINEST);
Logger.getLogger("org.apache.http").setLevel(Level.FINEST);

// disable rhox logging for fun
// Logger.getLogger("com.rhox.classpath").setLevel(Level.SEVERE);

var System = java.lang.System;
var out = System.out;
out.println(System.getProperty("user.dir"));

try {
    var classpath = require("rhox-classpath");
    out.println("" + classpath);
    for (var x in classpath){
        out.println(x + "=" + classpath[x]);
    }
    var files = classpath.requirePath.resolve(mavenDir + "/lib/slf4j*.jar");
    out.println("" + files);
    var maven = require("rhox-maven");
} catch (e) {
    if (e.cause) {
        e.cause.printStackTrace();
    }
    throw e;
}

maven.requireArtifact('junit:junit:4.11');

// does not load again!
maven.requireArtifact('junit:junit:4.11');

var Assert = Java.type("org.junit.Assert");

Assert.assertTrue(true);

out.println("Assert works");

var Throwables;
try {
    Throwables = Java.type("com.google.common.base.Throwables")
    Assert.fail("Cannot load internal classes of the dependency classloader!");
} catch (expected) {
    out.println("No access to guava - ok!");
}

maven.requireArtifact('com.google.guava:guava:19.0');
Throwables = Java.type("com.google.common.base.Throwables");
out.println("Throwables ist nach dem manuellen Laden von guava verfügbar!");
Assert.assertTrue(true);

var mavenDir = java.nio.file.Paths.get(require.resolve('rhox-maven')).getParent();
out.println(mavenDir);

var slf4jFile = mavenDir + "/lib/slf4j*.jar";
out.println("Lade " + slf4jFile);
classpath.requirePath(slf4jFile);
var files = classpath.requirePath.resolve(slf4jFile);
out.println("Dependencies: ");
files.forEach(function (f) {
    out.println(f);
});

var log = org.slf4j.LoggerFactory.getLogger("hello");

log.info("hello");
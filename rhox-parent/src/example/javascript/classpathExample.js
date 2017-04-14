load(java.lang.System.getenv('NASHORN_GLOBALS'));

var Logger = Java.type('java.util.logging.Logger');
var Level = Java.type('java.util.logging.Level');
// Set the root logger with its handlers
var rootLogger = Logger.getLogger("");
rootLogger.setLevel(Level.INFO);
Java.from(rootLogger.getHandlers()).forEach(function (h) {
    h.setLevel(Level.ALL);
});
Logger.getLogger("com.rhox").setLevel(Level.FINE);

// Enable http wire logging by setting its logger to info!
// does not work? i dont get anything out?
// Logger.getLogger("org.apache.http.wire").setLevel(Level.FINEST);
// Logger.getLogger("org.apache.http").setLevel(Level.FINEST);

// disable rhox logging for fun
// Logger.getLogger("com.rhox.classpath").setLevel(Level.SEVERE);

var System = java.lang.System;
var out = System.out;
out.println("Hello and welcome in " + System.getProperty("user.dir"));


try {
    var classpath = require("rhox-classpath");
    var files = classpath.include.resolve(mavenDir + "/lib/slf4j*.jar");
    var maven = require("rhox-maven");
} catch (e) {
    if (e.cause) {
        e.cause.printStackTrace();
    }
    throw e;
}

out.println("Now load junit");
maven.include({group: 'junit', name: 'junit', version: '4.11'});

out.println("Does not load again!");
maven.include({group: 'junit', name: 'junit', version: '4.11'});

out.println("not even like this");
maven.include('junit:junit:4.11');

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

maven.include('com.google.guava:guava:19.0');
Throwables = Java.type("com.google.common.base.Throwables");
out.println("Throwables ist nach dem manuellen Laden von guava verfügbar!");
Assert.assertTrue(true);

var mavenDir = java.nio.file.Paths.get(require.resolve('rhox-maven')).getParent();
out.println(mavenDir);

var slf4jFile = mavenDir + "/lib/slf4j*.jar";
out.println("Lade " + slf4jFile);
classpath.include(slf4jFile);
var files = classpath.include.resolve(slf4jFile);
out.println("Dependencies: ");
files.forEach(function (f) {
    out.println(f);
});

out.println("--- TEST RESOLVING MULTIPLE DEPENDENCIES AT ONCE!");
maven.include('com.google.guava:guava:19.0',
        'junit:junit:4.11',
        {group: 'junit', name: 'junit', version: '4.11'},
        {group: 'org.springframework.data', name: 'spring-data-jpa', version: '1.8.0.RELEASE'});

var log = Java.type("org.slf4j.LoggerFactory").getLogger("hello");

log.info("WELL DONE!");
load(java.lang.System.getProperty('user.home') + '/rhox/bootstrap.js');


// Set the root logger to all
java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.ALL);

// Enable http wire logging by setting its logger to info!
// does not work? i dont get anything out?
java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.FINEST);

// disable rhox logging for fun
java.util.logging.Logger.getLogger("com.rhox.classpath").setLevel(java.util.logging.Level.SEVERE);

var System = java.lang.System;
System.out.println(System.getProperty("user.dir"));

try {
    var jclasspath = require("rhox-classpath");
    var maven = require("rhox-maven");
} catch (e) {
    if (e.cause){
        e.cause.printStackTrace();    
    }
    throw e;
}

maven.requireArtifact('junit:junit:4.11');

// does not load again!
maven.requireArtifact('junit:junit:4.11');

var Assert = Java.type("org.junit.Assert");

Assert.assertTrue(true);

System.out.println("Assert works");

var Throwables;
try {
    Throwables = Java.type("com.google.common.base.Throwables")
    Assert.fail("Cannot load internal classes of the dependency classloader!");
} catch (expected) {
    System.out.println("No access to guava - ok!");
}

maven.requireArtifact('com.google.guava:guava:19.0');
Throwables = Java.type("com.google.common.base.Throwables");
System.out.println("Throwables ist nach dem manuellen Laden von guava verfügbar!");
Assert.assertTrue(true);

var jclassPathDir = java.nio.file.Paths.get(require.resolve('rhox-classpath')).getParent();
System.out.println(jclassPathDir);

maven.requirePath(jclassPathDir, "lib/slf4j*.jar");

var files = maven.requirePath.resolve(jclassPathDir, "lib/slf4j*.jar");
System.out.println("Dependencies:");
files.forEach(function (f) {
    System.out.println(f);
});


var log = org.slf4j.LoggerFactory.getLogger("hello");

log.info("hello");
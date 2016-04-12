load(java.lang.System.getProperty('user.home') + '/rhox/bootstrap.js');

var System = java.lang.System;
System.out.println(System.getProperty("user.dir"));

try {
    var jclasspath = require("rhox-classpath");
} catch (e) {
    e.cause.printStackTrace();
}

jclasspath.requireArtifact('junit:junit:4.11');

// does not load again!
jclasspath.requireArtifact('junit:junit:4.11');

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

jclasspath.requireArtifact('com.google.guava:guava:19.0');
Throwables = Java.type("com.google.common.base.Throwables");
System.out.println("Throwables ist nach dem manuellen Laden von guava verfügbar!");
Assert.assertTrue(true);

var jclassPathDir = java.nio.file.Paths.get(require.resolve('rhox-classpath')).getParent();
System.out.println(jclassPathDir);

jclasspath.requirePath(jclassPathDir, "lib/slf4j*.jar");

var files = jclasspath.requirePath.resolve(jclassPathDir, "lib/slf4j*.jar");
System.out.println("Dependencies:");
files.forEach(function (f) {
    System.out.println(f);
});


var log = org.slf4j.LoggerFactory.getLogger("hello");

log.info("hello");
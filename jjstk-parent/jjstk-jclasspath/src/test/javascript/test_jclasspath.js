load(java.lang.System.getProperty('user.home') + '/jjstk/bootstrap.js');

var System = java.lang.System;
System.out.println(System.getProperty("user.dir"));

try {
    var jclasspath = require("../../../target/jjstk-jclasspath/jclasspath.js");
} catch (e) {
    e.cause.printStackTrace();
}

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

jclasspath.requirePath("lib/slf4j*.jar");

var log = org.slf4j.LoggerFactory.getLogger("hello");

log.info("hello");
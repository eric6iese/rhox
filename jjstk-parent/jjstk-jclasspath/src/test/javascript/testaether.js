load(java.lang.System.getProperty("user.home") + "/jjstk/bootstrap.js");

var jclasspath = require("../../../target/jautoit-aether/aether.js");

jclasspath.requireJar('junit:junit:4.11');

var System = Java.type("java.lang.System");
var Assert = Java.type("org.junit.Assert");

Assert.assertTrue(true);

System.out.println("Assert works");

var Throwables;
try {
   Throwables = Java.type("com.google.common.base.Throwables")
   Assert.fail("Cannot load internal classes of the dependency classloader!");
}catch (expected){
   System.out.println("No access to guava - ok!");
}

jclasspath.requireJar('com.google.guava:guava:19.0');
Throwables =  Java.type("com.google.common.base.Throwables");
System.out.println("Throwables ist nach dem Laden manuellen Laden von guava verfügbar!");

Assert.fail("Fails as expected!");

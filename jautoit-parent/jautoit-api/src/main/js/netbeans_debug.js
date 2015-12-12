/**
 * Just a test: Load a class by hacking the system classloader.
 */
var System = java.lang.System;
var ClassLoader = java.lang.ClassLoader;
var Paths = java.nio.file.Paths;
var URL = java.net.URL;
var URLClassLoader = java.net.URLClassLoader;

// Add JavaPoet in SystemClassLoader
var home = System.getProperty("user.home");
var file = Paths.get(home, ".m2/repository", "com/squareup/javapoet/1.4.0/javapoet-1.4.0.jar")
var url = file.toFile().toURI().toURL();
var data = url.openStream();    
var sysloader = ClassLoader.getSystemClassLoader(); 
var method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
method.setAccessible(true);
method.invoke(sysloader, url);

projectBase = Paths.get("../../..").toAbsolutePath().toString();
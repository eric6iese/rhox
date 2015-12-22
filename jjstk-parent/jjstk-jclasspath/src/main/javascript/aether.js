
/**
 * Load all jars of the distribution in a separate classloader to avoid version
 * collisions.
 */
var dependencyManager = (function(){
    var className = "de.evermind.scriptmaster.aether.DependencyManager";
	var libDir = new java.io.File( __DIR__, "lib");
    if (!libDir.isDirectory()){
    	throw new Error("Cannot find directory " + libDir + "!");
    }
        
	var libJars = Java.from(libDir.listFiles()).filter(function(f) f.getName().endsWith(".jar") || f.isDirectory());
	var libUrls = libJars.map(function(f) f.toURI().toURL());

        // Create the separate ClassLoader and finally load the class
	var classLoader = new java.net.URLClassLoader(libUrls);
	var clazz = classLoader.loadClass(className);
	return clazz.newInstance();
}());


/**
 * Resolves the given argument as a dependency in the local workspace.
 */
exports.requireJar = function(dependency) {
	var files = dependencyManager.resolve([dependency]);
	dependencyManager.load(files);
}
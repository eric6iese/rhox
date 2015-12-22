// Imports
var File = java.io.File;
var URLClassLoader = java.net.URLClassLoader;

/**
 * Load all jars of the distribution in a separate classloader to avoid version
 * collisions.
 */
var className = "de.evermind.scriptmaster.aether.DependencyManager";
var libDir = new File(__dirname, "lib");
if (!libDir.isDirectory()){
    throw new Error("Cannot find directory " + libDir + "!");
}

var libJars = Java.from(libDir.listFiles()).filter(function(f) f.getName().endsWith(".jar") || f.isDirectory());
var libUrls = libJars.map(function(f) f.toURI().toURL());

// Create the separate ClassLoader and finally load the class
var classLoader = new java.net.URLClassLoader(libUrls);
var clazz = classLoader.loadClass(className);
	
var dependencyManager = clazz.newInstance();

/**
 * Resolves the given argument as a jarfile to load in the local workspace.
 */
exports.require = function(file){
    dependencyManager.load([new File(file)]);
}

/**
 * Resolves the given argument as a dependency in the local workspace.
 */
exports.requireArtifact = function(dependency) {
	var files = dependencyManager.resolve([dependency]);
	dependencyManager.load(files);
}
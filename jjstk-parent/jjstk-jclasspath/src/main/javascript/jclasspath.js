// Imports
var File = java.io.File;
var URLClassLoader = java.net.URLClassLoader;
var URL = java.net.URL;
var Thread = java.lang.Thread;

var mAddUrl = null;

/**
 * Loads associated resolved Dependencies into the classpath.
 */
var requireAll = function (files) {
    if (mAddUrl === null) {
        mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        mAddUrl.setAccessible(true);
    }
    var classLoader = Thread.currentThread().getContextClassLoader();

    var urls = {};
    Java.from(classLoader.getURLs()).forEach(function (it) {
        urls[it.toString()] = true;
    });
    files.forEach(function (file) {
        var url = file.toURI().toURL();
        if (urls[url.toString()]) {
            return;
        }
        mAddUrl.invoke(classLoader, url);
    });
};

/**
 * Resolves the given argument as a jarfile to load in the local workspace.
 */
exports.require = function (file) {
    requireAll([new File(file)]);
};

var dependencyManager = null;

/**
 * Resolves the given argument as a dependency in the local workspace.
 */
exports.requireArtifact = function (dependency) {
    if (dependencyManager === null) {
        // Load all jars of the distribution in a separate classloader to avoid version collisions.
        var className = "de.evermind.scriptmaster.aether.DependencyManager";
        var libDir = new File(__dirname, "lib");
        if (!libDir.isDirectory()) {
            throw new Error("Cannot find directory " + libDir + "!");
        }

        var libJars = Java.from(libDir.listFiles()).filter(function (f) {
            return f.getName().endsWith(".jar") || f.isDirectory();
        });
        var libUrls = libJars.map(function (f) {
            return f.toURI().toURL();
        });

        // Create the separate ClassLoader and finally load the class
        var classLoader = new URLClassLoader(libUrls);
        var clazz = classLoader.loadClass(className);

        dependencyManager = clazz.newInstance();
    }
    var files = dependencyManager.resolve([dependency]);
    requireAll(files);
};
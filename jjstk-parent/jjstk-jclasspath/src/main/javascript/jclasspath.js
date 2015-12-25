// Imports
var Class = java.lang.Class;
var File = java.io.File;
var Paths = java.nio.file.Paths;
var Files = java.nio.file.Files;
var URLClassLoader = java.net.URLClassLoader;
var URL = java.net.URL;
var Thread = java.lang.Thread;

/**
 * A JavaModule is a separate unit which encapsulates the results of a given classloader or set of urls.
 * @param files the classpath array consisting of folder and jar file (strings)
 */
var JavaModule = function (files, parentClassLoader) {
    var libUrls = files.map(function (f) {
        return new File(f).toURI().toURL();
    });
    if (parentClassLoader === undefined) {
        this.classLoader = new URLClassLoader(libUrls);
    } else {
        this.classLoader = new URLClassLoader(libUrls, parentClassLoader);
    }
};
/**
 * Resolves the given class using the module's internal classloader.
 * Works pretty much the same as the java.type function, but for the module loader instead.
 */
JavaModule.prototype.type = function (className) {
    var clazz = Class.forName(className, true, this.classLoader);
    return clazz.static;
};

var mAddUrl = null;
/**
 * Loads associated resolved Dependencies into the classpath.
 * @param files an array of strings with the filenames
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
        var url = new File(file).toURI().toURL();
        if (urls[url.toString()]) {
            return;
        }
        mAddUrl.invoke(classLoader, url);
    });
};

var resolvePath = function (dirname, pattern) {
    var files = [];
    var dir = Paths.get("" + dirname);
    pattern = "glob:" + pattern;
    var matcher = dir.getFileSystem().getPathMatcher(pattern);
    var stream = Files.walk(dir);
    try {
        stream.forEach(function (path) {
            var p = dir.relativize(path);
            if (!matcher.matches(p)) {
                return;
            }
            files.push(path.toAbsolutePath().toString());
        });
    } finally {
        stream.close();
    }
    return files;
};

var dependencyManager = null;
var resolveArtifact = function (dependency) {
    if (dependencyManager === null) {
        // Load all jars of the distribution in a separate classloader to avoid version collisions.
        var className = "com.jjstk.jclasspath.DependencyManager";
        var libDir = new File(__dirname, "lib");
        if (!libDir.isDirectory()) {
            throw new Error("Cannot find directory " + libDir + "!");
        }

        var libFiles = Java.from(libDir.listFiles()).filter(function (f) {
            return f.getName().endsWith(".jar") || f.isDirectory();
        }).map(function (f) {
            return f.getAbsolutePath();
        });

        // Create the separate ClassLoader and finally load the class
        var mavenModule = new JavaModule(libFiles);
        var DependencyManager = mavenModule.type(className);
        dependencyManager = new DependencyManager();
    }
    var files = dependencyManager.resolve([dependency]);
    return Java.from(files).map(function (f) {
        return f.getAbsolutePath();
    });
};

// Exports

/**
 * Resolves the given argument as a jarfile to load in the local workspace.
 */
var requirePath = function (dirname, pattern) {
    var files = resolvePath(dirname, pattern);
    requireAll(files);
};
requirePath.resolve = resolvePath;

/**
 * Resolves the given argument as a dependency in the local workspace.
 */
var requireArtifact = function (dependency) {
    var files = resolveArtifact(dependency);
    requireAll(files);
};
requireArtifact.resolve = resolveArtifact;


exports.JavaModule = JavaModule;
exports.requirePath = requirePath;
exports.requireArtifact = requireArtifact;
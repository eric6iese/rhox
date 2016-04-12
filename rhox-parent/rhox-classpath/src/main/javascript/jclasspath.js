// Imports
var Class = Java.type('java.lang.Class');
var File = Java.type('java.io.File');
var Paths = Java.type('java.nio.file.Paths');
var Files = Java.type('java.nio.file.Files');
var URLClassLoader = Java.type('java.net.URLClassLoader');
var URL = Java.type('java.net.URL');
var Thread = Java.type('java.lang.Thread');

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

/**
 * The dependency cache tracks all known dependency ids to prevent duplicate classloading
 * In the same way, the classloaders each check individually which urls have already been loaded, to
 * prevent them from being loaded again later on.
 */
var dependencyCache = {};

/** Reflect-Hook for the add-url method. */
var methodAddUrl = null;

/**
 * Loads associated resolved Dependencies into the classpath.
 * @param files an array of strings with the filenames
 */
var requireAll = function (files) {
    if (methodAddUrl === null) {
        methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        methodAddUrl.setAccessible(true);
    }
    var classLoader = Thread.currentThread().getContextClassLoader();

    var urls = {};
    Java.from(classLoader.getURLs()).forEach(function (it) {
        urls[it.toString()] = true;
    });
    files.map(function (file) {
        // Check maven dependency cache if input is an artifact
        var id = file.artifact;
        if (id === undefined) {
            // not a maven dependency, go on
            return file;
        }
        if (dependencyCache[id]) {
            // skip: Dependency has already been loaded
            return null;
        }
        // add depency to cache and continue with the file part only
        dependencyCache[id] = true;
        return file.file;
    }).forEach(function (file) {
        if (file === null) {
            return;
        }
        // Check file url
        var url = new File(file).toURI().toURL();
        if (urls[url.toString()]) {
            return;
        }
        // load the directory / jar
        methodAddUrl.invoke(classLoader, url);
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
        var className = "com.rhox.classpath.DependencyManager";
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
    var artifacts = [];
    var artifactMap = dependencyManager.resolve([dependency]);
    artifactMap.forEach(function (id, file) {
        artifacts.push({artifact: id, file: file});
    });
    return artifacts;
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
    var artifacts = resolveArtifact(dependency);
    requireAll(artifacts);
};

exports.JavaModule = JavaModule;
exports.requirePath = requirePath;
exports.requireArtifact = requireArtifact;
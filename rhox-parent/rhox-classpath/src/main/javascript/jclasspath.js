// Imports
var Class = Java.type('java.lang.Class');
var File = Java.type('java.io.File');
var Paths = Java.type('java.nio.file.Paths');
var Files = Java.type('java.nio.file.Files');
var URL = Java.type('java.net.URL');
var URLClassLoader = Java.type('java.net.URLClassLoader');
var Thread = Java.type('java.lang.Thread');
var ClassNotFoundException = Java.type('java.lang.ClassNotFoundException');
var Logger = Java.type('java.util.logging.Logger');

var log = Logger.getLogger('com.rhox.classpath');

/**
 * The dependency cache tracks all known dependency ids to prevent duplicate classloading
 * In the same way, the classloaders each check individually which urls have already been loaded, to
 * prevent them from being loaded again later on.
 */
var dependencyCache = {};


/**
 * Resolves the files of the given path pattern.
 * Before the glob check starts, all fragments will be split up to identify those
 * segments where no matching is necessary.
 */
var resolvePath = function (path) {
    // normalize the path to the system dependent separator
    path = path.replace(/\//g, File.separator);
    var parts = path.split(File.separator);
    var i = 0;
    while (i < parts.length && !/[\*\?\[]/.test(parts[i])) {
        i++;
    }
    var dirname = parts.slice(0, i).join(File.separator);
    var dir = Paths.get(dirname);
    log.fine("Resolve-Basefile : " + dir);
    if (i === parts.length) {
        // no pattern found: must be a full path
        return [dir];
    }
    var pattern = parts.slice(i).join(File.separator);
    log.fine("Resolve-Pattern : " + pattern);
    if (!Files.isDirectory(dir)){
        // Search returns nothing if the base of the pattern is not a dir
        return [];
    }
    var files = [];
    var matcher = dir.getFileSystem().getPathMatcher("glob:" + pattern);
    var stream = Files.walk(dir);
    try {
        stream.forEach(function (path) {
            var p = dir.relativize(path);
            if (!matcher.matches(p)) {
                return;
            }
            p = path.toAbsolutePath().toString();
            files.push(p);
        });
    } finally {
        stream.close();
    }
    return files;
};


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

/**
 * A JavaModule is a separate unit which encapsulates the results of a given classloader or set of urls.
 * @param files the classpath array consisting of folder and jar file (strings)
 */
var JavaModule = function (path, parentClassLoader) {
    var files = resolvePath(path);
    this.urls = files.map(function (f) {
        return new File(f).toURI().toURL();
    });
    if (parentClassLoader === undefined) {
        this.classLoader = new URLClassLoader(this.urls);
    } else {
        this.classLoader = new URLClassLoader(this.urls, parentClassLoader);
    }
};
JavaModule.prototype.toString = function () {
    return "Module{" + this.urls + "}";
};

/**
 * Resolves the given class using the module's internal classloader.
 * Works pretty much the same as the java.type function, but for the module loader instead.
 */
JavaModule.prototype.type = function (className) {
    try {
        var clazz = Class.forName(className, true, this.classLoader);
        return clazz.static;
    } catch (ex) {
        throw new ClassNotFoundException("Cannot find " + className + " in " + this.toString(), ex);
    }
};

// Exports

/**
 * Resolves the given argument as a jarfile to load in the local workspace.
 * @param path one of the following
 * <ol>
 * <li>a String denoting a single real path or glob-expression</li>
 * <li>an array of strings for multiple paths or glob-expresions</li>
 * </ol>
 */
var requirePath = function (path) {
    var files;
    if (Array.isArray(path)){
        log.fine(path);
        files = [];
        path.forEach(function(it){
           files = files.concat(resolvePath(it)); 
        });
    }else {
        files = resolvePath(path);
    }
    log.fine("Resolved files: " + files);
    requireAll(files);
};

requirePath.resolve = resolvePath;

exports.JavaModule = JavaModule;
exports.requirePath = requirePath;
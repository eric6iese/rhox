// Imports
var Class = Java.type('java.lang.Class');
var File = Java.type('java.io.File');
var Paths = Java.type('java.nio.file.Paths');
var Files = Java.type('java.nio.file.Files');
var URL = Java.type('java.net.URL');
var URLClassLoader = Java.type('java.net.URLClassLoader');
var Thread = Java.type('java.lang.Thread');

var Logger = Java.type('java.util.logging.Logger');
var Level = Java.type('java.util.logging.Level');
var FINE = Level.FINE;
var log = Logger.getLogger('com.rhox.classpath');

/**
 * If the given function is an arraylike, then return it.
 * Otherwise wrap it in a single-element array.
 */
var toArray = function (value) {
    return Array.isArray(value) ? value : [value];
}

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
    log.log(FINE, "Resolve-Basefile: {0}", dir);
    if (i === parts.length) {
        // no pattern found: must be a full path
        return [dir];
    }
    var pattern = parts.slice(i).join(File.separator);
    log.log(FINE, "Resolve-Pattern : {0}", pattern);
    if (!Files.isDirectory(dir)) {
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

/**
 * A JavaModule is a separate unit which encapsulates the results of a given classloader or set of urls.
 * @param files the classpath array consisting of folder and jar file (strings)
 */
var JavaModule = function () {
    /**
     * The dependency cache tracks all known dependency ids to prevent duplicate classloading
     * In the same way, the classloaders each check individually which urls have already been loaded, to
     * prevent them from being loaded again later on.
     */
    this.dependencyCache = {};
    this._classLoader = null;
};

/**
 * Prints the Files of this classloader
 */
JavaModule.prototype.toString = function () {
    var urls = this._classLoader ? Java.from(this._classLoader.getURLs()) : [];
    return "Module{" + urls + "}";
};

/**
 * Resolves the given class using the module's internal classloader.
 * Works pretty much the same as the java.type function, but for the module loader instead.
 */
JavaModule.prototype.type = function (className) {
    var cl = this._classLoader;
    log.log(Level.FINEST, "Resolve type {0}.", className);
    try {
        var clazz = Class.forName(className, true, this._classLoader);
        return clazz.static;
    } catch (e) {
        log.log(FINE, "Resolving type {0} failed against: {1}", [className, Java.from(cl.getURLs())]);
        throw e;
    }
};

/** Reflect-Hook for the add-url method. */
var methodAddUrl = null;

/**
 * Loads associated resolved Dependencies into the classpath.
 * @param files an array of strings with the filenames
 */
JavaModule.prototype.requireAll = function (files) {
    var cl = this._classLoader;
    if (methodAddUrl === null) {
        methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        methodAddUrl.setAccessible(true);
    }

    var urls = {};
    Java.from(cl.getURLs()).forEach(function (it) {
        urls[it.toString()] = true;
    });
    files.map(function (file) {
        // Check maven dependency cache if input is an artifact
        var id = file.artifact;
        if (id === undefined) {
            // not a maven dependency, go on
            return file;
        }
        if (this.dependencyCache[id]) {
            // skip: Dependency has already been loaded
            return null;
        }
        // add depency to cache and continue with the file part only
        this.dependencyCache[id] = true;
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
        log.log(FINE, "Add URL to classloader {0}: {1}", [cl, url]);
        methodAddUrl.invoke(cl, url);
    });
};


/**
 * Resolves the given argument as a jarfile to load in the local workspace.
 * @param path one of the following
 * <ol>
 * <li>a String denoting a single real path or glob-expression</li>
 * <li>an array of strings for multiple paths or glob-expresions</li>
 * </ol>
 */
JavaModule.prototype.include = function (path) {
    var files = [];
    path = toArray(path);
    log.log(FINE, "Requiring... {0}", path);
    path.forEach(function (it) {
        files = files.concat(resolvePath(it));
    });
    log.log(FINE, "Resolved files: {0}", files);
    this.requireAll(files);
};

JavaModule.prototype.include.resolve = resolvePath;

/**
 * These Modules can be instantiated manually by the RootLoader.
 */
var ChildModule = function (path, parentClassLoader) {
    var files = resolvePath(path);
    var urls = files.map(function (f) {
        return new File(f).toURI().toURL();
    });
    this._classLoader = parentClassLoader ? new URLClassLoader(urls, parentClassLoader) : new URLClassLoader(urls);
};
ChildModule.prototype = new JavaModule();

/**
 * Inspired by groovy, this 'hacks' into java's default url classloader to do some
 * magic. Note that this only ever works in simple scripts as jjs, since many
 * enterprise frameworks (and osgi) tend to manipulate the contextclassloader in such
 * a way that is no longer useable.
 */
var RootModule = function () {
    this._classLoader = Thread.currentThread().getContextClassLoader();
    // some more code could be added which searches a matching url loader in the parent-classloaders
};
RootModule.prototype = new JavaModule();
RootModule.prototype.JavaModule = ChildModule;

module.exports = new RootModule();
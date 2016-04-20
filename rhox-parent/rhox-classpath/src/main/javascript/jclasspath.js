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
var DEBUG = Level.FINE;
var TRACE = Level.FINEST;
var log = Logger.getLogger('com.rhox.classpath');

// FileSystem Constants
var fsPathSeparator = File.pathSeparator;
var fsSeparator = File.separator;

/**
 * Resolves the files of the given path pattern.
 * Before the glob check starts, all fragments will be split up to identify those
 * segments where no matching is necessary.
 */
var resolvePath = function (pathPattern) {
    // normalize the path to the system dependent separator    
    var path = pathPattern.replace(/\//g, fsSeparator);
    var parts = path.split(fsSeparator);
    var i = 0;
    while (i < parts.length && !/[\*\?\[]/.test(parts[i])) {
        i++;
    }
    var dirname = parts.slice(0, i).join(fsSeparator);
    var dir = Paths.get(dirname);
    log.log(TRACE, 'Resolve-Basefile: "{0}"', dir);
    if (i === parts.length) {
        // no pattern found: must be a full path
        return [dir];
    }
    var pattern = parts.slice(i).join(fsSeparator);
    log.log(TRACE, 'Resolve-Pattern : "{0}"', pattern);
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
    if (log.isLoggable(DEBUG)) {
        log.log(DEBUG, 'Pattern "' + pathPattern + '" resolved to ' + files.join(fsPathSeparator));
    }
    return files;
};

/**
 * A JavaModule is a separate unit which encapsulates the results of a given classloader or set of urls.
 * @param files the classpath array consisting of folder and jar file (strings)
 */
var JavaModule = function () {
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
    log.log(TRACE, "Resolve type {0}", className);
    try {
        var clazz = Class.forName(className, true, cl);
        return clazz.static;
    } catch (e) {
        log.log(DEBUG, "Resolving type {0} failed against: {1}", [className, Java.from(cl.getURLs())]);
        throw e;
    }
};

/** Reflection-Hook for the add-url method. */
var methodAddUrl = methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
methodAddUrl.setAccessible(true);

/**
 * Loads associated resolved Dependencies into the classpath.
 * @param classLoader must be an urlclassloader which will load the given files
 * @param files to resolve as varargs
 */
var requireFiles = function (classLoader, files) {
    var urls = {};
    Java.from(classLoader.getURLs()).forEach(function (it) {
        urls[it.toString()] = true;
    });

    // iterate over the urls and add them to the classpath,
    // if they are not already part of the urls map
    files.forEach(function (file) {
        if (file === null) {
            return;
        }
        // Check file url
        var url = new File(file).toURI().toURL();
        if (urls[url.toString()]) {
            return;
        }
        // load the directory / jar
        log.log(DEBUG, "Add URL to classloader {0}: {1}", [classLoader, url]);
        methodAddUrl.invoke(classLoader, url);
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
JavaModule.prototype.include = function () {
    var paths = [];
    for (var i = 0; i < arguments.length; i++) {
        paths[i] = arguments[i];
    }
    log.log(TRACE, "Requiring... {0}", paths.join(fsPathSeparator));
    var files = [];
    paths.forEach(function (it) {
        files = files.concat(resolvePath(it));
    });
    log.log(TRACE, "Resolved files: {0}", files.join(fsPathSeparator));
    requireFiles(this._classLoader, files);
};

JavaModule.prototype.include.resolve = resolvePath;

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

// var emptyUrlArray = new Java.type('java.net.URL[]')(0);
/**
 * These Modules can be instantiated manually by the RootLoader.
 */
var ChildModule = function (parentClassLoader) {
    this._classLoader = parentClassLoader ? new URLClassLoader([], parentClassLoader) : new URLClassLoader([]);
};
ChildModule.prototype = new JavaModule();

/**
 * Creates a new Module as a submodule of the current one.
 * The ChildModule uses the classloader of its parent but defines its own scope,
 * so that all of its includes are not visible to it.<br/>
 * Technically this is of course just the default for all of java classloading when defining a subclassloader.
 */
JavaModule.prototype.createModule = function () {
    return new ChildModule(this._classLoader);
};

module.exports = new RootModule();
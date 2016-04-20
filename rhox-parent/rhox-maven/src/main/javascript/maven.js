var classpath = require('rhox-classpath');

// Create the separate ClassLoader and finally load the class
var mavenModule = classpath.createModule();
mavenModule.include(__dirname + '/lib/*.jar');

var CharSequence = Java.type('java.lang.CharSequence');
var DependencyManager = mavenModule.type("com.rhox.maven.DependencyManager");
var LoggerFactory = mavenModule.type('org.slf4j.LoggerFactory');

var log = LoggerFactory.getLogger('com.rhox.maven');
var dependencyManager = new DependencyManager();

var asNull = function (value) {
    return value === undefined ? null : value;
};

/**
 * Takes an input object an either destructures it or parses it and
 * then creates an aether artifact for resolving out of it.
 */
var toAetherArtifact = function (artifact) {
    if (artifact instanceof CharSequence) {
        var parts = artifact.split(":", 5);
        var i = 0;
        var group = parts[i++];
        var name = parts[i++];
        var version = parts[i++];
        var ext = null;
        var classifier = null;
        if (i < parts.length) {
            ext = version;
            version = parts[i++];
            if (i < parts.length) {
                classifier = version;
                version = parts[i++];
            }
        }
    } else {
        var group = asNull(artifact.group);
        var name = asNull(artifact.name);
        var classifier = asNull(artifact.classifier);
        var ext = asNull(artifact.ext);
        var version = asNull(artifact.version);
    }
    return DependencyManager.newArtifact(group, name, ext, classifier, version);
};

/**
 * Takes an aether artifact and resolves it as a nice-formatted javascript-object
 */
var fromAetherArtifact = function (artifact) {
    return {
        name: artifact.artifactId,
        group: artifact.groupId,
        ext: artifact.extension,
        classifier: artifact.classifier,
        version: artifact.version,
        // only contained in resolved artifacts
        file: artifact.file.getAbsolutePath(),
        toString: function () {
            return [name, group, ext, classifier, version].join(":");
        }
    };
};

// TODO: Create 'real' artifacts like gradle with group, name, version, classifier, extension
// internally for all parsing in javascript (or mirrors of the java-objects)
// resolve then returns these + file attribute like below

/**
 * @param .... the dependencies to resolve as varargs
 */
var resolve = function (varargs) {
    var artifacts = new Array(arguments.length);
    for (var i = 0; i < arguments.length; i++) {
        artifacts[i] = toAetherArtifact(arguments[i]);
    }
    if (log.isDebugEnabled()) {
        log.debug('Resolving "' + artifacts.join('", "') + '"');
    }
    artifacts = Java.from(dependencyManager.resolve(artifacts));
    if (log.isDebugEnabled()) {
        log.debug('Resolved: "' + artifacts.join('", "') + '"');
    }
    artifacts = artifacts.map(fromAetherArtifact);
    return artifacts;
};

/**
 * Resolves the given argument as a dependency in the local workspace.
 * @param .... the dependencies to resolve as varargs
 */
var include = function (varargs) {
    var artifacts = resolve.apply(null, arguments);
    var paths = artifacts.map(function (it) {
        return it.file;
    });
    classpath.include.apply(classpath, paths);
};

exports.toString = function () {
    return "MavenModule{}";
};

exports.resolve = resolve;
exports.include = include;
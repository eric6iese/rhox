var classpath = require('rhox-classpath');

// Create the separate ClassLoader and finally load the class
var javaModule = new classpath.JavaModule(__dirname + '/lib/*.jar');
var DependencyManager = javaModule.type("com.rhox.maven.DependencyManager");
var dependencyManager = new DependencyManager();

var resolveArtifact = function (dependency) {
    var artifacts = [];
    var artifactMap = dependencyManager.resolve([dependency]);
    artifactMap.forEach(function (id, file) {
        artifacts.push({artifact: id, file: file});
    });
    return artifacts;
};

/**
 * Resolves the given argument as a dependency in the local workspace.
 */
var requireArtifact = function (dependency) {
    var artifacts = resolveArtifact(dependency);
    var paths = artifacts.map(function(it){
        return it.file;
    });
    classpath.requirePath(paths);
};

exports.requireArtifact = requireArtifact;
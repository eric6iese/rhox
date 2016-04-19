package com.rhox.maven;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * The dependency manager resolves project using the internal configuration.
 */
public final class DependencyManager {

    private final DependencyResolver resolver = new DependencyResolver();

    /**
     * Resolves the dependencies and downloads them as needed.
     * <br/>
     * The result is a map consisting of the dependency artifact identifier
     * (anything except the possibly conflicting version).
     *
     * @param dependencies the dependency strings which should be resolve
     * @return a map of the dependency ids (without version) and the
     * corresponding files as strings.
     */
    public List<Artifact> resolve(Collection<Artifact> dependencies) throws IOException {
        return resolver.resolve(dependencies);
    }

    public static Artifact newArtifact(String group, String name, String version) {
        return newArtifact(group, name, null, null, version);
    }

    public static Artifact newArtifact(String group, String name, String extension, String classifier, String version) {
        return new DefaultArtifact(group, name, //
                classifier == null ? "" : classifier, //
                extension == null ? "jar" : extension, //
                version);
    }

    static String toIdentifier(Artifact artifact) {
        return String.join(":", artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension());
    }
}

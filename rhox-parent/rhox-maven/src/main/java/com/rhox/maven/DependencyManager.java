package com.rhox.maven;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    public Map<String, String> resolve(Collection<String> dependencies) throws IOException {
        List<Artifact> required = dependencies.stream().map(DependencyManager::parse).collect(Collectors.toList());
        List<Artifact> resolved = resolver.resolve(required);
        Map<String, String> result = new LinkedHashMap<>();
        resolved.forEach(artifact -> result.put(toIdentifier(artifact), artifact.getFile().getAbsolutePath()));
        return result;
    }

    /**
     * Creates a dependency out of the default gradle-like pattern with the :
     * separator.<br/>
     * Example:<br/>
     * 'org.springframework.data:spring-data-jpa:1.8.0.RELEASE' <br/>
     */
    static Artifact parse(String fullFormat) {
        Iterator<String> parts = Arrays.asList(fullFormat.split(":")).iterator();
        String group = parts.next();
        String name = parts.next();
        String ext = parts.next();
        if (!parts.hasNext()) {
            return new DefaultArtifact(group, name, "jar", ext);
        }
        String classifier = parts.next();
        if (!parts.hasNext()) {
            return new DefaultArtifact(group, name, ext, classifier);
        }
        return new DefaultArtifact(group, name, ext, classifier, parts.next());
    }

    static String toIdentifier(Artifact artifact) {
        return String.join(":", artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension());
    }
}

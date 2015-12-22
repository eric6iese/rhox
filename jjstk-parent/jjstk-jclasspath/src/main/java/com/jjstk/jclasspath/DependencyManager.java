package com.jjstk.jclasspath;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * The dependency manager resolves project using the internal configuration.
 */
public final class DependencyManager {

    private final DependencyResolver resolver = new DependencyResolver();

    /**
     * Resolves the dependencies and downloads them as needed.
     */
    public Set<File> resolve(Collection<String> dependencies) throws IOException {
        return resolver.resolve(dependencies);
    }
}

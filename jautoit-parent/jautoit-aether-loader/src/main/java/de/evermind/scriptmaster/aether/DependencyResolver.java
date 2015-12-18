package de.evermind.scriptmaster.aether;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * Resolves external dependencies by its resolve Method.<br/>
 * Each of these dependencies must use the default format.
 * 
 * @author giese
 *
 */
interface DependencyResolver {

	/**
	 * Resolves any number of dependencies, downloads them if not available, and
	 * returns them as a Set of paths in the local repository.
	 * 
	 * @throws IOException
	 *             if the resolving failed for networking, technical or
	 *             resolving reasons.
	 */
	Set<Path> resolve(Collection<Dependency> dependencies) throws IOException;
}

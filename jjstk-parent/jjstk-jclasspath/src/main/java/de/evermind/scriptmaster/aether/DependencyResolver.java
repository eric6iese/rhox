package de.evermind.scriptmaster.aether;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * Resolves external dependencies by its resolve Method.<br/>
 * Each of these dependencies must use the default format.
 * 
 * @author giese
 */
final class DependencyResolver {

	private final DependencyConfiguration cfg;
	private final RepositorySystem repositorySystem;
	private final DefaultRepositorySystemSession session;

	public DependencyResolver() {
		this(DependencyConfiguration.getMavenDefault());
	}

	public DependencyResolver(DependencyConfiguration cfg) {
		this.cfg = DependencyConfiguration.getMavenDefault();

		repositorySystem = newRepositorySystem();
		session = MavenRepositorySystemUtils.newSession();

		final LocalRepository local = new LocalRepository(cfg.getLocalRepository());
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, local));

		session.setTransferListener(new ConsoleTransferListener());
		session.setRepositoryListener(new ConsoleRepositoryListener());

	}

	/**
	 * Resolves any number of dependencies, downloads them if not available, and
	 * returns them as a Set of Files in the local repository.
	 * 
	 * @throws IOException
	 *             if the resolving failed for networking, technical or
	 *             resolving reasons.
	 */
	public Set<File> resolve(Collection<String> dependencies) throws IOException {
		List<Dependency> deps = dependencies.stream().map(Dependency::parse).collect(Collectors.toList());
		DependencyResult result;
		try {
			result = resolveDependencies(deps);
		} catch (RepositoryException e) {
			throw new IOException(e);
		}
		return getFile(result);
	}

	public CollectResult collectDependencies(Collection<Dependency> dependencies) throws RepositoryException {
		CollectRequest collectRequest = newCollectRequest(dependencies);
		return repositorySystem.collectDependencies(session, collectRequest);
	}

	public DependencyResult resolveDependencies(Collection<Dependency> dependencies) throws RepositoryException {
		CollectRequest collectRequest = newCollectRequest(dependencies);
		DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);
		return repositorySystem.resolveDependencies(session, dependencyRequest);
	}

	public Set<File> getFile(DependencyResult dependencyResult) {
		return dependencyResult.getArtifactResults().stream().//
				map(a -> a.getArtifact().getFile()).//
				collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		return locator.getService(RepositorySystem.class);
	}

	private CollectRequest newCollectRequest(Collection<Dependency> dependencies) {
		return new CollectRequest(dependencies.stream().map(DependencyResolver::toAether).collect(Collectors.toList()), //
				null, cfg.getRemoteRepositories());
	}

	private static org.eclipse.aether.graph.Dependency toAether(Dependency dependency) {
		String ext = dependency.getExt();
		if (ext.isEmpty()) {
			ext = "jar";
		}
		Artifact artifact = new DefaultArtifact(dependency.getGroup(), dependency.getName(), ext,
				dependency.getVersion());
		return new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.COMPILE);
	}

}

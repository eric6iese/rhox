package de.evermind.scriptmaster.aether;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

public class DependencyCollector {

	private final DependencyConfiguration cfg;
	private final RepositorySystem repositorySystem;
	private final DefaultRepositorySystemSession session;

	public DependencyCollector() {
		this(DependencyConfiguration.getMavenDefault());
	}

	public DependencyCollector(DependencyConfiguration cfg) {
		this.cfg = DependencyConfiguration.getMavenDefault();

		repositorySystem = newRepositorySystem();
		session = MavenRepositorySystemUtils.newSession();

		final LocalRepository local = new LocalRepository(cfg.getLocalRepository());
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, local));

		session.setTransferListener(new ConsoleTransferListener());
		session.setRepositoryListener(new ConsoleRepositoryListener());

	}

	public DependencyNode collect(Collection<Dependency> dependencies) throws RepositoryException {
		CollectRequest collectRequest = new CollectRequest(new ArrayList<>(dependencies), null,
				cfg.getRemoteRepositories());
		CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
		return result.getRoot();
	}

	public List<ArtifactResult> resolve(Collection<Dependency> dependencies) throws RepositoryException {
		CollectRequest collectRequest = new CollectRequest(new ArrayList<>(dependencies), null,
				cfg.getRemoteRepositories());
		DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);
		return repositorySystem.resolveDependencies(session, dependencyRequest).getArtifactResults();
	}

	private static RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		return locator.getService(RepositorySystem.class);
	}
}

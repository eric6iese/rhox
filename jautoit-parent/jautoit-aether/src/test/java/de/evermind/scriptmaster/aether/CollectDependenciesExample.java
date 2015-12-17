package de.evermind.scriptmaster.aether;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.junit.Test;

/**
 * This example as a base of my aether implementation was taken from <br/>
 * https://github.com/ivarref/collect-provided-dependencies.
 */
public class CollectDependenciesExample {

	@Test
	public void collectDependencies() throws DependencyCollectionException {
		// TODO you may need to change the following line:
		File localRepo = new File(String.join(File.separator, System.getProperty("user.home"), ".m2", "repository"));

		RepositorySystem repositorySystem = newRepositorySystem();
		DefaultRepositorySystemSession defaultRepositorySystemSession = MavenRepositorySystemUtils.newSession();

		final LocalRepository local = new LocalRepository(localRepo);
		defaultRepositorySystemSession.setLocalRepositoryManager(
				repositorySystem.newLocalRepositoryManager(defaultRepositorySystemSession, local));

		List<RemoteRepository> remotes = Arrays.asList(
				new RemoteRepository.Builder("maven-central", "default", "http://repo1.maven.org/maven2/").build());

		defaultRepositorySystemSession.setDependencySelector(new DependencySelector() {
			@Override
			public boolean selectDependency(Dependency dependency) {
				return true;
			}

			@Override
			public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
				return this;
			}
		});

		DefaultArtifact artifact = new DefaultArtifact("junit", "junit-dep", "", "jar", "4.10");
		
		CollectRequest request = new CollectRequest(new Dependency(artifact, null), remotes);

		CollectResult result = repositorySystem.collectDependencies(defaultRepositorySystemSession, request);
		DependencyNode root = result.getRoot();
		root.accept(new DependencyVisitor() {
			AtomicInteger indent = new AtomicInteger();

			@Override
			public boolean visitEnter(DependencyNode node) {
				StringBuilder sb = new StringBuilder();
				int indentLength = indent.getAndIncrement();
				for (int i = 0; i < indentLength; i++) {
					sb.append("  ");
				}
				System.err.println(sb.toString() + node.getDependency());
				return true;
			}

			@Override
			public boolean visitLeave(DependencyNode node) {
				indent.decrementAndGet();
				return true;
			}
		});
	}

	private static RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		return locator.getService(RepositorySystem.class);
	}

}
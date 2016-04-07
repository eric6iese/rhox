package com.jjstk.jclasspath;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
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
        this(MavenSettings.getDefault().toConfig());
    }

    public DependencyResolver(DependencyConfiguration cfg) {
        this.cfg = cfg;

        repositorySystem = newRepositorySystem();
        session = MavenRepositorySystemUtils.newSession();

        session.setOffline(cfg.isOffline());

        final LocalRepository local = new LocalRepository(cfg.getLocalRepository());
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, local));

        session.setProxySelector(cfg.getProxySelector());
        session.setMirrorSelector(cfg.getMirrorSelector());
        session.setAuthenticationSelector(cfg.getAuthSelector());

        session.setCache(new DefaultRepositoryCache());
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        // Configuration
        Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        session.setConfigProperties(configProps);
    }

    private String getUserAgent() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Java ").append(System.getProperty("java.version"));
        buffer.append("; ");
        buffer.append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version"));
        buffer.append(")");
        buffer.append(" Aether");
        return buffer.toString();
    }

    /**
     * Resolves any number of dependencies, downloads them if not available, and
     * returns them as a Set of Files in the local repository.
     *
     * @throws IOException if the resolving failed for networking, technical or
     * resolving reasons.
     */
    public List<Artifact> resolve(Collection<Artifact> artifacts) throws IOException {
        DependencyResult result;
        try {
            result = resolveDependencies(artifacts);
        } catch (RepositoryException e) {
            throw new IOException(e);
        }
        return getArtifacts(result);
    }

    public CollectResult collectDependencies(Collection<Artifact> dependencies) throws RepositoryException {
        CollectRequest collectRequest = newCollectRequest(dependencies);
        return repositorySystem.collectDependencies(session, collectRequest);
    }

    public DependencyResult resolveDependencies(Collection<Artifact> dependencies) throws RepositoryException {
        CollectRequest collectRequest = newCollectRequest(dependencies);
        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);
        return repositorySystem.resolveDependencies(session, dependencyRequest);
    }

    public List<Artifact> getArtifacts(DependencyResult dependencyResult) {
        return dependencyResult.getArtifactResults().stream().//
                map(a -> a.getArtifact()).//
                collect(Collectors.toList());
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private CollectRequest newCollectRequest(Collection<Artifact> artifacts) {
        return new CollectRequest(toDependencies(artifacts), //
                null, cfg.getRemoteRepositories());
    }

    private List<Dependency> toDependencies(Collection<Artifact> artifacts) {
        return artifacts.stream().map(artifact -> new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.COMPILE)).collect(Collectors.toList());
    }
}

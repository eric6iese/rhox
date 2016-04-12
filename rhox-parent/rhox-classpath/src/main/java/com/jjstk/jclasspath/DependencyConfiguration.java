package com.jjstk.jclasspath;

import java.io.File;
import java.util.List;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Defines the different dependency configurations for the system.
 */
final class DependencyConfiguration {

    private final File localRepository;
    private final List<RemoteRepository> remoteRepositories;

    private final ProxySelector proxySelector;
    private final MirrorSelector mirrorSelector;
    private final AuthenticationSelector authSelector;
    private final boolean offline;

    DependencyConfiguration(File localRepository, List<RemoteRepository> remotes) {
        this(localRepository, remotes, null, null, null, false);
    }

    DependencyConfiguration(File localRepository, List<RemoteRepository> remoteRepositories, ProxySelector proxySelector, MirrorSelector mirrorSelector, AuthenticationSelector authSelector, boolean offline) {
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.proxySelector = proxySelector;
        this.mirrorSelector = mirrorSelector;
        this.authSelector = authSelector;
        this.offline = offline;
    }

    public File getLocalRepository() {
        return localRepository;
    }

    public List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public ProxySelector getProxySelector() {
        return proxySelector;
    }

    public MirrorSelector getMirrorSelector() {
        return mirrorSelector;
    }

    public AuthenticationSelector getAuthSelector() {
        return authSelector;
    }

    public boolean isOffline() {
        return offline;
    }
}

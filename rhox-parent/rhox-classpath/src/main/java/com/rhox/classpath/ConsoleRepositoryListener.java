package com.rhox.classpath;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simplistic repository listener that logs events to the console. <br/>
 * Base form taken from:<br/>
 * <a href="https://github.com/eclipse/aether-demo">aether-demo</a>
 */
final class ConsoleRepositoryListener extends AbstractRepositoryListener {

    private final Logger LOG = LoggerFactory.getLogger(ConsoleRepositoryListener.class);

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        LOG.info("Deployed {} to {}" + event.getArtifact(), event.getRepository());
    }

    @Override
    public void artifactDeploying(RepositoryEvent event) {
        LOG.info("Deploying {} to {}", event.getArtifact(), event.getRepository());
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        LOG.error("Invalid artifact descriptor for " + event.getArtifact(), event.getException());
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        LOG.warn("Missing artifact descriptor for {}", event.getArtifact());
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        LOG.info("Installed {} to {}", event.getArtifact(), event.getFile());
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        LOG.info("Installing {} to {}", event.getArtifact(), event.getFile());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        LOG.debug("Resolved artifact {} from {}", event.getArtifact(), event.getRepository());
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        LOG.info("Downloading artifact {} from {}", event.getArtifact(), event.getRepository());
    }

    @Override
    public void artifactDownloaded(RepositoryEvent event) {
        LOG.info("Downloaded artifact {} from {}", event.getArtifact(), event.getRepository());
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        LOG.debug("Resolving artifact {}", event.getArtifact());
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        LOG.info("Deployed {} to {}", event.getMetadata(), event.getRepository());
    }

    @Override
    public void metadataDeploying(RepositoryEvent event) {
        LOG.info("Deploying {} to {}", event.getMetadata(), event.getRepository());
    }

    @Override
    public void metadataInstalled(RepositoryEvent event) {
        LOG.info("Installed {} to {}", event.getMetadata(), event.getFile());
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        LOG.debug("Installing {} to {}", event.getMetadata(), event.getFile());
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        LOG.error("Invalid metadata {}", event.getMetadata());
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        LOG.debug("Resolved metadata {} from {}", event.getMetadata(), event.getRepository());
    }

    @Override
    public void metadataResolving(RepositoryEvent event) {
        LOG.debug("Resolving metadata {} from {}", event.getMetadata(), event.getRepository());
    }
}

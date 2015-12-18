package de.evermind.scriptmaster.aether;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.repository.RemoteRepository;

/**
 * Defines the different dependency configurations for the system.
 */
class DependencyConfiguration {

	private final File localRepository;
	private final List<RemoteRepository> remoteRepositories;

	public static DependencyConfiguration getMavenDefault() {
		return new DependencyConfiguration(Paths.get(System.getProperty("user.home"), ".m2", "repository").toFile(),
				Arrays.asList(new RemoteRepository.Builder("maven-central", "default", "http://repo1.maven.org/maven2/")
						.build()));

	}

	public DependencyConfiguration(File localRepository, List<RemoteRepository> remotes) {
		this.localRepository = localRepository;
		this.remoteRepositories = Collections.unmodifiableList(new ArrayList<>(remotes));
	}

	public File getLocalRepository() {
		return localRepository;
	}

	public List<RemoteRepository> getRemoteRepositories() {
		return remoteRepositories;
	}

}

package de.evermind.scriptmaster.aether;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Creates Dependencies out of name patterns, here the Gradle-like syntax.
 */
public class DependencyFactory {

	/**
	 * Creates a dependency out of the default gradle-like pattern with the :
	 * separator.<br/>
	 * Example:<br/>
	 * 'org.springframework.data:spring-data-jpa:1.8.0.RELEASE'
	 */
	public Dependency parse(String pattern) {
		Iterator<String> parts = Arrays.asList(pattern.split(":")).iterator();
		String group = parts.next();
		String name = parts.next();
		String version = parts.next();
		String pack = "jar";
		if (parts.hasNext()) {
			pack = version;
			version = parts.next();
		}
		DefaultArtifact artifact = new DefaultArtifact(group, name, "", pack, version);
		return new Dependency(artifact, JavaScopes.COMPILE);
	}

}

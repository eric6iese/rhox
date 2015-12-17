package de.evermind.scriptmaster.aether;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.Ignore;
import org.junit.Test;

/**
 * PoC: A manual dependency manager which uses the Maven Repository.<br/>
 * 
 * @author giese
 *
 */
public class DependencyManagerTest {

	DependencyFactory fac = new DependencyFactory();
	DependencyCollector sut = new DependencyCollector();
	DependencyResolver resolver = new DependencyResolver();

	Dependency dep1 = fac.parse("org.springframework.data:spring-data-jpa:1.8.0.RELEASE");
	Dependency dep2 = fac.parse("org.apache.tomcat.embed:tomcat-embed-jasper:8.0.30");
	Dependency dep3 = fac.parse("org.springframework.data:spring-data-commons:1.8.0.RELEASE");
	List<Dependency> deps = Arrays.asList(dep1, dep2, dep3);

	@Test
	@Ignore
	public void testResolve() throws Exception {
		DependencyNode root = sut.collect(deps);
		resolver.print(root);
	}

	@Test
	public void testCollect() throws Exception {
		List<ArtifactResult> results = sut.resolve(deps);
		List<Artifact> arts = results.stream().map(ArtifactResult::getArtifact).collect(Collectors.toList());
		resolver.load(arts);
	}

}

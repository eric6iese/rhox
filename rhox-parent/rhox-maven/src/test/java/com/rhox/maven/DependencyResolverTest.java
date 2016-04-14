package com.rhox.maven;

import com.rhox.maven.DependencyManager;
import com.rhox.maven.DependencyPrinter;
import com.rhox.maven.DependencyResolver;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.Ignore;
import org.junit.Test;

/**
 * PoC: A manual dependency manager which uses the Maven Repository.<br/>
 *
 * @author giese
 *
 */
public class DependencyResolverTest {

    DependencyResolver sut = new DependencyResolver();
    DependencyPrinter printer = new DependencyPrinter();

    Artifact dep1 = DependencyManager.parse("org.springframework.data:spring-data-jpa:1.8.0.RELEASE");
    Artifact dep2 = DependencyManager.parse("org.apache.tomcat.embed:tomcat-embed-jasper:8.0.30");
    Artifact dep3 = DependencyManager.parse("org.springframework.data:spring-data-commons:1.8.0.RELEASE");
    List<Artifact> deps = Arrays.asList(dep1, dep2, dep3);

    @Test
    @Ignore
    public void testCollect() throws Exception {
        DependencyNode root = sut.collectDependencies(deps).getRoot();
        printer.print(root);
    }

    @Test
    public void testResolve() throws Exception {
        DependencyResult result = sut.resolveDependencies(deps);
        List<Artifact> artifacts = sut.getArtifacts(result);
        artifacts.forEach(System.out::println);
    }
}

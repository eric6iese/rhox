package com.jjstk.jclasspath;

import com.jjstk.jclasspath.Dependency;
import com.jjstk.jclasspath.DependencyResolver;
import com.jjstk.jclasspath.DependencyPrinter;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

	Dependency dep1 = Dependency.parse("org.springframework.data:spring-data-jpa:1.8.0.RELEASE");
	Dependency dep2 = Dependency.parse("org.apache.tomcat.embed:tomcat-embed-jasper:8.0.30");
	Dependency dep3 = Dependency.parse("org.springframework.data:spring-data-commons:1.8.0.RELEASE");
	List<Dependency> deps = Arrays.asList(dep1, dep2, dep3);

	@Test
	@Ignore
	public void testCollect() throws Exception {
		DependencyNode root = sut.collectDependencies(deps).getRoot();
		printer.print(root);
	}

	@Test
	public void testResolve() throws Exception {
		DependencyResult result = sut.resolveDependencies(deps);
		Set<File> paths = sut.getFile(result);
		paths.forEach(System.out::println);
	}
}

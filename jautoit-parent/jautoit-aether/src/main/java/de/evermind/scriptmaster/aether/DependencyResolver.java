package de.evermind.scriptmaster.aether;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

public class DependencyResolver {

	private static final Method M_ADDURL;

	static {
		try {
			M_ADDURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		} catch (NoSuchMethodException unexpected) {
			throw new UnsupportedOperationException(unexpected);
		}
		M_ADDURL.setAccessible(true);
	}

	/**
	 * Loads associated resolved Dependencies into the classpath.
	 */
	public void load(ClassLoader classLoader, List<Artifact> artifacts) throws Exception {
		for (Artifact artifact : artifacts) {
			URL url = artifact.getFile().toURI().toURL();
			System.out.println(url);
		}
		// var url = file.toFile().toURI().toURL();
		// var data = url.openStream();
		// M_ADDURL.invoke(classLoader, url);
	}

	/**
	 * Loads associated resolved Dependencies into the classpath.
	 */
	public void load(List<Artifact> artifacts) throws Exception {
		load(Thread.currentThread().getContextClassLoader(), artifacts);
	}

	/**
	 * Writes dependencies to the given writer.
	 */
	public void write(DependencyNode node, Appendable writer) {
		node.accept(new DependencyVisitor() {
			AtomicInteger indent = new AtomicInteger();

			@Override
			public boolean visitEnter(DependencyNode node) {
				StringBuilder sb = new StringBuilder();
				int indentLength = indent.getAndIncrement();
				for (int i = 0; i < indentLength; i++) {
					sb.append("  ");
				}
				Dependency dep = node.getDependency();
				try {
					writer.append(sb.toString() + dep);
				} catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
				return true;
			}

			@Override
			public boolean visitLeave(DependencyNode node) {
				indent.decrementAndGet();
				return true;
			}
		});
	}

	public void print(DependencyNode node) {
		write(node, new PrintWriter(System.out, true));
	}
}

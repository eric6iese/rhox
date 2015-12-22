package de.evermind.scriptmaster.aether;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

final class DependencyPrinter {

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

package de.evermind.scriptmaster.aether;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The dependency manager resolves project using the internal configuration.
 */
public final class DependencyManager {

	private static final Method M_ADDURL;

	static {
		try {
			M_ADDURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		} catch (NoSuchMethodException impossible) {
			throw new AssertionError(impossible);
		}
		M_ADDURL.setAccessible(true);
	}

	private final DependencyResolver resolver = new DependencyResolver();

	/**
	 * Resolves the dependencies and downloads them as needed.
	 */
	public Set<File> resolve(Collection<String> dependencies) throws IOException {
		return resolver.resolve(dependencies);
	}

	/**
	 * Loads associated resolved Dependencies into the classpath.
	 */
	public void load(URLClassLoader classLoader, Collection<File> resources) {
		Set<String> urls = Arrays.stream(classLoader.getURLs()).map(Object::toString).collect(Collectors.toSet());
		for (File resource : resources) {
			try {
				URL url = resource.toURI().toURL();
				if (urls.contains(url.toString())) {
					continue;
				}
				M_ADDURL.invoke(classLoader, url);
			} catch (ReflectiveOperationException | MalformedURLException impossible) {
				throw new AssertionError(impossible);
			}
		}
	}

	/**
	 * Loads associated resolved Dependencies into the classpath.
	 */
	public void load(Collection<File> resources) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		load(classLoader, resources);
	}
}
package de.evermind.scriptmaster.aether;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * The dependency manager resolves project using the internal configuration.
 */
public class DependencyManager {

	private static final Method M_ADDURL;

	static {
		try {
			M_ADDURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		} catch (NoSuchMethodException unexpected) {
			throw new UnsupportedOperationException(unexpected);
		}
		M_ADDURL.setAccessible(true);
	}

	private static final String C_RESOLVER = "de.evermind.scriptmaster.aether.DefaultDependencyResolver";

	/**
	 * The dependency resolver, which is load by a separate classloader
	 * mechanism.
	 */
	private static final DependencyResolver RESOLVER;

	static {
		URL[] jars;
		// TODO: Define packaging
		try (JarInputStream jarStream = new JarInputStream(null)) {
			Manifest mf = jarStream.getManifest();
			String classpath = mf.getMainAttributes().getValue("Class-Path");
			jars = Arrays.stream(classpath.split(File.pathSeparator)). //
					map(DependencyManager::newUrl). //
					toArray(URL[]::new);
		} catch (IOException impossible) {
			throw new AssertionError(impossible);
		}

		// cl is not closed until the jvm terminates
		@SuppressWarnings("resource")
		URLClassLoader mine = new URLClassLoader(jars);
		try {
			Class<?> c = mine.loadClass(C_RESOLVER);
			Class<? extends DependencyResolver> cr = c.asSubclass(DependencyResolver.class);
			RESOLVER = cr.newInstance();
		} catch (ReflectiveOperationException impossible) {
			throw new AssertionError(impossible);
		}
	}

	private static URL newUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException impossible) {
			throw new AssertionError(impossible);
		}
	}

	/**
	 * Resolves the dependencies and downloads them as needed.
	 */
	public Set<Path> resolve(Collection<Dependency> dependencies) throws IOException {
		return RESOLVER.resolve(dependencies);
	}

	/**
	 * Loads associated resolved Dependencies into the classpath.
	 */
	public void load(URLClassLoader classLoader, Collection<Path> resources) {
		Set<String> urls = Arrays.stream(classLoader.getURLs()).map(Object::toString).collect(Collectors.toSet());
		for (Path resource : resources) {
			try {
				URL url = resource.toUri().toURL();
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
	public void load(Collection<Path> resources) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		load(classLoader, resources);
	}

}

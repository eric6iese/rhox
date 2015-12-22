package de.evermind.scriptmaster.jacob;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.LibraryLoader;
import com.jacob.com.Variant;

public class JacobLoader {

	private static boolean ARCH_64 = System.getProperty("os.arch", "").contains("64");

	private static boolean initialized = false;

	/**
	 * Loads the jacob-libraries by placing them into the default tmpdir, if not
	 * yet available.
	 */
	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"), "de.evermind.scriptmaster");
		if (!Files.isDirectory(tmpdir)) {
			try {
				Files.createDirectories(tmpdir);
			} catch (IOException unexpected) {
				throw new UncheckedIOException(unexpected);
			}
		}

		Path jacobDll = unpackFile(tmpdir, JacobLoader.class, "jacob-1.18-x86.dll", "jacob-1.18-x64.dll");
		// Cant load autoit externally YET. currently it still has to be installed
		// Path autoitDll = unpackFile(tmpdir, AutoItApiWriter.class,
		// "AutoItX3.dll", "AutoItX3_x64.dll");
		// System.load(autoitDll.toString());
		System.setProperty(LibraryLoader.JACOB_DLL_PATH, jacobDll.toString());

		initialized = true;
	}

	private static Path unpackFile(Path dir, Class<?> base, String x86Name, String x64Name) {
		String name = ARCH_64 ? x64Name : x86Name;
		Path file = dir.resolve(name).toAbsolutePath();
		if (!Files.isRegularFile(file)) {
			try (InputStream is = base.getResourceAsStream(name)) {
				Files.copy(is, file);
			} catch (IOException unexpected) {
				throw new UncheckedIOException(unexpected);
			}
		}
		return file;
	}

	/**
	 * Calls a function on an active-x component in reflection-like manner,
	 * wrapping all parameters in variants as necessary.
	 * 
	 * @return the result of the operation. primitives are wrapped as necessary.
	 */
	public static Object invoke(ActiveXComponent ax, String methodName, Object... params) {
		Variant[] vars = new Variant[params.length];
		for (int i = 0; i < vars.length; i++) {
			vars[i] = new Variant(params[i]);
		}
		Variant result = ax.invoke(methodName, vars);
		return result.toJavaObject();
	}
}

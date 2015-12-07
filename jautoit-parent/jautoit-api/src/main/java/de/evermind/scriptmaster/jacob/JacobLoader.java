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

		boolean x64 = System.getProperty("os.arch", "").contains("64");
		String dllName = x64 ? "jacob-1.18-x64.dll" : "jacob-1.18-x86.dll";

		Path jacobDllFile = tmpdir.resolve(dllName).toAbsolutePath();
		if (!Files.exists(jacobDllFile)) {
			try (InputStream is = JacobLoader.class.getResourceAsStream(dllName)) {
				Files.createDirectories(tmpdir);
				Files.copy(is, jacobDllFile);
			} catch (IOException unexpected) {
				throw new UncheckedIOException(unexpected);
			}
		}

		System.setProperty(LibraryLoader.JACOB_DLL_PATH, jacobDllFile.toString());

		initialized = true;
	}

	/**
	 * Calls a function on an active-x component in reflection-like manner,
	 * wrapping all parameters in variants as necessary.
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

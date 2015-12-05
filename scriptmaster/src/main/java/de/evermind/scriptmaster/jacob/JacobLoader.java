package de.evermind.scriptmaster.jacob;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.jacob.com.LibraryLoader;

public class JacobLoader {

	private static boolean initialized = false;

	/**
	 * Loads the jacob-libraries by placing them into the default tmpdir, if not yet available.
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
}

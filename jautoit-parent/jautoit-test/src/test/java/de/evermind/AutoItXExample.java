package de.evermind;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import autoitx4java.AutoItX;
import de.evermind.scriptmaster.jacob.JacobLoader;

/**
 * Implementation of the official autoitx4java example with the autoloading of
 * jacob dlls.
 */
public class AutoItXExample {

	/**
	 * There are some glaring problems with using the auto-api as is, as autoitx
	 * shows.<br/>
	 * I guess i will have to make some minor improvements to them.
	 */
	@Test
	public void testAutoit() throws Exception {
		JacobLoader.initialize();
		AutoItX x = new AutoItX();
		String notepad = "Unbenannt - Editor";
		String testString = "this is a test.";

		Runtime.getRuntime().exec("cmd /c notepad");
		// x.run("notepad.exe");
		x.winActivate(notepad);
		x.winWaitActive(notepad);
		x.send(testString);
		assertTrue(x.winExists(notepad, testString));
		x.winClose(notepad, testString);
		x.winWaitActive("Editor");
		x.send("{ALT}n");
		assertFalse(x.winExists(notepad, testString));
	}
}

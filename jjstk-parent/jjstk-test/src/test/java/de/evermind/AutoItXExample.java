package de.evermind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jautoit.AutoItX;
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
		assertEquals(1, x.winExists(notepad, testString));
		x.winClose(notepad, testString);
		x.winWaitActive("Editor");
		x.send("{ALT}n");
		assertEquals(0, x.winExists(notepad, testString));
	}
}

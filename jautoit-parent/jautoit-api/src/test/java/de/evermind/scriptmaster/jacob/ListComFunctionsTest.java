package de.evermind.scriptmaster.jacob;

import org.junit.Ignore;
import org.junit.Test;

import com.jacob.activeX.ActiveXComponent;

public class ListComFunctionsTest {
	{
		JacobLoader.initialize();
	}

	ActiveXComponent ax = new ActiveXComponent("AutoItX3.Control");

	@Test
	public void printAutoit() {
		// TODO: Herausfinden wie man an einem COM-Objekt die Methoden
		// herausbekommt
		// Nächster Stop: OLEVIEWER
		System.out.println(ax);
	}

	@Ignore
	@Test
	public void dispatchDynamic() {
		int x = 300;
		int y = 500;
		int speed = 10;

		int result = (int) JacobLoader.invoke(ax, "MouseMove", x, y, speed);
		System.out.println(result);
	}

}

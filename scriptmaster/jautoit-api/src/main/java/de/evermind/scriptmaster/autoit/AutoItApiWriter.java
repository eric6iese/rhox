package de.evermind.scriptmaster.autoit;

/**
 * Creates a structure compatible for creating java (and javascript) types out of the autoit header files.
 */
public class AutoItApiWriter {

	/**
	 * API-Funktionen werden aus den Headern extrahiert:<br/>
	 * Zeilen die mit AU3_API starten werden zerlegt:<br/>
	 * Der Rückgabetyp wird direkt angewendet.<br/>
	 * WIN_API und der AU3_ header werden ignoriert.<br/>
	 * Type-Mappings. LPCWSTR => String, int => int (eigtl. long, theoretisch ja plattform-abhängig).<br/>
	 * Rückgabe:<br/>
	 * void oder int ODER HANDLE (?)<br/>
	 * Default-Werte müssen aus den Kommandos angewendet werden.<br/>
	 * Strings sind an der Rückgabe LPWSTR szResult, int nBufSize erkennbar.
	 * 
	 */
	public void writeClass(){
		// if li
	}
	
	
}

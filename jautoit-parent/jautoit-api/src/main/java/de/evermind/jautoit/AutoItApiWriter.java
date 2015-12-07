package de.evermind.jautoit;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import de.evermind.scriptmaster.jacob.JacobLoader;

/**
 * Creates a structure compatible for creating java (and javascript) types out
 * of the autoit header files.
 */
public class AutoItApiWriter {

	private static final String DIR = "C:/Program Files (x86)/AutoIt3/AutoItX";
	private static final String PACKAGE = AutoItApiWriter.class.getPackage().getName();

	private static final String HEAD = "AU3_API ";
	private static final String WINAPI = "WINAPI AU3_";

	public static void main(String[] args) throws IOException {
		writeClass();
	}

	/**
	 * API-Funktionen werden aus den Headern extrahiert:<br/>
	 * Zeilen die mit AU3_API starten werden zerlegt:<br/>
	 * Der Rückgabetyp wird direkt angewendet.<br/>
	 * WIN_API und der AU3_ header werden ignoriert.<br/>
	 * Type-Mappings. LPCWSTR => String, int => int (eigtl. long, theoretisch ja
	 * plattform-abhängig).<br/>
	 * Rückgabe:<br/>
	 * void oder int ODER HANDLE (?)<br/>
	 * Default-Werte müssen aus den Kommandos angewendet werden.<br/>
	 * Strings sind an der Rückgabe LPWSTR szResult, int nBufSize erkennbar.
	 * 
	 */
	public static void writeClass() throws IOException {
		Path headerFile = Paths.get(DIR, "AutoItX3_DLL.h");
		List<String> lines = Files.readAllLines(headerFile, Charset.defaultCharset());
		lines = lines.stream(). //
				filter(it -> it.startsWith(HEAD)). //
				map(it -> it.substring(HEAD.length()).replace(WINAPI, "").//
						replace("unsigned ", "unsigned_").//
						replace("/*[in,defaultvalue", "[default").replace("]*/", "]").replace("/*default ", "[default")
						.replace("*/", "]"))
				. //
				collect(Collectors.toList());

		TypeSpec.Builder apiClass = TypeSpec.classBuilder("AutoItX").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
		lines.stream().//
				map(AutoItApiWriter::toMethod).//
				forEach(apiClass::addMethod);

		JavaFile javaFile = JavaFile.builder(PACKAGE, apiClass.build()).build();

		Path dir = Paths.get("target/src-gen");
		javaFile.writeTo(dir);
	}

	private static final Pattern OUT_STRING = Pattern.compile("(.*)(LPWSTR [a-zA-Z]+, int [a-zA-Z]+)$");

	private static MethodSpec toMethod(String line) {
		String[] p = line.split(" ", 2);
		String out = p[0];
		p = p[1].split("\\(", 2);
		String name = p[0];
		String params = p[1].split("\\);", 2)[0];

		Matcher m = OUT_STRING.matcher(params);
		if ("void".equals(out) && m.matches()) {
			params = m.group(1);
			out = "LPCWSTR";
		}

		params = "void".equals(params) ? "" : params;

		Class<?> outType = typeOf(out);
		MethodSpec.Builder mb = MethodSpec.methodBuilder(name). //
				addModifiers(Modifier.PUBLIC).//
				returns(outType);

		List<String> paramList = new ArrayList<>();
		paramList.add("activeX");
		paramList.add("$S");
		for (String param : params.split("[ ]*,[ ]*")) {
			if (param.isEmpty()) {
				continue;
			}
			int pos = param.indexOf("]");
			if (pos != -1) {
				param = param.substring(pos + 1);
			}

			String[] ps = param.trim().split(" ");
			String pt = ps[0];
			String pname = ps[1];

			mb.addParameter(typeOf(pt), pname);
			paramList.add(pname);
		}

		String stmt = "$T.invoke(" + paramList.stream().collect(Collectors.joining(", ")) + ")";
		if (outType == void.class) {
			mb.addStatement(stmt, JacobLoader.class, name);
		} else {
			mb.addStatement("return " + stmt, JacobLoader.class, name);
		}
		return mb.build();
	}

	private static Class<?> typeOf(String name) {
		switch (name) {
		case "void":
			return void.class;
		case "LPCWSTR":
		case "LPWSTR":
			return String.class;
		case "DWORD":
		case "int":
		case "unsigned_int":
			return int.class;
		case "HWND":
			return long.class;
		case "LPRECT":
		case "LPPOINT":
			return int.class;
		default:
			throw new IllegalArgumentException(name);
		}
	}

}

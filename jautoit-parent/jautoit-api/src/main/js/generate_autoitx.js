// Imports
var Jvoid = Java.type("java.lang.Void");
var Charset = Java.type("java.nio.charset.Charset");
var Files = Java.type("java.nio.file.Files");
var Path = Java.type("java.nio.file.Path");
var Paths = Java.type("java.nio.file.Paths");
var Pattern = Java.type("java.util.regex.Pattern");

var Modifier = Java.type("javax.lang.model.element.Modifier");

var JavaFile = Java.type("com.squareup.javapoet.JavaFile");
var MethodSpec = Java.type("com.squareup.javapoet.MethodSpec");
var TypeSpec = Java.type("com.squareup.javapoet.TypeSpec");

var Dispatch = Java.type("com.jacob.com.Dispatch");



// Script
var DIR = "C:/Program Files (x86)/AutoIt3/AutoItX";
var PACKAGE = "de.evermind.jautoit";

var HEAD = "AU3_API ";
var WINAPI = "WINAPI AU3_";

var OUT_STRING = Pattern.compile("(.*)(LPWSTR [a-zA-Z]+, int [a-zA-Z]+)$");

/**
 * API-Funktionen werden aus den Headern extrahiert:<br/> Zeilen die mit
 * AU3_API starten werden zerlegt:<br/> Der Rückgabetyp wird direkt angewendet.<br/>
 * WIN_API und der AU3_ header werden ignoriert.<br/> Type-Mappings. LPCWSTR =>
 * String, int => int (eigtl. long, theoretisch ja plattform-abhängig).<br/>
 * Rückgabe:<br/> void oder int ODER HANDLE (?)<br/> Default-Werte müssen aus
 * den Kommandos angewendet werden.<br/> Strings sind an der Rückgabe LPWSTR
 * szResult, int nBufSize erkennbar.
 * 
 */
function writeClass() {
	var headerFile = Paths.get(DIR, "AutoItX3_DLL.h");
	var lines = Java.from(Files.readAllLines(headerFile, Charset.defaultCharset()));
	lines = lines. 
			filter(function(it) it.startsWith(HEAD)). 
			map(function(it) it.substring(HEAD.length()).replace(WINAPI, "").
					replace("unsigned ", "unsigned_").
					replace("/*[in,defaultvalue", "[default").replace("]*/", "]").replace("/*default ", "[default")
					.replace("*/", "]"));

	var apiClass = TypeSpec.classBuilder("AutoItX").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
	lines = lines.
			map(toMethod).
			forEach(apiClass.addMethod);

	var javaFile = JavaFile.builder(PACKAGE, apiClass.build()).build();

	var dir = Paths.get("target/src-gen");
	javaFile.writeTo(dir);
}



function toMethod(line) {
	var p = line.split(" ", 2);
	var out = p[0];
	p = p[1].split("(", 2);
	var name = p[0];
	var params = p[1].split(");", 2)[0];
	var m = OUT_STRING.matcher(params);
	if ("void" === out && m.matches()) {
		params = m.group(1);
		out = "LPCWSTR";
	}

	params = "void" === params ? "" : params;    
	var outType = typeOf(out);
	var mb = MethodSpec.methodBuilder(name). //
			addModifiers(Modifier.PUBLIC).//
			returns(outType);

	var paramList = ["activeX", "$S"];
	params.split(/[ ]*,[ ]*/).forEach(function(param) {
		if (param.isEmpty()) {
			return;
		}
		var pos = param.indexOf("]");
		if (pos != -1) {
			param = param.substring(pos + 1);
		}

		var ps = param.trim().split(" ");
		var pt = ps[0];
		var pname = ps[1];
		mb.addParameter(typeOf(pt), pname);
		paramList.push(pname);
	});

	var stmt = "$T.callN(" + paramList.join(", ") + ")";
	if (outType === Jvoid.TYPE) {
		mb.addStatement(stmt, Dispatch.class, name);
	} else {
		mb.addStatement("return " + stmt, Dispatch.class, name);
	}
	return mb.build();
}

function typeOf(name) {
	switch (name) {
	case "void":
		return Jvoid.TYPE;
	case "LPCWSTR":
	case "LPWSTR":
		return Java.type("java.lang.String").class;
	case "DWORD":
	case "int":
	case "unsigned_int":
		return Java.type("java.lang.Integer").TYPE;
	case "HWND":
		return Java.type("java.lang.Long").TYPE;
	case "LPRECT":
	case "LPPOINT":
		// TODO!
		return Java.type("java.lang.String[]").class;
	default:
		throw new Error(name);
	}
}

writeClass();
// Imports

var JVoid = Java.type("java.lang.Void");
var JInt = Java.type("java.lang.Integer");
var JLong = Java.type("java.lang.Long");
var JDouble = Java.type("java.lang.Double");
var JObject = Java.type("java.lang.Object");
var JString = Java.type("java.lang.String");
var System = Java.type("java.lang.System");

var StandardCharsets = Java.type("java.nio.charset.StandardCharsets");
var Files = Java.type("java.nio.file.Files");
var Path = Java.type("java.nio.file.Path");
var Paths = Java.type("java.nio.file.Paths");
var Pattern = Java.type("java.util.regex.Pattern");

var Modifier = Java.type("javax.lang.model.element.Modifier");

var JavaFile = Java.type("com.squareup.javapoet.JavaFile");
var TypeSpec = Java.type("com.squareup.javapoet.TypeSpec");
var FieldSpec = Java.type("com.squareup.javapoet.FieldSpec");
var MethodSpec = Java.type("com.squareup.javapoet.MethodSpec");


var Dispatch = Java.type("com.jacob.com.Dispatch");
var Variant = Java.type("com.jacob.com.Variant");

// Script
var CLASS = "AutoItX";
var PACKAGE = "de.evermind.jautoit";

// Klasse vorbereiten
var field = FieldSpec.builder(Dispatch.class, "dispatch", Modifier.PRIVATE, Modifier.FINAL).build();
var apiClass = TypeSpec.classBuilder(CLASS).
	addModifiers(Modifier.PUBLIC).
	addField(field);

apiClass.addMethod(MethodSpec.constructorBuilder().
		addModifiers(Modifier.PUBLIC).
		addStatement("this.$N = new $T($S)", field, Dispatch.class, "AutoItX3.Control").
		build());

var invokeMethod = MethodSpec.methodBuilder("invoke").
		addModifiers(Modifier.PRIVATE).
		addParameter(JString.class, "name").
		addParameter(Java.type(JObject.class.name + "[]").class, "parameters").
		varargs().
		returns(Variant.class).
		addStatement("$T[] vars = new $T[parameters.length]", Variant.class, Variant.class).
		beginControlFlow("for (int i = 0; i < vars.length; i++)").
		addStatement("vars[i] = new $T(parameters[i])", Variant.class).
		endControlFlow().
		beginControlFlow("synchronized($L.class)", CLASS).
		addStatement("return $T.callN($N,name,parameters)", Dispatch.class, field).
		endControlFlow().
		build();
apiClass.addMethod(invokeMethod);

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
	var headerFile = Paths.get("src/main/js/AutoItX3.idl");	
	var text = Java.from(Files.readAllLines(headerFile, StandardCharsets.UTF_8)).
	map(function(it) it.trim().
			replace("[in] ", "").
			replace(/\[in, optional, defaultvalue\(([^)]+)\)\] ([a-zA-Z0-9]+) ([a-zA-Z0-9]+)/, "$2 $3 $1").
			replace(/\[out(, retval)?\] ([a-zA-Z0-9]+)\* [a-zA-Z0-9]+/, "[out] $2").
			replace("HRESULT ", "")).
	map(function (it) !it.startsWith("[id") ? it : it.contains("propget") ? "propget|" : "method|").
	join("");
	// Text zwischen diesen Zeilen ermitteln
	text = text.split("interface IAutoItX3 : IDispatch {")[1];
	text = text.split("};")[0];
	
	// Zeilen anhand der ; wieder listenweise aufteilen
	var lines = text.split(";").
	  map(function(it) it.
			  replace("HRESULT ", "").
			  replace("(", "|").
			  replace(")", "").
			  replace(/,?\[out\] /, "|"));
	
	// Test/Debugging: Ausgabe der Zeilen
	// System.out.println(lines.join("\n"));

	
	lines.forEach(function(it) apiClass.addMethod(toMethod(it)));

	var javaFile = JavaFile.builder(PACKAGE, apiClass.build()).build();

	var dir = Paths.get("target/generated-sources/annotations");
	javaFile.writeTo(dir);
}

function toMethod(line) {	
	var parts = line.split("|");
	var type = parts[0];
	var name = parts[1];
	var params = parts.length > 2 ? parts[2].split(",") : [];
	var result = parts.length > 3 ? parts[3] : "void";
	
	var outType = typeOf(result, true);
	var mb = MethodSpec.methodBuilder(name). //
		addModifiers(Modifier.PUBLIC).//
		returns(outType);
	
	var paramList = params.
		filter(function(it) it != "").
		map(function(it){
			var ps = it.split(" ");
			var ptype = ps[0];
			var pname = ps[1];
			var def = ps.length > 2 ? ps[2] : undefined;		
			return [ptype, pname, def];
		});
	
	paramList.forEach(function (it) {
		mb.addParameter(typeOf(it[0]), it[1]);
	});
	
	var stmt = "$N($S" + paramList.map(function(it) ", " + it[1]).join("") + ")";
	if (outType == JVoid.TYPE){
		return mb.addStatement(stmt, invokeMethod, name).build();
	}
	var method;
	if (outType == JObject.class){
		method = "toJavaObject";
	}else {
		var outName = outType.simpleName;
		method = "get" + outName.charAt(0).toUpperCase() + outName.substring(1);
	}
	return mb.addStatement("return " + stmt + ".$L()", invokeMethod, name, method).build();
}

function typeOf(name, result) {
	switch (name) {
	case "VARIANT":
		return JObject.class;
	case "void":
		return JVoid.TYPE;
	case "BSTR":
		return JString.class;
	case "float":
	case "double":
		return JDouble.TYPE;
	case "long":
	case "int":
		return result ? JLong.TYPE : JInt.TYPE;	
	default:
		throw new Error(name);
	}
}

writeClass();
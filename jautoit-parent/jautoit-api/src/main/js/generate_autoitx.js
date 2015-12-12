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
var URL = Java.type("java.net.URL");
var Modifier = Java.type("javax.lang.model.element.Modifier");

var JavaFile = Java.type("com.squareup.javapoet.JavaFile");
var TypeSpec = Java.type("com.squareup.javapoet.TypeSpec");
var FieldSpec = Java.type("com.squareup.javapoet.FieldSpec");
var MethodSpec = Java.type("com.squareup.javapoet.MethodSpec");
var ClassName = Java.type("com.squareup.javapoet.ClassName");

var typeDispatch = ClassName.get("com.jacob.com", "Dispatch");
var typeVariant = ClassName.get("com.jacob.com", "Variant");

// Script
var CLASS = "AutoItX";
var PACKAGE = "com.jautoit";

// Klasse vorbereiten
var field = FieldSpec.builder(typeDispatch, "dispatch", Modifier.PRIVATE, Modifier.FINAL).build();
var apiClass = TypeSpec.classBuilder(CLASS).
	addModifiers(Modifier.PUBLIC).
	addField(field);

apiClass.addMethod(MethodSpec.constructorBuilder().
		addModifiers(Modifier.PUBLIC).
		addStatement("this.$N = new $T($S)", field, typeDispatch, "AutoItX3.Control").
		build());

var invokeMethod = MethodSpec.methodBuilder("invoke").
		addModifiers(Modifier.PRIVATE).
		addParameter(JString.class, "name").
		addParameter(Java.type(JObject.class.name + "[]").class, "parameters").
		varargs().
		returns(JObject.class).
		addStatement("$T[] vars = new $T[parameters.length]", typeVariant, typeVariant).
		beginControlFlow("for (int i = 0; i < vars.length; i++)").
		addStatement("vars[i] = new $T(parameters[i])", typeVariant).
		endControlFlow().
		beginControlFlow("synchronized($L.class)", CLASS).
		addStatement("return $T.callN($N,name,parameters).toJavaObject()", typeDispatch, field).
		endControlFlow().
		build();
apiClass.addMethod(invokeMethod);

function cleanDoc(str){
	return str.replace(/\$/g, '').replace(/\\u/g, "&#92;u").trim();
}

function substringBetween(str, start, end){
	var ps = str.indexOf(start);
	if (ps == -1){
		return "";
	}
	var pe = str.indexOf(end, ps + 1);
	if (pe == -1){
		return "";
	}
	return str.substring(ps + start.length, pe);	
}

function capitalized(word){
	return word == "" ? "" : word.charAt(0).toUpperCase() + word.substring(1)
}

function decapitalized(word){	
	return word == "" ? "" : word.charAt(0).toLowerCase() + word.substring(1)
}

/**
 * Einige Dokumentationen sind leicht abgewandelt oder nicht vorhanden
 */
var docMappings = {
	'Init' 					: null,
	'ControlGetPosX'		: 'ControlGetPos',
	'ControlGetPosY'		: 'ControlGetPos',
	'ControlGetPosHeight'	: 'ControlGetPos',
	'ControlGetPosWidth'	: 'ControlGetPos',
	'MouseGetPosX'			: 'MouseGetPos',
	'MouseGetPosY'			: 'MouseGetPos',
	'Opt'					: null,
	'WinGetCaretPosX'		: 'WinGetCaretPos',
	'WinGetCaretPosY'		: 'WinGetCaretPos',
	'WinGetClientSizeHeight': 'WinGetClientSize',
	'WinGetClientSizeWidth'	: 'WinGetClientSize',
	'WinGetPosX'			: 'WinGetPos',
	'WinGetPosY'			: 'WinGetPos',
	'WinGetPosHeight'		: 'WinGetPos',
	'WinGetPosWidth'		: 'WinGetPos',
	
};

/**
 * Lädt zu einer Api-Method die passende autoit-dokumentation herunter, falls nicht schon geschehen.<br/>
 * Diese werden zunächst nicht in target sondern im autoit-doc ordner zwischengespeichert,
 * um die downloads zwischen clean-builds und die abhängigkeit vom internet zu senken.
 */
function getJavaDoc(name){
	var fname = capitalized(name);
	var real = docMappings[fname];
	if (real){
		// use mapping
		fname = real;	
	}else if (real === null){
		// no doc for this one
		return {}; 
	}
	var dir = Paths.get("autoit-doc");
	Files.createDirectories(dir);
        var file = dir.resolve(fname + ".htm");
	if (!Files.exists(file)){
		var docBase = "https://www.autoitscript.com/autoit3/docs/functions/";
		var url = new URL(docBase + file.getFileName());
		System.out.print("Download " + url + " ... ");
		var instream = null;
		try{
			instream = url.openStream();
			Files.copy(instream, file);
			System.out.println("... ok.");
		}catch (e){
			System.out.println("... failed and ignored.");
		} finally {			
			if (instream != null){
				instream.close();	
			}				
		}
	}
	if (!Files.exists(file)){
		return {};
	}
	var data = Java.from(Files.readAllLines(file, StandardCharsets.UTF_8)).join("\n");
	var doc = {}
	doc.desc = cleanDoc(substringBetween(data, '<p class="funcdesc">', '</p>'));
	doc.returns = cleanDoc(substringBetween(data, '<h2>Return Value</h2>', '<h2>'));
	doc.parameters = cleanDoc(substringBetween(data, '<h2>Parameters</h2>', '<h2>'));
	// TODO: irgendwann auch die parameter richtig parsen?
	/*var parameters = [];
	var paramLines= substringBetween(data, '<h2>Parameters</h2>\n<table>\n<tr>\n', '\n</tr>\n</table>').split("\n</tr>\n<tr>\n");
	paramLines.forEach(function(it){
		it = it.trim();
		if (!it.isEmpty()){
			return;
		}
		it.replace(/<td[^>]*>/g, '')
	});
	params = params.split("</tr>");
	*/
	return doc;
}

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
	  filter(function(it) it != "").
	  map(function(it) it.
			  replace("HRESULT ", "").
			  replace("(", "|").
			  replace(")", "").
			  replace(/,?\[out\] /, "|"));
	
	// Test/Debugging: Ausgabe der Zeilen
	// System.out.println(lines.join("\n"));

	lines.forEach(function(it) addMethod(it));

	var javaFile = JavaFile.builder(PACKAGE, apiClass.build()).build();

	var dir = Paths.get("target/generated-sources/annotations");
	javaFile.writeTo(dir);
}

function addMethod(line) {	
	var parts = line.split("|");
	var type = parts[0];
	var name = parts[1];
	var params = parts.length > 2 ? parts[2].split(",") : [];
	var result = parts.length > 3 ? parts[3] : "void";
	
	var outType = typeOf(result, true);

	var required = null;
	var paramList = params.
		filter(function(it) it != "").
		map(function(it, idx){
			var ps = it.split(" ");
			var ptype = typeOf(ps[0]);
			var pname = decapitalized(ps[1].replace(/^(str|n)/, ''));

			if (ps.length == 2){
				return [ptype, pname]
			}
			var def = ps[2];			
			if (!required){
				required = idx;	
			}
			return [ptype, pname, def]; 
		});
	if (required == null){
		required = paramList.length;
	}
	
	var javadoc = type == "method" ? getJavaDoc(name) : {};		 
	
	// ... overloads für den Rest erzeugen
	for (var i = required; i <= paramList.length; i++){
		apiClass.addMethod(createMethod(outType, name, paramList.slice(0, i), paramList, javadoc));
	}
}

function createMethod(outType, name, paramList, allParams, javadoc){
	var mb = MethodSpec.methodBuilder(decapitalized(name)).
		addModifiers(Modifier.PUBLIC).
		returns(outType);
	
	if (javadoc.desc){
		mb.addJavadoc(javadoc.desc + '\n');
	}
	if (javadoc.parameters){
		mb.addJavadoc(javadoc.parameters + '\n');
	}
	if (javadoc.returns){
		mb.addJavadoc('@return ' + javadoc.returns + '\n');
	}

	var pnames = [];	
	paramList.forEach(function(it) {
		mb.addParameter(it[0], it[1]);
		pnames.push(it[1]);
	});
	applyParameterPatches(name, pnames, allParams);
	
	var stmt = "$N($S" + pnames.map(function(it) ", " + it).join("") + ")";
	switch (outType){
	case JVoid.TYPE:
		return mb.addStatement(stmt, invokeMethod, name).build();
	case JObject.TYPE:
		return mb.addStatement("return " + stmt + "", invokeMethod, name).build();
	case JLong.TYPE:
		return mb.addStatement("return ((Number)" + stmt + ").longValue()", invokeMethod, name).build();
	default:
		return mb.addStatement("return ($T)" + stmt + "", outType, invokeMethod, name).build();
	}
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

function applyParameterPatches(name, pnames, allParams){
	if ((name == "Send" || name == "ControlSend") && pnames.length == allParams.length -1){
		// Patch: Das Mode-Flag wird von AutoItX falsch deklariert: 0 und 1 sind vertauscht!	  
		pnames.push(1);
	}
}

writeClass();
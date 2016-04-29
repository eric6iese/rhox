# rhox
Rhox - you faithful Rhinoceross Ox (or whatever).
A Toolset of libraries designed for easy creation of java and nashorn shell and windows scripts


## rhox-classpath
This module allows for the easy-on demand adding of entries to the current classloader (if it is an urlclassloader, that is).
Loading of classes is featured in two styles:

```javascript
// -------- JAR-FILE/CLASS-FOLDER-LOADING ----
var classpath = require("rhox-classpath");

// load all slfj4-jars from the subfolder 
classpath.include("lib/slf4j*.jar");
var log = org.slf4j.LoggerFactory.getLogger("hello");
log.info("well done!");

// requiring the same file(s) multiple times has no effect, as all urls are checked on the classloader before they are added
classpath.include("lib/slf4j*.jar");

// -------- MAVEN DEPENDENCY LOADING ----
var maven = require("rhox-maven");

// load JUnit into the current classloader using maven (usually SystemClassLoader)
maven.include('junit:junit:4.11');
var Assert = Java.type("org.junit.Assert");
Assert.assertTrue(true, "I am now available!");
```
WHY?
classpath solves the problem that javascript tends to maintain its dependencies via a dependency-system on-the-fly, while in java (se) traditionally all dependencies have to be defined in the classpath. In order to overcome this restriction, this class performs a mild hack on the classloader which should work at least in all jjs-scripts.
In general, resolving dependencies via maven should be the preferred way, as it prevents duplicates much easier, but the manual file-style might be more useful if you design your own java-script-node-style modules which include java-bindings.
Besides being a tool for playing around, this is also the binding glue of all other rhox-modules, which use both ways to include common maven libraries as well as their own module-internal jar-bindings.

## rhox-native
Use Windows COM-Objects as if they were native Javascript Objects

```javascript
var native = require('rhox-native');
var ComObject = native.ComObject;

// Create a new ComObject binding for Word
var word = new ComObject('Word.Application');
try {
    word.Visible = true;
    var documents = word.Documents;
    var document = documents.Open('SomeDoc.docx');
    var selection = word.Selection;
    var find = selection.Find;
    find.Text = "FIND ME";
    find.Execute();
    selection.Text = "INSERT THIS";
} finally {
    word.Quit();
}
```

[More...](https://github.com/eric6iese/rhox/wiki)

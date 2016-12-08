# rhox
Rhox - you faithful Rhinoceross Ox (or whatever).
A Toolset of libraries designed for easy creation of java and nashorn shell and windows scripts


## rhox-classpath
```javascript
This module allows for the easy-on demand adding of entries to the current classloader (if it is an urlclassloader, that is).
var classpath = require("rhox-classpath");

// Adds all jars directly in the lib-folder to the classpath
classpath.include('lib/*.jar');
```

WHY?
classpath solves the problem that javascript tends to maintain its dependencies via a dependency-system on-the-fly, while in java (se) traditionally all dependencies have to be defined in the classpath. In order to overcome this restriction, this class performs a mild hack on the classloader which should work at least in all jjs-scripts.
In general, resolving dependencies via maven should be the preferred way, as it prevents duplicates much easier, but the manual file-style might be more useful if you design your own java-script-node-style modules which include java-bindings.
Besides being a tool for playing around, this is also the binding glue of all other rhox-modules, which use both ways to include common maven libraries as well as their own module-internal jar-bindings.

## rhox-native
Use Windows COM-Objects as if they were native Javascript Objects

```javascript
var native = require('rhox-native');
var ComObject = native.win32.ComObject;

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

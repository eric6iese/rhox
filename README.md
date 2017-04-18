# rhox
Rhox - you faithful Rhinoceross Ox (or whatever).
A Toolset of libraries designed for easy creation of java and nashorn shell and windows scripts


## rhox-classpath and rhox-maven
```javascript
// This module allows for the easy-on demand adding of entries to the current classloader (if it is an urlclassloader, that is).
var classpath = require("rhox-classpath");

// Adds all jars directly in the lib-folder to the classpath
classpath.include('lib/*.jar');

// This module connects to maven, using your local USER_DIR/settings.xml for repo/proxy configuration
var maven = require('rhox-maven');

// Get slf4j from local repo or internet, including all transitives
maven.include('org.slf4j:slf4j-simple:1.7.21');
var LoggerFactory = classpath.type('org.slf4j.LoggerFactory');
var log = LoggerFactory.getLogger('bob');
log.info('hello');
```

WHY?
classpath solves the problem that javascript tends to maintain its dependencies via a dependency-system on-the-fly, while in java (se) traditionally all dependencies have to be defined in the classpath. In order to overcome this restriction, this class performs a mild hack on the classloader which should work at least in all jjs-scripts. Besides being a tool for playing around, this is also the binding glue of all other rhox-modules, which use it to include their jars dynamically.

But you will rarely use it by yourself. Because... you're going to use rhox-maven. In general, resolving dependencies with rhox-maven, the second example, should be the preferred way, as it is much shorter and easier, but the manual file-style might be more useful if you design your own java-script-node-style modules which include java-bindings.


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

## rhox-shell
Simple process execution in scripts, but way better than nashorn's $EXEC

```javascript
// this is windows example - sorry :-/
var exec = require('rhox-shell').exec;
// prints hello
exec('cmd /c echo hello');
// appends hello to local file out.txt.
exec('cmd /c echo hello', { '>>': out.txt });
```


[More...](https://github.com/eric6iese/rhox/wiki)

# rhox
Rhox - you faithful Rhinoceross Ox (or whatever).
A Toolset of libraries designed for easy creation of java and nashorn shell and windows scripts


## rhox-classpath
This module allows for the easy-on demand adding of entries to the current classloader (if it is an urlclassloader, that is).
Loading of classes is featured in two styles:

```javascript
var jclasspath = require("rhox-classpath");

// -------- JAR-FILE/CLASS-FOLDER-LOADING ----

// load all slfj4-jars from the subfolder 
jclasspath.requirePath(__dirname, "lib/slf4j*.jar");
var log = org.slf4j.LoggerFactory.getLogger("hello");
log.info("well done!");

// requiring the same file(s) multiple times has no effect, as all urls are checked on the classloader before they are added
jclasspath.requirePath(__dirname, "lib/slf4j*.jar");

// -------- MAVEN DEPENDENCY LOADING ----

// load JUnit into the current classloader using maven (usually SystemClassLoader)
jclasspath.requireArtifact('junit:junit:4.11');
var Assert = Java.type("org.junit.Assert");
Assert.assertTrue(true, "I am now available!");

// note: does not the same groupId/artifactId again in any version!
jclasspath.requireArtifact('junit:junit:4.10');
```
WHY?
jclasspath solves the problem that javascript tends to maintain its dependencies via a dependency-system on-the-fly, while in java (se) traditionally all dependencies have to be defined in the classpath. In order to overcome this restriction, this class performs a mild hack on the classloader which should work at least in all jjs-scripts.
In general, resolving dependencies via maven should be the preferred way, as it prevents duplicates much easier, but the manual file-style might be more useful if you design your own java-script-node-style modules which include java-bindings.
Besides being a tool for playing around, this is also the binding glue of all other rhox-modules, which use both ways to include common maven libraries as well as their own module-internal jar-bindings.

## rhox-combridge
Use Windows COM-Objects as if they were native Javascript Objects

```javascript
var combridge = require('rhox-combridge');
var ComObject = combridge.ComObject;
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

# Bootstrapping
One of the biggest issues to solve with scripting additions like rhox and the like might be the bootstrapping of the module loader. This might be changed if java 9 is completely ES6 compatible, but up to this point we need a workable and simple solution which can be used in ALL scripts in the same way. That requires two steps:
1. Choose an implementation of the commonjs-require function
2. Load the function in each of your scripts

## the require function
First of all, you need an implementation of the require-function for nashorn.
* The best and most well-known implementation of this jvm-npm, a fully working implementation of the npm way of doing require(). It is a side-product of the now abandoned nodyn-Project which tried to provide a full node.js implementation. Get it here: https://github.com/nodyn/jvm-npm
* One of the problems of jvm-npm is that the way in which it invokes your module provides very little support for debugging and logging in java, because the load()-function of nashorn is not used. Therefore your debugger will have no idea from where the script is called, and the stacktraces are also not for the weak of heart. Therefore I have created an fork of jvm-npm which does the compatibility with rhino and other js-engines in favor of a better nashorn experience. Get it here: https://github.com/eric6iese/jvm-npm
* There might also be other alternatives but I would strongly recommend you to make sure that you choose a node-compatible module-loader as rhox also uses node's format.

## bootstrapping
Loading the requirefunction should be the first line in each of your scripts which is invoked by jjs. There might be other alternatives as well, like classpath magic or stuff on the commandline when invoking jjs, but here I expect you are using jjs the 'normal' way and bootstrap everything in each of your scripts. I would recommend this way because it allows your scripts to be run simpler and on more platforms.  There are various ways to do this, and when considering that should try to use the same idiom in all of your local scripts the same you should choose wisely.

### Environment variables
Using an enviroment variable is the 'classical' way to do this, with a name like NASHORN_REQUIRE, NASHORN_GLOBALS.

While it offers a very high amount of flexibility and ensures that your scripts always use the 'right' way,
introducing env-variables might annoy some people as there are usually more than enough of these
Choosing an enviroment variable does not solve the problem by itself, but now at least you are free to change the path later on

```javascript
// Accessing env-variables via java's System object:
load(java.lang.System.getenv('NASHORN_GLOBALS'));

// If you are in nashorn shell-scripting mode, you can also use the $ENV-map
// However, I strongly prefer the only slightly longer call via System because it works in all enviroments the same
load($ENV.NASHORN_GLOBALS));
```

### Classpath
The more natural approach in java would be to put the script on the classpath
Then you can load i.e. jvm-npm simply like this:
```javascript
// use jvm-npm
load('classpath:jvm-npm.js');

// you might want to rename the script for stylistic reasons:
load('classpath:require.js');
```

The problem here is: 
How to put the script on the classpath in the first place?
The whole reason for an inscript-classpath-manipulation was that scripts should not have to be configured from the outside.
So, supplying it from the commandline via classpath-param is a no-go and also too verbose.

A possible solution to this might be to checkout and compile one of the jvm-npm distributions with maven to get a jar containing the script.
This jar can then be placed in your java/jre/lib/ext folder and 'voila', it just works.
Note that this mechanism is now considered deprecated, but thats not a problem.
Even if java9 removes this feature, it's es6-nashorn implementation might support modules which solve the problem in a much better way.

This solution is elegant and concise, but because it requires modifiying your java-home many peoply might shun away from it.
Its also not portable to other systems where you don't have root permissions (shouldn't be problem for many scripts, but I wanted to mention this)

### Default Property Directory
You should never hardcore the full or relative path in your scripts!
You can put the Script into one of the default system directories which are easily reached by a call to System.getProperty()
Add a nice-sounding subdirectory to one, and maybe define a wrapper script like globals.js for more flexibility later on and you're done:

```javascript
// stupid choice because you don't need the prefix at all for relative (not recommended) loading
load(System.getProperty('user.dir')/js/globals.js');

// creating scripts in your jdk has exactly the same advantages as the classpath-approach above.
// choose whichever you prefer
load(System.getProperty('java.home')/js/globals.js');

// a better idea. problem is that each user who wants to execute the script must create the dir
// still perhaps the most portable solution out of this bunch
load(System.getProperty('user.home')/js/globals.js');
```

### Recommendation
Like it or not, env-variables are the most flexible approach and make it easier to maintain your scripts.
So go for it unless you REALLY like to use some magic directory (still possible with env-vars) or the classpath.

But when you are using the ENV-Variable approach above, then where to put your script?
There are many different alternatives, and I have to admit that I do not have a preference right now...
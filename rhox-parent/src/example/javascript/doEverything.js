load(java.lang.System.getenv('NASHORN_GLOBALS'));

var maven = require("rhox-maven");

maven.include('com.google.guava:guava:19.0');

var ImmutableList = Java.type('com.google.common.collect.ImmutableList');
var System = Java.type('java.lang.System');
var list = ImmutableList.of(1, 2, 3);
System.out.println(list);



var native = require('rhox-native');
var ComObject = native.ComObject;
var au = new ComObject("AutoItX3.Control");


au.send("Hello I screw your script!");

load(java.lang.System.getProperty('user.home') + '/jjstk/bootstrap.js');

var combridge = require('jjstk-combridge');

var System = Java.type('java.lang.System')
var Runtime = Java.type('java.lang.Runtime')

var au = combridge.newComObject("AutoItX3.Control");

function assertWinExists(value) {
    var result = au.winExists(notepad, testString);
    if (result != value) {
        throw new Error('Expected ' + value + ' but was ' + result + '!');
    }
}

var notepad = "Unbenannt - Editor";
var testString = "this is a test.";

Runtime.getRuntime().exec("cmd /c notepad");
// x.run("notepad.exe");
au.WinActivate(notepad);
au.WinWaitActive(notepad);
au.Send(testString);

assertWinExists(1);

au.WinClose(notepad, testString);
au.WinWaitActive("Editor");
au.Send("{ALT}n", 1);

assertWinExists(0);

var err = "" + au.error;
System.out.println(err);

var swShow = au.SW_SHOW
System.out.println(swShow);

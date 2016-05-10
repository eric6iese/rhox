load(java.lang.System.getenv('NASHORN_GLOBALS'));

var native = require('rhox-native');
var ComObject = native.win32.ComObject;

var System = Java.type('java.lang.System')
var Runtime = Java.type('java.lang.Runtime')

var au = new ComObject("AutoItX3.Control");

function assertWinExists(value) {
    var result = au.winExists(notepad, testString);
    if (result !== value) {
        throw new Error('Expected ' + value + ' but was ' + result + '!');
    }
}

try {
    au.thisValueDoesNeverExist;
    System.out.println("hätte fehlschlagen müssen!");
} catch (e) {
    System.out.println("ok, fehler hier erwartet!");
}


try {
    au.thisMethodDoesNeverExist();
    System.out.println("hätte fehlschlagen müssen!");
} catch (e) {
    System.out.println("ok, fehler hier erwartet!");
}

var notepad = "Unbenannt - Editor";
var testString = "this is a test.";

Runtime.getRuntime().exec("cmd /c notepad");
// x.run("notepad.exe");
au.WinActivate(notepad);
au.WinWaitActive(notepad);
au.Send(testString);
java.lang.Thread.sleep(2000);
assertWinExists(1);

au.WinClose(notepad, testString);
au.WinWaitActive("Editor");
au.Send("{ALT}n", 1);

assertWinExists(0);

var err = au.error;
System.out.println(err);

var swShow = au.SW_SHOW
System.out.println(swShow);

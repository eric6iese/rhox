load(java.lang.System.getenv('NASHORN_GLOBALS'));

var native = require('rhox-native');

var Excel = new native.ComObject('Excel.Application');
var file = new java.io.File('excel.xlsx').getCanonicalFile();
if (!file.exists()) {
    throw new Error("Damn: " + file);
}
Excel.Workbooks.Open(file.toString());
try {
    Excel.Visible = true;
    var Cells = Excel.Cells;
    var CellsAgain = Excel.Cells;
    var cells = Cells(1, 1).Value + " - " + Cells(1, 2).Value;
    java.lang.System.out.println(cells);
} finally {
    Excel.Quit();
}
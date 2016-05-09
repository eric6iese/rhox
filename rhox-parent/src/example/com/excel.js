load(java.lang.System.getenv('NASHORN_GLOBALS'));

var out = java.lang.System.out;

var native = require('rhox-native');
var ComObject = native.ComObject;
var ComDispatch = native.ComDispatch;

var Excel = new ComObject('Excel.Application');
var file = new java.io.File('excel.xlsx').getCanonicalFile();
if (!file.exists()) {
    throw new Error("Damn: " + file);
}
Excel.Workbooks.Open(file.toString());
try {
    Excel.Visible = true;
    var Cells = Excel.Cells;
    var CellsAgain = Excel.Cells;
    var moreCells = Excel.Cells(1, 1);
    out.println(moreCells);

    var cells = moreCells.Value + " - " + Cells(1, 2).Value;
    out.println(cells);

    var ExcelDispatch = new ComDispatch(Excel);
    out.println("Per Excel dispatch:");
    var CellDispatch = ExcelDispatch.get("Cells", 1, 1);
    out.println(CellDispatch + ":");
    out.println(CellDispatch.get("Value"));

    out.println("Back to ComObject:");
    Cells = new ComObject(CellDispatch);
    out.println(Cells + ":");
    out.println(Cells.Value);

} catch(e){
    e.printStackTrace();
}finally {
    Excel.Quit();
}
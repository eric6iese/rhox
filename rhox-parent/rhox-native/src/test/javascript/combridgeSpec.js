try {
    var combridge = require('rhox-native');
} catch (e) {
    if (e.cause){
      e.cause.printStacktrace();     
    }
    throw e;
}
describe("combobject", function () {
    it("does wonderful things", function () {     
        // TODO: com-tests
    });
});
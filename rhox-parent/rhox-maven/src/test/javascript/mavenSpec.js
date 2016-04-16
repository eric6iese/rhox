var maven = require("rhox-maven");

describe("internal classpath", function () {
    it("cannot access by default", function () {
        var Throwables;
        try {
            Throwables = Java.type("com.google.common.base.Throwables")
            Assert.fail("Cannot load internal classes of the dependency classloader!");
        } catch (expected) {
            System.out.println("No access to guava - ok!");
        }

        // ... but now i hsve!
        maven.include('com.google:guava:19.0');
        Throwables = Java.type("com.google.common.base.Throwables");
    });

});
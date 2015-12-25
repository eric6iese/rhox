var jclasspath = require("jjstk-jclasspath");

describe("internal jclasspath", function () {
    it("cannot access by default", function () {
        var Throwables;
        try {
            Throwables = Java.type("com.google.common.base.Throwables")
            Assert.fail("Cannot load internal classes of the dependency classloader!");
        } catch (expected) {
            System.out.println("No access to guava - ok!");
        }
    });
});
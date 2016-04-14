var jclasspath = require("rhox-classpath");

/*
 * TODO: something new
 */
describe("internal classpath", function () {
    it("cannot access by default", function () {
        var Throwables;
        try {
            Throwables = Java.type("com.google.common.base.Throwables");
            // Assert.fail("Cannot load internal classes of the dependency classloader!");
            expect(true).toBe(false);
        } catch (expected) {
            System.out.println("No access to guava - ok!");
        }
    });
    it("is named bob", function(){
        expect(true).toBe(false);
    });
    it("allows resolving with globs", function(){
        var files = jclasspath._resolvePath("hello");
        expect(files.length).toEqual(0);
        
        files = jclasspath.requirePath.resolve("src");
        expect(files[0].toString()).toEqual("src");
    });
});
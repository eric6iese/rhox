package com.rhox.exec;

import static com.rhox.exec.ProcessUtils.LINE_SEPARATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

public class JsTest {

    private Path f = Paths.get("src/main/javascript/shell.js");
    private ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");

    private String dir = TestPrograms.DIR.toAbsolutePath().toString().replace("\\", "/");
    private String cmdErr = TestPrograms.CMD_ERR.stream().map(s -> s.replace("\\", "\\\\"))
            .collect(Collectors.joining("','", "['", "']"));

    @Before
    public void setup() throws Exception {
        String script = Files.lines(f).collect(Collectors.joining(LINE_SEPARATOR));
        script = script.replace("execModule.type(", "Java.type(").substring(script.indexOf("var RhoxShell"));
        eval("var exports = {};", "var module = { exports: exports };", script, "var shell = module.exports;",
                "var exec = shell.System.exec;", "var start = shell.System.start;", "var Redirect = shell.Redirect;",
                "var sh = new shell.Shell();", "sh.dir = '" + dir + "'");
    }

    @Test
    public void callErr() {
        Object out = eval("sh.err = [];", "var i = sh.exec(" + cmdErr + ");", "Java.to(sh.err, 'java.util.List');");
        assertThat(out).isEqualTo(ErrProgram.ERRORS);
    }

    @Test
    public void callErrWithInlineRedirect() {
        Object out = eval("var err = [];", "exec(" + cmdErr + ", {dir: '" + dir + "', err: err});",
                "Java.to(err, 'java.util.List')");
        assertThat(out).isEqualTo(ErrProgram.ERRORS);
    }

    @Test
    public void callErrWithProcess() {
        Object out = eval("var proc = sh.start(" + cmdErr + ", {err: Redirect.PIPE});", "var lines = [];",
                "proc.err.forEach(function(line){lines.push(line);});", "proc.waitFor();",
                "Java.to(lines, 'java.util.List')");
        assertThat(out).isEqualTo(ErrProgram.ERRORS);
    }

    private Object eval(String... lines) {
        return eval(Arrays.stream(lines).collect(Collectors.joining(LINE_SEPARATOR)));
    }

    private Object eval(String script) {
        try {
            return nashorn.eval(script + LINE_SEPARATOR);
        } catch (ScriptException e) {
            throw new AssertionError(e);
        }
    }

}

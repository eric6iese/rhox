package com.rhox.shell;

import static com.rhox.shell.ErrProgram.ERRORS;
import static com.rhox.shell.ProcessUtils.LINE_SEPARATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

public class JsTest {

    private Path scriptfile = Paths.get("src/main/javascript/shell.js");
    private ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");

    private String dir = TestPrograms.DIR.toAbsolutePath().toString().replace('\\', '/');
    private String cmdErr = TestPrograms.CMD_ERR.stream().map(s -> s.replace("\\", "\\\\"))
            .collect(Collectors.joining("','", "['", "']"));

    @Before
    public void setup() throws Exception {
        String script = Files.lines(scriptfile).collect(Collectors.joining(LINE_SEPARATOR));
        script = script.replace("execModule.type(", "Java.type(").substring(script.indexOf("var RhoxShell"));
        eval("var exports = {};", "var module = { exports: exports };", script, "var shell = module.exports;",
                "var exec = shell.exec;", "var start = shell.start;", "var Redirect = shell.Redirect;",
                "var sh = new shell.Shell();", "sh.dir = '" + dir + "'");
    }

    @Test
    public void callErr() {
        Object out = eval("sh.err = [];", "var i = sh.exec(" + cmdErr + ");", "Java.to(sh.err, 'java.util.List');");
        assertThat(out).isEqualTo(ERRORS);
    }

    @Test
    public void callErrWithInlineRedirect() {
        Object out = eval("var err = [];", "exec(" + cmdErr + ", {dir: '" + dir + "', err: err});",
                "Java.to(err, 'java.util.List')");
        assertThat(out).isEqualTo(ERRORS);
    }

    @Test
    public void callErrWithProcess() {
        Object out = eval("var proc = sh.start(" + cmdErr + ", {err: Redirect.PIPE});", "var lines = [];",
                "proc.err.forEach(function(line){lines.push(line);});", "proc.waitFor();",
                "Java.to(lines, 'java.util.List')");
        assertThat(out).isEqualTo(ERRORS);
    }

    @Test
    public void callErrWithProcessStart() {
        Object out = eval("var proc = start(" + cmdErr + ", {err: Redirect.PIPE, dir: '" + dir + "'});", "var lines = [];",
                "proc.err.forEach(function(line){lines.push(line);});", "proc.waitFor();",
                "Java.to(lines, 'java.util.List')");
        assertThat(out).isEqualTo(ERRORS);
    }

    @Test
    public void testRedirectAliases() throws IOException {
        String arg = "blah";
        List<String> out = Arrays.asList(arg);
        List<String> fullout = Stream.concat(out.stream(), ERRORS.stream()).collect(Collectors.toList());

        List<String> result = exec("{'&out':out}", arg);
        assertThat(result).isEqualTo(fullout);

        // File based redirection
        Path file = Files.createTempFile(null, null);
        file.toFile().deleteOnExit();
        String f = "'" + file.toString().replace('\\', '/') + "'";

        exec("{'&>':" + f + "}", arg);
        assertThat(Files.readAllLines(file)).isEqualTo(fullout);

        exec("{'>':" + f + "}", arg);
        assertThat(Files.readAllLines(file)).isEqualTo(out);

        List<String> lines = Arrays.asList("hello");
        Files.write(file, lines);
        exec("{'&>>':" + f + "}", arg);
        assertThat(Files.readAllLines(file))
                .isEqualTo(Stream.concat(lines.stream(), fullout.stream()).collect(Collectors.toList()));

        Files.write(file, Arrays.asList("hello"));
        exec("{'>>':" + f + "}", arg);
        assertThat(Files.readAllLines(file))
                .isEqualTo(Stream.concat(lines.stream(), out.stream()).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private List<String> exec(String config, String args) {
        String cmd = cmdErr.substring(0, cmdErr.length() - 1) + ",'" + args + "']";
        Object out = eval("sh.err = []; var out = [];", "sh.exec(" + cmd + ", " + config + ");",
                "Java.to(out, 'java.util.List')");
        return (List<String>) out;
    }

    @Test
    public void testRedirectSetters() throws IOException {
        Object out;
        out = eval("var out = ['a'];", "sh['&out']=out;", "Java.to(sh.out, 'java.util.List');");
        assertThat(out).isEqualTo(Arrays.asList("a"));
        assertThat(eval("sh.redirectErr")).isEqualTo(true);
        eval("sh.redirectErr=false");

        File file = Files.createTempFile(null, null).toFile();
        file.deleteOnExit();
        String f = "'" + file.toString().replace('\\', '/') + "'";

        out = eval("sh['>>']=" + f, "sh.out");
        assertThat(out).isEqualTo(Redirect.appendTo(file));
        assertThat(eval("sh.redirectErr")).isEqualTo(false);

        out = eval("sh['&>>']=" + f, "sh.out");
        assertThat(out).isEqualTo(Redirect.appendTo(file));
        assertThat(eval("sh.redirectErr")).isEqualTo(true);
        eval("sh.redirectErr=false");

        out = eval("sh['>']=" + f, "sh.out");
        assertThat(out).isEqualTo(Redirect.to(file));
        assertThat(eval("sh.redirectErr")).isEqualTo(false);

        out = eval("sh['&>']=" + f, "sh.out");
        assertThat(out).isEqualTo(Redirect.to(file));
        assertThat(eval("sh.redirectErr")).isEqualTo(true);
        eval("sh.redirectErr=false");

        out = eval("sh['<']=" + f, "sh.in");
        assertThat(out).isEqualTo(Redirect.from(file));
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

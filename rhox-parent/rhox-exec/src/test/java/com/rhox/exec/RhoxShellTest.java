package com.rhox.exec;

import static com.rhox.exec.ProcessUtils.LINE_SEPARATOR;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class RhoxShellTest {

    RhoxShell sh = new RhoxShell();

    Path f1 = sh.createTempFile();
    Path f2 = sh.createTempFile();

    String s1 = "hello goodbye";
    String s2 = "hey jude";
    String s3 = "we can work it out";

    List<String> l1 = new ArrayList<>(asList("1", "2", "3"));
    List<String> l2 = new ArrayList<>(asList("a", "b", "c"));
    List<String> l3 = new ArrayList<>(asList("f", "g", "h", "j"));

    @Before
    public void before() throws IOException {
        Files.write(f1, s1.getBytes());
        Files.write(f2, s2.getBytes());
        sh.setDir(TestPrograms.DIR);
    }

    @Test
    public void copyChars() {
        sh.copy(f1, f2);
        assertThat(f2).hasContent(s1);

        sh.copy(s3, f2);
        assertThat(f2).hasContent(s3);

        l1.clear();
        sh.copy(f1, l1);
        assertThat(l1).containsExactly(s1);
    }

    /**
     * This needs further discussion: Is appending always the best choice, or only for output and error? Or maybe not
     * even then?
     * <p>
     * Matters for files and collections, not for streams (which cannot be rewinded)
     */
    @Test
    public void copyAlwaysAppends() {
        l2 = new ArrayList<>(l1);
        l1.add(s1);
        sh.copy(f1, l2);
        assertThat(l2).isEqualTo(l1);
        sh.copy(f1, l2);
        assertThat(l2.size()).isGreaterThan(l1.size());
    }

    private List<String> cmdOut = Stream.concat(TestPrograms.CMD_OUT.stream(), Stream.of("hello"))
            .collect(Collectors.toList());
    private String cmdOutResult = "hello";

    @Test
    public void execEcho() throws Exception {
        StringBuilder out = new StringBuilder();
        sh.setOut(out);
        int rc = sh.exec(cmdOut);
        assertThat(rc).isEqualTo(0);
        assertThat(out.toString().trim()).isEqualTo(cmdOutResult);
    }

    @Test
    public void pipeEcho() throws Exception {
        sh.setOut(ProcessRedirect.PIPE);
        RhoxProcess proc = sh.start(cmdOut);
        BufferedReader br = proc.getReader();
        String out = br.readLine();
        assertNull(br.readLine());
        assertThat(proc.waitForOrDestroy(1000, TimeUnit.SECONDS)).isEqualTo(0);
        assertThat(out).isEqualTo(cmdOutResult);
    }

    @Test
    public void echoFile() throws Exception {
        sh.setOut(f1);
        sh.exec(cmdOut);
        String out = Files.readAllLines(f1).stream().collect(Collectors.joining());
        assertThat(out).isEqualTo(cmdOutResult);
    }

    @Test
    public void customConfigWins() throws Exception {
        sh.setOut(f1);
        ProcessConfig cfg = new ProcessConfig();
        StringBuilder out = new StringBuilder();
        cfg.setOut(out);
        sh.exec(cmdOut, cfg);
        assertThat(out.toString().trim()).isEqualTo(cmdOutResult);
        assertThat(f1).hasContent(s1);
    }

    private List<String> cmdIn = TestPrograms.CMD_IN;
    private List<String> cmdInInput = Arrays.asList("b", "a", "c", "k");
    private String cmdInInputString = cmdInInput.stream().collect(Collectors.joining(LINE_SEPARATOR));
    private List<String> cmdInResult = Arrays.asList("a", "b", "c", "k");

    @Test
    public void cmdInString() throws Exception {
        testCmdIn(cmdInInputString);
    }

    @Test
    public void cmdInReader() throws Exception {
        testCmdIn(new StringReader(cmdInInputString));
    }

    @Test
    public void cmdInStream() throws Exception {
        testCmdIn(new ByteArrayInputStream(cmdInInputString.getBytes()));
    }

    @Test
    public void cmdInPipe() throws Exception {
        sh.setIn(Redirect.PIPE);
        sh.setOut(new ArrayList<>());
        RhoxProcess proc = sh.start(cmdIn);
        try (PrintWriter writer = proc.getWriter()) {
            cmdInInput.forEach(writer::println);
        }
        proc.waitFor();
        assertThat(sh.getOut()).isEqualTo(cmdInResult);
    }

    private void testCmdIn(Object input) throws Exception {
        sh.setIn(cmdInInput);
        sh.setOut(new ArrayList<>());
        sh.exec(cmdIn);
        assertThat(sh.getOut()).isEqualTo(cmdInResult);
    }
}

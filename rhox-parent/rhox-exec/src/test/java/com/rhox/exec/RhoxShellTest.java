package com.rhox.exec;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
     * This needs further discussion: Is appending always the best choice, or
     * only for output and error? Or maybe not even then?
     * <p>
     * Matters for files and collections, not for streams (which cannot be
     * rewinded)
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

    @Test
    public void execEcho() throws Exception {
        StringBuilder out = new StringBuilder();
        sh.setOut(out);
        int rc = sh.exec("cmd /c echo hello");
        assertThat(rc).isEqualTo(0);
        assertThat(out.toString().trim()).isEqualTo("hello");
    }

}

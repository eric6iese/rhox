package com.rhox.exec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;

public class RhoxShellTest {

    RhoxShell sh = new RhoxShell();

    Path f1 = sh.createTempFile();
    Path f2 = sh.createTempFile();

    String s1 = "hello goodbye";
    String s2 = "hey jude";
    String s3 = "we can work it out";

    @Before
    public void before() throws IOException {
        Files.write(f1, s1.getBytes());
        Files.write(f2, s2.getBytes());
    }

    @Test
    public void copyAnything() {
        sh.copy(f1, f2);
    }
}

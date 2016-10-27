package com.rhox.exec;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class RhoxShellErrorTest {

    RhoxShell sh = new RhoxShell();
    List<String> cmd = TestPrograms.CMD_ERR;

    @Before
    public void init() {
        sh.setDir(TestPrograms.DIR);
    }

    @Test
    public void sendErr() throws Exception {
        sh.exec(cmd);
    }
}

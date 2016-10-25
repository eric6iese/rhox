/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

/**
 *
 * @author giese
 */
public class AbstractShell {

    protected final ProcessConfig config;

    AbstractShell() {
        config = new ProcessConfig();
        config.setIn(ProcessRedirect.INHERIT);
        config.setOut(ProcessRedirect.INHERIT);
        config.setErr(ProcessRedirect.INHERIT);
        config.setRedirectErr(false);
    }

    public Object getIn() {
        return config.getIn();
    }

    public void setIn(Object in) {
        config.setIn(in);
    }

    public void setOut(Object out) {
        config.setOut(out);
    }

    public Object getOut() {
        return config.getOut();
    }

    public void setErr(Object err) {
        config.setErr(err);
    }

    public Object getErr() {
        return config.getErr();
    }

    public void setRedirectErr(boolean redirectErr) {
        config.setRedirectErr(redirectErr);
    }

    public boolean isRedirectErr() {
        return config.getRedirectErr();
    }
}

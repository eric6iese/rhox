/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

import java.nio.charset.Charset;
import static java.util.Objects.requireNonNull;

/**
 *
 * @author giese
 */
public class AbstractShell {

    private static final String LN = System.getProperty("line.separator");

    protected final ProcessConfig config;

    AbstractShell() {
        config = new ProcessConfig();
        config.setIn(ProcessRedirect.INHERIT);
        config.setOut(ProcessRedirect.INHERIT);
        config.setErr(ProcessRedirect.INHERIT);
        config.setRedirectErr(false);
        config.setLineSeparator(LN);
        config.setCharset(Charset.defaultCharset());
    }

    public Object getIn() {
        return config.getIn();
    }

    public void setIn(Object in) {
        config.setIn(requireNonNull(in, "in"));
    }

    public void setOut(Object out) {
        config.setOut(requireNonNull(out, "out"));
    }

    public Object getOut() {
        return config.getOut();
    }

    public void setErr(Object err) {
        config.setErr(requireNonNull(err, "err"));
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

    public void setLineSeparator(String lineSeparator) {
        config.setLineSeparator(requireNonNull(lineSeparator, "lineSeparator"));
    }

    public String getLineSeparator() {
        return config.getLineSeparator();
    }

    public void setCharset(Charset charset) {
        config.setCharset(requireNonNull(charset, "charset"));
    }

    public Charset getCharset() {
        return config.getCharset();
    }

}

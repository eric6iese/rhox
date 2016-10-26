/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

import java.nio.charset.Charset;
import java.nio.file.Path;
import static java.util.Objects.requireNonNull;

/**
 *
 * @author giese
 */
public class AbstractShell implements ProcessContext {

    protected final ProcessConfig config;

    AbstractShell() {
        config = new ProcessConfig();
        config.setIn(ProcessRedirect.INHERIT);
        config.setOut(ProcessRedirect.INHERIT);
        config.setErr(ProcessRedirect.INHERIT);
        config.setRedirectErr(false);
        config.setDir(ProcessUtils.USER_DIR);
        config.setLineSeparator(ProcessUtils.LINE_SEPARATOR);
        config.setCharset(Charset.defaultCharset());
    }

    @Override
    public Object getIn() {
        return config.getIn();
    }

    @Override
    public void setIn(Object in) {
        config.setIn(requireNonNull(in, "in"));
    }

    @Override
    public void setOut(Object out) {
        config.setOut(requireNonNull(out, "out"));
    }

    @Override
    public Object getOut() {
        return config.getOut();
    }

    @Override
    public void setErr(Object err) {
        config.setErr(requireNonNull(err, "err"));
    }

    @Override
    public Object getErr() {
        return config.getErr();
    }

    @Override
    public void setRedirectErr(Boolean redirectErr) {
        config.setRedirectErr(requireNonNull(redirectErr, "redirectErr"));
    }

    @Override
    public Boolean getRedirectErr() {
        return config.getRedirectErr();
    }

    @Override
    public void setDir(Object dir) {
        config.setDir(requireNonNull(dir, "dir"));
    }

    @Override
    public Path getDir() {
        return config.getDir();
    }

    @Override
    public void setLineSeparator(String lineSeparator) {
        config.setLineSeparator(requireNonNull(lineSeparator, "lineSeparator"));
    }

    @Override
    public String getLineSeparator() {
        return config.getLineSeparator();
    }

    @Override
    public void setCharset(Object charset) {
        config.setCharset(requireNonNull(charset, "charset"));
    }

    @Override
    public Charset getCharset() {
        return config.getCharset();
    }
}

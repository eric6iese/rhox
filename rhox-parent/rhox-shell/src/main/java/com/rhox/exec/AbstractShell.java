package com.rhox.exec;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 *
 * @author giese
 */
class AbstractShell implements ProcessContext {

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

    public Object getOut() {
        return config.getOut();
    }

    @Override
    public Object getErr() {
        return config.getErr();
    }

    @Override
    public Boolean getRedirectErr() {
        return config.getRedirectErr();
    }

    @Override
    public Path getDir() {
        return config.getDir();
    }

    @Override
    public String getLineSeparator() {
        return config.getLineSeparator();
    }

    @Override
    public Charset getCharset() {
        return config.getCharset();
    }

    public void setIn(Object in) {
        config.setIn(requireNonNull(in, "in"));
    }

    public void setOut(Object out) {
        config.setOut(requireNonNull(out, "out"));
    }

    public void setErr(Object err) {
        config.setErr(requireNonNull(err, "err"));
    }

    public void setRedirectErr(Boolean redirectErr) {
        config.setRedirectErr(requireNonNull(redirectErr, "redirectErr"));
    }

    public void setDir(Object dir) {
        config.setDir(requireNonNull(dir, "dir"));
    }

    public void setLineSeparator(String lineSeparator) {
        config.setLineSeparator(requireNonNull(lineSeparator, "lineSeparator"));
    }

    public void setCharset(Object charset) {
        config.setCharset(requireNonNull(charset, "charset"));
    }
}

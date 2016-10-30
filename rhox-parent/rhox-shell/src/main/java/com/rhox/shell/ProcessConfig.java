package com.rhox.shell;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Allows it to customize some config attributes when running from within the
 * shell (or from the outside)
 *
 * @author giese
 */
public class ProcessConfig implements Cloneable, ProcessContext {

    private Object in;

    private Object out;

    private Object err;

    /**
     * Marker, used to identify a redirected error stream.
     */
    private Boolean redirectErr;

    private Path dir;

    /**
     * The line separator used by the external process. Used especially for
     * sending piped input to the process, but ignored in most other cases.
     */
    private String lineSeparator;

    /**
     * The charset used by the external process.
     */
    private Charset charset;

    @Override
    public Object getIn() {
        return in;
    }

    @Override
    public Object getOut() {
        return out;
    }

    @Override
    public Object getErr() {
        return err;
    }

    @Override
    public Boolean getRedirectErr() {
        return redirectErr;
    }

    /**
     * the currently set directory, or null if the default workdir should be
     * used.
     */
    @Override
    public Path getDir() {
        return dir;
    }

    @Override
    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setIn(Object in) {
        this.in = in;
    }

    public void setOut(Object out) {
        this.out = out;
    }

    public void setErr(Object err) {
        this.err = err;
    }

    public void setRedirectErr(Boolean redirectErr) {
        this.redirectErr = redirectErr;
    }

    public void setDir(Object dir) {
        this.dir = ProcessUtils.toPath(dir);
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public void setCharset(Object charset) {
        if (charset instanceof CharSequence) {
            charset = Charset.forName(charset.toString());
        }
        this.charset = (Charset) charset;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    protected ProcessConfig clone() {
        try {
            return (ProcessConfig) super.clone();
        } catch (CloneNotSupportedException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /**
     * Uses this configuration as the base to create a new one where all
     * non-null-values of the given are applied.
     */
    protected ProcessConfig merge(ProcessContext in) {
        ProcessConfig cfg = clone();
        if (in != null) {
            merge(in::getDir, cfg::setDir);
            merge(in::getIn, cfg::setIn);
            merge(in::getOut, cfg::setOut);
            merge(in::getErr, cfg::setErr);
            merge(in::getRedirectErr, cfg::setRedirectErr);
            merge(in::getCharset, cfg::setCharset);
            merge(in::getLineSeparator, cfg::setLineSeparator);
        }
        return cfg;
    }

    private <T> void merge(Supplier<T> in, Consumer<T> out) {
        T value = in.get();
        if (value != null) {
            out.accept(value);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ProcessConfig [in=" + in + ", out=" + out + ", err=" + err + ", redirectErr=" + redirectErr + ", dir="
                + dir + ", lineSeparator=" + lineSeparator + ", charset=" + charset + "]";
    }

}

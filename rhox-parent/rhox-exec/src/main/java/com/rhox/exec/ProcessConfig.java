/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Allows it to customize some config attributes when running from within the
 * shell (or from the outside)
 *
 * @author giese
 */
public class ProcessConfig implements Cloneable {

    private Object in;

    private Object out;

    private Object err;

    /**
     * Marker, used to identify a redirected error stream.
     */
    private Boolean redirectErr;

    /**
     * The line separator used by the external process. Used especially for
     * sending piped input to the process, but ignored in most other cases.
     */
    private String lineSeparator;

    /**
     * The charset used by the external process.
     */
    private Charset charset;

    public Object getIn() {
        return in;
    }

    public void setIn(Object in) {
        this.in = in;
    }

    public void setOut(Object out) {
        this.out = out;
    }

    public Object getOut() {
        return out;
    }

    public void setErr(Object err) {
        this.err = err;
    }

    public Object getErr() {
        return err;
    }

    /**
     * If called, then err is redirected to the output stream.<br/>
     * This will overwrite any set error value.
     */
    public void setRedirectErr(Boolean redirectErr) {
        this.err = true;
    }

    public Boolean getRedirectErr() {
        return redirectErr;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

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
    protected ProcessConfig merge(ProcessConfig in) {
        ProcessConfig cfg = clone();
        if (in != null) {
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
}

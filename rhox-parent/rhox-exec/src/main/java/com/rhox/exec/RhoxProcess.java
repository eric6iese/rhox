/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Extends java.lang.process with additional char-based methods.
 *
 * @author giese
 */
public class RhoxProcess extends Process {

    private static final Field LINE_SEPARATOR_FIELD;

    static {
        try {
            LINE_SEPARATOR_FIELD = BufferedWriter.class.getDeclaredField("lineSeparator");
            LINE_SEPARATOR_FIELD.setAccessible(true);
        } catch (NoSuchFieldException unexpected) {
            throw new UnsupportedOperationException(unexpected);
        }
    }

    private final Process process;
    private final Charset charset;
    private final String lineSeparator;
    private final Iterator<Thread> threads;

    RhoxProcess(Process process, Iterator<Thread> threads, Charset charset, String lineSeparator) {
        this.process = process;
        this.threads = threads;
        this.charset = charset;
        this.lineSeparator = lineSeparator;
    }

    @Override
    public int waitFor() throws InterruptedException {
        waitForThreads();
        return process.waitFor();
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        waitForThreads();
        return process.waitFor(timeout, unit);
    }

    private void waitForThreads() throws InterruptedException {
        while (threads.hasNext()) {
            threads.next().join();
        }
    }

    @Override
    public int exitValue() {
        return process.exitValue();
    }

    @Override
    public void destroy() {
        process.destroy();
    }

    @Override
    public Process destroyForcibly() {
        return process.destroyForcibly();
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return process.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return process.getErrorStream();
    }

    public BufferedWriter getWriter() {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(), charset));
        try {
            LINE_SEPARATOR_FIELD.set(writer, lineSeparator);
        } catch (ReflectiveOperationException unexpected) {
            throw new UnsupportedOperationException(unexpected);
        }
        return writer;

    }

    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    public BufferedReader getErrorReader() {
        return new BufferedReader(new InputStreamReader(getErrorStream(), charset));
    }
}

package com.rhox.exec;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extends java.lang.process with additional char-based methods.
 *
 * @author giese
 */
public class RhoxProcess extends Process {

    private final Process process;
    private final OutputStream in;
    private final InputStream out;
    private final InputStream err;
    private final Charset charset;
    private final String lineSeparator;
    private final Iterator<Future<?>> threads;

    RhoxProcess(Process process, OutputStream in, InputStream out, InputStream err, Iterator<Future<?>> threads,
            Charset charset, String lineSeparator) {
        this.process = process;
        this.in = in;
        this.out = out;
        this.err = err;
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

    public int waitForOrKill(long timeout, TimeUnit unit) throws InterruptedException {
        waitFor(timeout, unit);
        if (isAlive()) {
            destroy();
        }
        return exitValue();
    }

    public int waitForOrKill(long millis) throws InterruptedException {
        return waitForOrKill(millis, TimeUnit.MILLISECONDS);
    }

    private void waitForThreads() {
        while (threads.hasNext()) {
            try {
                threads.next().get();
            } catch (InterruptedException | ExecutionException ex) {
                // Later I need to think about what should really happen here.
                // Problem is, I can't do much anyway if one of the processors threads has died
                Logger.getLogger(RhoxProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        if (in == null) {
            throw new UnsupportedOperationException("No piped input");
        }
        return in;
    }

    @Override
    public InputStream getInputStream() {
        if (out == null) {
            throw new UnsupportedOperationException("No piped output");
        }
        return out;
    }

    @Override
    public InputStream getErrorStream() {
        if (err == null) {
            throw new UnsupportedOperationException("No piped error");
        }
        return err;
    }

    /**
     * Gets the piped PrinterWriter which can be used to send output to the stream.
     */
    public ProcessWriter getIn() {
        return new ProcessWriter(getOutputStream(), charset, lineSeparator);
    }

    /**
     * Gets the piped reader for the output.
     */
    public ProcessReader getOut() {
        return new ProcessReader(getInputStream(), charset);
    }

    /**
     * Gets the piped reader for the error output.
     */
    public ProcessReader getErr() {
        return new ProcessReader(getErrorStream(), charset);
    }
}

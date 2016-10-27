package com.rhox.exec;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * An extended print writer for handling of piped processes.
 *
 * @author x002664 (eg)
 */
public class ProcessWriter extends PrintWriter {

    private final String lineSeparator;

    ProcessWriter(OutputStream outputStream, Charset cs, String lineSeparator) {
        super(new OutputStreamWriter(outputStream, cs));
        this.lineSeparator = lineSeparator;
    }

    @Override
    public void println() {
        try {
            synchronized (lock) {
                out.write(lineSeparator);
                out.flush();
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            setError();
        }
    }
}

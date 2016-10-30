package com.rhox.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * A BufferedReader extended with lambda-methods.
 */
public class ProcessReader extends BufferedReader {

    ProcessReader(InputStream is, Charset cs) {
        super(new InputStreamReader(is, cs));
    }

    public void forEach(Consumer<String> lineConsumer) throws IOException {
        while (true) {
            String line = readLine();
            if (line == null) {
                break;
            }
            lineConsumer.accept(line);
        }
    }

}

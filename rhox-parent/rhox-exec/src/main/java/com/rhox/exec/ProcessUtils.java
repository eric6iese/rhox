/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author giese
 */
final class ProcessUtils {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String USER_DIR = System.getProperty("user.dir");

    /**
     * Starts a new Process from the commandline. The command is derived from
     * the arguments, all of them are converted to strings, if necessary.
     */
    public static RhoxProcess start(List<String> args, ProcessConfig config) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (config.getDir() != null) {
            processBuilder.directory(config.getDir().toFile());
        }
        ProcessBuilder.Redirect rIn = createRedirect(config.getIn(), true);
        processBuilder.redirectInput(rIn);

        ProcessBuilder.Redirect rOut = createRedirect(config.getOut(), false);
        processBuilder.redirectOutput(rOut);

        ProcessBuilder.Redirect rErr;
        if (config.getRedirectErr()) {
            rErr = createRedirect(config.getErr(), false);
            processBuilder.redirectError(rErr);
        } else {
            rErr = null;
            processBuilder.redirectErrorStream(true);
        }

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        List<Thread> threads = new ArrayList<>();
        if (rIn == ProcessBuilder.Redirect.PIPE) {
            threads.add(startInputThread("ProcessInput", config, config.getIn(), process.getOutputStream()));
        }
        if (rOut == ProcessBuilder.Redirect.PIPE) {
            threads.add(startOutputThread("ProcessOutput", config, config.getOut(), process.getInputStream()));
        }
        if (rErr == ProcessBuilder.Redirect.PIPE) {
            threads.add(startOutputThread("ProcessError", config, config.getErr(), process.getErrorStream()));
        }
        return new RhoxProcess(process, threads.iterator(), config.getCharset(), config.getLineSeparator());
    }

    /**
     * Creates the appropiate redirect, dependening on the target type.
     */
    private static ProcessBuilder.Redirect createRedirect(Object target, boolean read) {
        if (target instanceof ProcessBuilder.Redirect) {
            return (ProcessBuilder.Redirect) target;
        }
        if (target instanceof Path) {
            Path f = (Path) target;
            return read ? ProcessRedirect.from(f) : ProcessRedirect.to(f);
        }
        if (target instanceof File) {
            File f = (File) target;
            return read ? ProcessRedirect.from(f) : ProcessRedirect.to(f);
        }
        return ProcessRedirect.PIPE;
    }

    private static Thread startInputThread(String name, ProcessConfig config, Object input, OutputStream out) {
        ProcessSource source = ProcessSource.of(input, config.getDir(), config.getCharset());
        StreamSink sink = new StreamSink(out, config.getCharset(), config.getLineSeparator());
        Thread thread = new Thread(() -> source.copyTo(sink), name);
        thread.start();
        return thread;
    }

    private static Thread startOutputThread(String name, ProcessConfig config, Object output, InputStream in) {
        StreamSource source = new StreamSource(in, config.getCharset());
        ProcessSink sink = ProcessSink.of(output, config.getDir(), config.getCharset(), config.getLineSeparator());
        Thread thread = new Thread(() -> source.copyTo(sink), name);
        thread.start();
        return thread;
    }

    public static Path toPath(Object file) {
        if (file instanceof CharSequence) {
            return Paths.get(file.toString());
        }
        if (file instanceof File) {
            return ((File) file).toPath();
        }
        return (Path) file;
    }
}

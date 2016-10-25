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
import java.lang.ProcessBuilder.Redirect;
import static java.lang.ProcessBuilder.Redirect.PIPE;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author giese
 */
final class ProcessUtils {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String USER_DIR = System.getProperty("user.dir");

    private static ExecutorService EXEC = Executors.newCachedThreadPool();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EXEC.shutdown();
            try {
                EXEC.awaitTermination(120, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // Cannot handle during shutdown!
            }
        }, "ExecutionShutdown"));
    }

    /**
     * Starts a new Process from the commandline. The command is derived from
     * the arguments, all of them are converted to strings, if necessary.
     */
    public static RhoxProcess start(List<String> args, ProcessConfig config) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (config.getDir() != null) {
            processBuilder.directory(config.getDir().toFile());
        }
        Object input = config.getIn();
        Object output = config.getOut();
        Object error = config.getErr();

        ProcessBuilder.Redirect rIn = createRedirect(input, true);
        processBuilder.redirectInput(rIn);

        ProcessBuilder.Redirect rOut = createRedirect(output, false);
        processBuilder.redirectOutput(rOut);

        ProcessBuilder.Redirect rErr;
        if (config.getRedirectErr()) {
            rErr = createRedirect(error, false);
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
        List<Future<?>> threads = new ArrayList<>();

        OutputStream in = null;
        if (rIn == PIPE) {
            if (input == PIPE) {
                in = process.getOutputStream();
            } else {
                threads.add(startInputThread(config, input, process.getOutputStream()));
            }
        }

        InputStream out = null;
        if (rOut == PIPE) {
            if (output == PIPE) {
                out = process.getInputStream();
            } else {
                threads.add(startOutputThread(config, output, process.getInputStream()));
            }
        }

        InputStream err = null;
        if (rErr == PIPE) {
            if (error == PIPE) {
                err = process.getErrorStream();
            } else {
                threads.add(startOutputThread(config, error, process.getErrorStream()));
            }
        }
        return new RhoxProcess(process, in, out, err, threads.iterator(), config.getCharset(), config.getLineSeparator());
    }

    /**
     * Creates the appropiate redirect, dependening on the target type.
     */
    private static Redirect createRedirect(Object target, boolean read) {
        if (target instanceof Redirect) {
            return (Redirect) target;
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

    private static Future<?> startInputThread(ProcessConfig config, Object input, OutputStream out) {
        ProcessSource source = ProcessSource.of(input, config.getDir(), config.getCharset());
        StreamSink sink = new StreamSink(out, config.getCharset(), config.getLineSeparator());
        return EXEC.submit(() -> source.copyTo(sink));
    }

    private static Future<?> startOutputThread(ProcessConfig config, Object output, InputStream in) {
        StreamSource source = new StreamSource(in, config.getCharset());
        ProcessSink sink = ProcessSink.of(output, config.getDir(), config.getCharset(), config.getLineSeparator());
        return EXEC.submit(() -> source.copyTo(sink));
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

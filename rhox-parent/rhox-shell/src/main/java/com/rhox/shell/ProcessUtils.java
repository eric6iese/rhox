package com.rhox.shell;

import static java.lang.ProcessBuilder.Redirect.PIPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal engine:<br/>
 * Starts and runs the processes and pipes their in- and outputs.
 */
final class ProcessUtils {

    static final String LINE_SEPARATOR = System.getProperty("line.separator");
    static final String USER_DIR = System.getProperty("user.dir");

    /**
     * All Threads for piping are derived from this pool which is automatically
     * closed when the jvm terminates.
     */
    private static final ExecutorService EXEC;

    static {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        AtomicInteger id = new AtomicInteger();
        EXEC = Executors.newCachedThreadPool(runnable -> {
            Thread t = new Thread(group, runnable, "ProcessIO-" + id.incrementAndGet(), 0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EXEC.shutdown();
            try {
                // there is no timeout until all io has been consumed
                // (expect for an external interrupt)
                EXEC.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // Cannot handle during shutdown!
                // (even guava doesn't do anything here)
            }
        }, "ProcessIO-Shutdown"));
    }

    /**
     * Starts a new Process from the commandline. The command is derived from
     * the arguments, all of them are converted to strings, if necessary.
     */
    public static RhoxProcess start(List<String> args, ProcessContext config) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (config.getDir() != null) {
            processBuilder.directory(config.getDir().toFile());
        }

        Object input = config.getIn();
        ProcessBuilder.Redirect rIn = createRedirect(input, true);
        processBuilder.redirectInput(rIn);

        Object output = config.getOut();
        ProcessBuilder.Redirect rOut = createRedirect(output, false);
        processBuilder.redirectOutput(rOut);

        Object error = config.getErr();
        Boolean redirectErr = config.getRedirectErr();
        ProcessBuilder.Redirect rErr;
        if (redirectErr != null && redirectErr) {
            rErr = null;
            processBuilder.redirectErrorStream(true);
        } else {
            rErr = createRedirect(error, false);
            processBuilder.redirectError(rErr);
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
        return new RhoxProcess(process, in, out, err, threads.iterator(), config.getCharset(),
                config.getLineSeparator());
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

    private static Future<?> startInputThread(ProcessContext config, Object input, OutputStream out) {
        ProcessSource source = ProcessSource.of(input, config.getDir(), config.getCharset());
        return EXEC.submit(() -> {
            try (OutputStream os = out) {
                StreamSink sink = new StreamSink(os, config.getCharset(), config.getLineSeparator());
                source.copyTo(sink);
            }
            return null;
        });
    }

    private static Future<?> startOutputThread(ProcessContext config, Object output, InputStream in) {

        ProcessSink sink = ProcessSink.of(output, config.getDir(), config.getCharset(), config.getLineSeparator());
        return EXEC.submit(() -> {
            try (InputStream is = in) {
                StreamSource source = new StreamSource(is, config.getCharset());
                source.copyTo(sink);
            }
            return null;
        });
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

package com.rhox.exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * A combination of a process-execution library and a file handler used for easier scripting.<br/>
 *
 * @author giese
 */
public class RhoxShell {

    /**
     * Marker, used to identify a redirected error stream.
     */
    private static final Object REDIRECT_ERR = new Object();

    private static final String LN = System.getProperty("line.separator");
    private static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    private Path dir = USER_DIR;

    /**
     * The line separator used by the external process. Used especially for sending piped input to the process, but
     * ignored in most other cases.
     */
    private String lineSeparator = LN;

    /**
     * The charset used by the external process.
     */
    private Charset charset = Charset.defaultCharset();

    private Object in;

    private Object out;

    private Object err;

    /**
     * Modifies the working directory for all processes started afterwards. Setting it to null will restore the default.
     *
     * @param the
     *            work dir
     */
    public void setDir(String dir) {
        if (dir == null) {
            this.dir = null;
        } else if (this.dir == null) {
            this.dir = Paths.get(dir).toAbsolutePath();
        } else {
            this.dir = this.dir.resolve(dir).toAbsolutePath();
        }
    }

    /**
     * the currently set directory, or null if the default workdir should be used.
     */
    public String getDir() {
        return dir == null ? null : dir.toString();
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
    public void redirectErr() {
        this.err = REDIRECT_ERR;
    }

    /**
     * Creates the appropiate redirect, dependening on the target type.
     */
    private static Redirect createRedirect(Object target, boolean read) {
        if (target instanceof Path) {
            File f = ((Path) target).toFile();
            return read ? Redirect.from(f) : Redirect.appendTo(f);
        }
        return target != null ? Redirect.PIPE : Redirect.INHERIT;
    }

    /**
     * Starts a new Process from the commandline.
     */
    public Process start(String command) {
        return start(toArgs(command));
    }

    /**
     * Converts the command into an List-based args using the same implementation as Runtime.exec().
     */
    private List<String> toArgs(String command) {
        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }
        return Arrays.asList(cmdarray);
    }

    /**
     * Starts a new Process from the commandline. The command is derived from the arguments, all of them are converted
     * to strings, if necessary.
     */
    public Process start(List<?> command) {
        List<String> args = command.stream().map(Objects::toString).collect(Collectors.toList());
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (dir != null) {
            processBuilder.directory(dir.toFile());
        }
        Redirect rIn = createRedirect(in, true);
        processBuilder.redirectInput(rIn);

        Redirect rOut = createRedirect(out, false);
        processBuilder.redirectOutput(rOut);

        Redirect rErr;
        if (err != REDIRECT_ERR) {
            rErr = createRedirect(err, false);
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
        if (rIn == Redirect.PIPE) {
            startInputThread("ProcessInput", in, process.getOutputStream());
        }
        if (rOut == Redirect.PIPE) {
            startOutputThread("ProcessOutput", out, process.getInputStream());
        }
        if (rErr == Redirect.PIPE) {
            startOutputThread("ProcessError", err, process.getErrorStream());
        }
        return process;
    }

    private void startInputThread(String name, Object input, OutputStream out) {
        ProcessSource source = ProcessSource.of(input, dir, charset);
        StreamSink sink = new StreamSink(out, charset, lineSeparator);
        new Thread(() -> source.copyTo(sink), name).start();
    }

    private void startOutputThread(String name, Object output, InputStream in) {
        StreamSource source = new StreamSource(in, charset);
        ProcessSink sink = ProcessSink.of(out, dir, charset, lineSeparator);
        new Thread(() -> source.copyTo(sink), name).start();
    }

    public int exec(String command) {
        return exec(toArgs(command));
    }

    public int exec(List<String> command) {
        try {
            return start(command).waitFor();
        } catch (InterruptedException unexpected) {
            throw new IllegalStateException(unexpected);
        }
    }

    /**
     * Creates a new empty temporary file.<br/>
     * The file is automatically removed as soon as the jvm terminates.
     */
    public Path createTempFile() {
        try {
            Path file = Files.createTempFile(null, null);
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Copies all data from any kind of input into the output.
     */
    public void copy(Object input, Object output) {
        ProcessSource source = ProcessSource.of(input, dir, charset);
        ProcessSink sink = ProcessSink.of(output, dir, charset, lineSeparator);
        source.copyTo(sink);
    }
}
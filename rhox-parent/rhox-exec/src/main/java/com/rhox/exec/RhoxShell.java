package com.rhox.exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import static java.lang.System.out;
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
 * A combination of a process-execution library and a file handler used for
 * easier scripting.<br/>
 *
 * @author giese
 */
public class RhoxShell extends AbstractShell {

    private static final String LN = System.getProperty("line.separator");
    private static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    private Path dir = USER_DIR;

    /**
     * The line separator used by the external process. Used especially for
     * sending piped input to the process, but ignored in most other cases.
     */
    private String lineSeparator = LN;

    /**
     * The charset used by the external process.
     */
    private Charset charset = Charset.defaultCharset();

    /**
     * Modifies the working directory for all processes started afterwards.
     * Setting it to null will restore the default.
     *
     * @param the work dir
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
     * the currently set directory, or null if the default workdir should be
     * used.
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

    public int exec(String command) throws InterruptedException {
        return exec(command, null);
    }

    public int exec(String command, ProcessConfig config) throws InterruptedException {
        return exec(toArgs(command), config);
    }

    public int exec(List<String> command) throws InterruptedException {
        return exec(command, null);
    }

    public int exec(List<String> command, ProcessConfig config) throws InterruptedException {
        return start(command, config).waitFor();
    }

    /**
     * Starts a new Process from the commandline.
     */
    public RhoxProcess start(String command) {
        return start(command, null);
    }

    public RhoxProcess start(String command, ProcessConfig config) {
        return start(toArgs(command), config);
    }

    public RhoxProcess start(List<?> command) {
        return start(command, null);
    }

    /**
     * Starts a new Process from the commandline. The command is derived from
     * the arguments, all of them are converted to strings, if necessary.
     */
    public RhoxProcess start(List<?> command, ProcessConfig config) {
        config = config == null ? this.config : this.config.merge(config);
        List<String> args = command.stream().map(Objects::toString).collect(Collectors.toList());
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (dir != null) {
            processBuilder.directory(dir.toFile());
        }
        Redirect rIn = createRedirect(config.getIn(), true);
        processBuilder.redirectInput(rIn);

        Redirect rOut = createRedirect(config.getOut(), false);
        processBuilder.redirectOutput(rOut);

        Redirect rErr;
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
        if (rIn == Redirect.PIPE) {
            startInputThread("ProcessInput", config.getIn(), process.getOutputStream());
        }
        if (rOut == Redirect.PIPE) {
            startOutputThread("ProcessOutput", config.getOut(), process.getInputStream());
        }
        if (rErr == Redirect.PIPE) {
            startOutputThread("ProcessError", config.getErr(), process.getErrorStream());
        }
        return new RhoxProcess(process, charset, lineSeparator);
    }

    /**
     * Converts the command into an List-based args using the same
     * implementation as Runtime.exec().
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

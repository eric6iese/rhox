package com.rhox.exec;

import java.io.IOException;
import java.io.UncheckedIOException;
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

    private static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

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
        List<String> args = command.stream().map(Objects::toString).collect(Collectors.toList());
        config = config == null ? this.config : this.config.merge(config);
        return ProcessUtils.start(args, config);
    }

    /**
     * Converts the command into an List-based args using the same
     * implementation as Runtime.exec().
     */
    private static List<String> toArgs(String command) {
        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }
        return Arrays.asList(cmdarray);
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
        ProcessSource source = ProcessSource.of(input, config.getDir(), getCharset());
        ProcessSink sink = ProcessSink.of(output, config.getDir(), getCharset(), getLineSeparator());
        source.copyTo(sink);
    }
}

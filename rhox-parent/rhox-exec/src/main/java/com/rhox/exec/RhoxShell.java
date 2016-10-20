package com.rhox.exec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A combination of a process-execution library and a file handler used for
 * easier scripting.<br/>
 *
 * @author giese
 */
public class RhoxShell {

    /**
     * Marker, used to identify a redirected error stream.
     */
    private static final Object REDIRECT_ERR = new Object();

    private Path dir;

    private Object in;

    private Object out;

    private Object err;

    /**
     * The line separator used by the external process. Used especially for
     * sending piped input to the process, but ignored in most other cases.
     */
    private String lineSeparator = System.getProperty("line.separator");

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

    public void setIn(Path in) {
        this.in = in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public void setIn(Supplier<String> in) {
        this.in = in;
    }

    public void setOut(Path out) {
        this.out = out;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public void setOut(Consumer<String> out) {
        this.out = out;
    }

    public void setErr(Path err) {
        this.err = err;
    }

    public void setErr(OutputStream err) {
        this.err = err;
    }

    public void setErr(Consumer<String> err) {
        this.err = err;
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
     * Starts a new Process from the commandline.<br/>
     * Converts the command into an List-based args using the same
     * implementation as Runtime.exec().
     */
    public Process start(String command) {
        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }
        return start(Arrays.asList(cmdarray));
    }

    /**
     * Starts a new Process from the commandline. The command is derived from
     * the arguments, all of them are converted to strings, if necessary.
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
        new Thread(() -> {
            try (OutputStream os = out) {
                copyStream(input, os);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }, name).start();
    }

    private void startOutputThread(String name, Object output, InputStream in) {
        new Thread(() -> {
            try (InputStream is = in) {
                copyStream(is, output);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }, name).start();
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
        if (input instanceof Path) {
            copyFileToOutput((Path) input, output);
        } else if (output instanceof Path) {
            copyInputToFile(input, (Path) output);
        } else {
            copyStream(input, output);
        }
    }

    private Object normalizeInput(Object input) {
        if (input instanceof File) {
            return ((File) input).toPath();
        }
        if (input instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) input);
        }
        if (input instanceof CharSequence) {
            return new StringReader(input.toString());
        }
        return input;
    }

    private Object normalizeOutput(Object output) {
        if (output instanceof File) {
            return ((File) output).toPath();
        }
        if (output instanceof Appendable) {
            return newLineWriter((Appendable) output);
        }
        return output;
    }

    private void copyFileToOutput(Path infile, Object output) {
        if (output instanceof Path) {
            try {
                Files.copy(infile, (Path) output);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        } else if (output instanceof OutputStream) {
            try {
                Files.copy(infile, (OutputStream) output);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        } else {
            try (BufferedReader reader = Files.newBufferedReader(infile)) {
                copyStream(reader, output);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
    }

    private void copyInputToFile(Object input, Path outfile) {
        if (input instanceof InputStream) {
            try {
                Files.copy((InputStream) input, outfile);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        } else {
            try (BufferedWriter writer = Files.newBufferedWriter(outfile)) {
                copyStream(input, writer);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
    }

    /**
     * Copies all data from an input streamlike resource to the output
     * streamlike resource. Used for piped-io with processes.<br/>
     * Please note that this method currently is entirely focussed on char-data
     * and cannot cope with binary data in any way. Neither line-endings on
     * different os'es or charsets are supported, instead always the system
     * default is used. That's by design for now...
     * <p>
     * Note as well that is implementation is horribly slow as it is, but this
     * really shouldn't matter here, since this will rarely be the bottleneck in
     * the IPC.
     */
    private void copyStream(Object input, Object output) {
        if (input instanceof InputStream) {
            InputStream is = (InputStream) input;
            if (output instanceof OutputStream) {
                copyBinary(is, (OutputStream) output);
                return;
            }
            input = newLineReader(new InputStreamReader(is, charset));
        }
        @SuppressWarnings("unchecked")
        Supplier<String> reader = (Supplier<String>) input;

        if (output instanceof OutputStream) {
            output = newLineWriter(new OutputStreamWriter((OutputStream) output, charset));
        }
        @SuppressWarnings("unchecked")
        Consumer<String> writer = (Consumer<String>) output;
        copyLines(reader, writer);
    }

    private static final int BUF_SIZE = 1024 * 8;

    private void copyBinary(InputStream from, OutputStream to) {
        try {
            byte[] buf = new byte[BUF_SIZE];
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                synchronized (this) {
                    to.write(buf, 0, r);
                    to.flush();
                }
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Writes all lines from the supplier to the consumer.
     */
    private void copyLines(Supplier<String> reader, Consumer<String> writer) {
        while (true) {
            String line = reader.get();
            if (line == null) {
                break;
            }
            synchronized (this) {
                writer.accept(line);
            }
        }
    }

    /**
     * Creates a new line-based supplier out of the inputstream reader.
     */
    protected Supplier<String> newLineReader(Reader reader) {
        BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        return () -> {
            try {
                return br.readLine();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        };
    }

    /**
     * Creates a new line-based writer out of the appendable. If the appendable
     * is a java.io.writer, then Autoflushing is done.
     */
    protected Consumer<String> newLineWriter(Appendable appendable) {
        if (appendable instanceof Writer) {
            Writer writer = (Writer) appendable;
            return line -> {
                try {
                    writer.append(line);
                    writer.append(lineSeparator);
                    writer.flush();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            };
        }
        return line -> {
            try {
                appendable.append(line);
                appendable.append(lineSeparator);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        };
    }
}

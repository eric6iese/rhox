package com.rhox.exec;

import java.io.BufferedReader;
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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A ProcessExecutor Factory, simplified in many ways for shell-scripting. Its
 * main properties are dir (workdir), in, out, err.
 *
 * @author giese
 */
public class RhoxProcessBuilder {

    private Path dir;

    private Object in;

    private Object out;

    private Object err;

    private String lineSeparator = System.getProperty("line.separator");

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
        } else if (dir == null) {
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

    public void setIn(File in) {
        setIn(in.toPath());
    }

    public void setIn(Path in) {
        this.in = in;
    }

    public void setIn(Iterable<String> in) {
        Iterator<String> itr = in.iterator();
        setIn(() -> itr.hasNext() ? itr.next() : null);
    }

    public void setIn(Supplier<String> in) {
        this.in = in;
    }

    public void setIn(String in) {
        setIn(new StringReader(in));
    }

    public void setIn(Reader in) {
        this.in = in;
    }

    public void setOut(File out) {
        setOut(out.toPath());
    }

    public void setOut(Path out) {
        this.out = out;
    }

    public void setOut(Appendable out) {
        this.out = out;
    }

    public void setOut(Collection<String> out) {
        this.out = out;
    }

    public void setOut(Consumer<String> out) {
        this.out = out;
    }

    public void setErr(File err) {
        setErr(err.toPath());
    }

    public void setErr(Path err) {
        this.err = err;
    }

    public void setErr(Appendable err) {
        this.err = err;
    }

    public void setErr(List<String> err) {
        this.err = err;
    }

    public void setErr(Consumer<String> err) {
        this.err = err;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Creates the appropiate redirect, dependening on the target type.
     */
    private ProcessBuilder.Redirect createRedirect(Object target, boolean read) {
        if (target instanceof Path) {
            File f = ((Path) target).toFile();
            return read ? ProcessBuilder.Redirect.from(f) : ProcessBuilder.Redirect.appendTo(f);
        }
        return target != null ? ProcessBuilder.Redirect.PIPE : ProcessBuilder.Redirect.INHERIT;
    }

    public Process start(String commandline) {
        return start(Arrays.asList(commandline.split(" ")));
    }

    public Process start(List<String> commandline) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (dir != null) {
            processBuilder.directory(dir.toFile());
        }
        ProcessBuilder.Redirect rIn = createRedirect(in, true);
        ProcessBuilder.Redirect rOut = createRedirect(out, false);
        ProcessBuilder.Redirect rErr = createRedirect(err, false);

        processBuilder.redirectInput(rIn)
                .redirectOutput(rOut).redirectError(rErr);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        if (rIn == ProcessBuilder.Redirect.PIPE) {
            startThread(() -> copyStream(in, process.getOutputStream()));
        }
        if (rOut == ProcessBuilder.Redirect.PIPE) {
            startThread(() -> copyStream(process.getInputStream(), out));
        }
        if (rErr == ProcessBuilder.Redirect.PIPE) {
            startThread(() -> copyStream(process.getErrorStream(), err));
        }
        return process;
    }

    /**
     * Executes multiple commands as a script file from the commandline.
     *
     * @param script the command script as String, String[], File oder Path.
     * @return a new process instance which allows waitFor() and destroy().
     */
    public Process startScript(CharSequence script) {
        return startScript(Arrays.asList(script.toString()));
    }

    public Process startScript(List<String> script) {
        Path file;
        try {
            file = Files.createTempFile(null, null);
            file.toFile().deleteOnExit();
            Files.write(file, script, charset);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return startScript(file);
    }

    public Process startScript(File script) {
        return startScript(script.toPath());
    }

    public Process startScript(Path script) {
        script = script.toAbsolutePath();
        if (!Files.isRegularFile(script)) {
            throw new IllegalArgumentException(
                    "File does not exist: " + script);
        }
        // windows-only variant!
        return start(Arrays.asList("cmd", "/c", script.toString()));
    }

    private void startThread(Runnable runner) {
        new Thread(runner).start();
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
        if (input instanceof InputStream && output instanceof OutputStream) {
            copyBinary((InputStream) input, (OutputStream) input);
            return;
        }
        if (input instanceof Supplier) {
            input = ((Collection<String>) input).stream().map(Object::toString).collect(Collectors.joining(lineSeparator));
        }
        if (input instanceof InputStream) {
            input = new InputStreamReader((InputStream) input);
        }
        if (!(input instanceof Reader)) {
            throw new IllegalStateException(
                    "Invalid Input: " + input);
        }
        BufferedReader reader = new BufferedReader((Reader) input);
        // convert and check the output for the best writing strategy
        if (output instanceof OutputStream) {
            output = new OutputStreamWriter((OutputStream) output);
        }
        copy(reader, output);
    }

    private void copyBinary(InputStream from, OutputStream to) {
        try {
            byte[] buf = new byte[1024 * 8];
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                to.write(buf, 0, r);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private void copyChars(Reader from, Appendable to) {
        try {
            char[] buf = new char[1024 * 8];
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                to.append(CharBuffer.wrap(buf, 0, r));
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private void copy(BufferedReader reader, Object writer) {
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                synchronized (writer) {
                    if (writer instanceof Collection) {
                        ((Collection<Object>) writer).add(line);
                    } else if (writer instanceof Appendable) {
                        ((Appendable) writer).append(line).append(lineSeparator);
                    } else {
                        throw new IllegalArgumentException("Invalid Output: " + writer);
                    }
                }
            }
            if (writer instanceof Writer) {
                ((Writer) writer).flush();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}

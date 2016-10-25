package com.rhox.exec;

import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

abstract class ProcessSink {

    public static ProcessSink of(Object output, Path dir, Charset charset, String lineSeparator) {
        // Files
        if (output instanceof File) {
            return new PathSink((File) output, dir, charset, lineSeparator);
        }
        if (output instanceof Path) {
            return new PathSink((Path) output, dir, charset, lineSeparator);
        }

        // Binary data
        if (output instanceof OutputStream) {
            return new StreamSink((OutputStream) output, charset, lineSeparator);
        }

        // Character Data
        if (output instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> collection = (Collection<String>) output;
            return new LineWriterSink(collection);
        }
        if (output instanceof Appendable) {
            return new LineWriterSink((Appendable) output, lineSeparator);
        }
        if (output instanceof Consumer) {
            @SuppressWarnings("unchecked")
            Consumer<String> consumer = (Consumer<String>) output;
            return new LineWriterSink(consumer);
        }

        throw new IllegalArgumentException("Invalid type: " + output);
    }

    static LineWriter asLineWriter(Appendable appendable, String lineSeparator) {
        return line -> {
            try {
                appendable.append(line);
                appendable.append(lineSeparator);
                if (appendable instanceof Flushable) {
                    ((Flushable) appendable).flush();
                }
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        };
    }
}

class PathSink extends ProcessSink {

    private final Path path;
    private final Charset charset;
    private final String lineSeparator;

    PathSink(File out, Path dir, Charset charset, String lineSeparator) {
        this(out.toPath(), dir, charset, lineSeparator);
    }

    PathSink(Path out, Path dir, Charset charset, String lineSeparator) {
        this.path = dir.resolve(out);
        this.charset = charset;
        this.lineSeparator = lineSeparator;
    }

    public LineWriter asLineWriter() {
        Writer writer;
        try {
            writer = Files.newBufferedWriter(path, charset);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return new LineWriter() {
            @Override
            public void accept(String line) {
                try {
                    writer.write(line);
                    writer.write(lineSeparator);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }

            @Override
            public void close() {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }
        };
    }

    public Path getPath() {
        return path;
    }
}

class StreamSink extends ProcessSink {

    private final OutputStream stream;
    private final Charset charset;
    private final String lineSeparator;

    StreamSink(OutputStream out, Charset charset, String lineSeparator) {
        this.stream = out;
        this.charset = charset;
        this.lineSeparator = lineSeparator;
    }

    public OutputStream getStream() {
        return stream;
    }

    public LineWriter asLineWriter() {
        return asLineWriter(new OutputStreamWriter(stream, charset), lineSeparator);
    }
}

class LineWriterSink extends ProcessSink {

    private final LineWriter writer;

    LineWriterSink(Collection<String> out) {
        this(out::add);
    }

    LineWriterSink(Appendable out, String lineSeparator) {
        this(asLineWriter(out, lineSeparator));
    }

    LineWriterSink(Consumer<String> out) {
        this(out::accept);
    }

    LineWriterSink(LineWriter out) {
        this.writer = out;
    }

    public LineWriter getWriter() {
        return writer;
    }
}

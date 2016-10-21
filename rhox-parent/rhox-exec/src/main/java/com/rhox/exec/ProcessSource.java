package com.rhox.exec;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.function.Supplier;

abstract class ProcessSource {

    public static ProcessSource of(Object input, Path dir, Charset charset) {
        // Files
        if (input instanceof File) {
            return new PathSource(dir, (File) input, charset);
        }
        if (input instanceof Path) {
            return new PathSource(dir, (Path) input, charset);
        }

        // Binary Data
        if (input instanceof byte[]) {
            return new StreamSource((byte[]) input, charset);
        }
        if (input instanceof InputStream) {
            return new StreamSource((InputStream) input, charset);
        }

        // Character Data
        if (input instanceof CharSequence) {
            return new LineSource((CharSequence) input);
        }
        if (input instanceof Reader) {
            return new LineSource((Reader) input);
        }
        if (input instanceof Iterable) {
            @SuppressWarnings("unchecked")
            Iterable<String> iterable = (Iterable<String>) input;
            return new LineSource(iterable);
        }
        if (input instanceof Iterator) {
            @SuppressWarnings("unchecked")
            Iterator<String> iterator = (Iterator<String>) input;
            return new LineSource(iterator);
        }
        if (input instanceof Supplier) {
            @SuppressWarnings("unchecked")
            Supplier<String> supplier = (Supplier<String>) input;
            return new LineSource(supplier);
        }

        throw new IllegalArgumentException("Invalid type: " + input);
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
    public void copyTo(ProcessSink output) {
        if (output instanceof PathSink) {
            copyTo((PathSink) output);
        } else if (output instanceof StreamSink) {
            copyTo((StreamSink) output);
        } else {
            copyTo((LineWriterSink) output);
        }
    }

    public abstract void copyTo(PathSink output);

    public abstract void copyTo(StreamSink output);

    public abstract void copyTo(LineWriterSink output);
}

class PathSource extends ProcessSource {

    private final Path path;
    private final Charset charset;

    PathSource(Path dir, File in, Charset charset) {
        this(dir, in.toPath(), charset);
    }

    PathSource(Path dir, Path in, Charset charset) {
        this.path = dir.resolve(in);
        this.charset = charset;
    }

    @Override
    public void copyTo(PathSink output) {
        try {
            Files.copy(path, output.getPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public void copyTo(StreamSink output) {
        try {
            Files.copy(path, output.getStream());
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public void copyTo(LineWriterSink output) {
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            LineSource lines = new LineSource(reader);
            lines.copyTo(output);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}

class StreamSource extends ProcessSource {

    private final InputStream stream;
    private final Charset charset;

    StreamSource(byte[] in, Charset charset) {
        this(new ByteArrayInputStream(in), charset);
    }

    StreamSource(InputStream in, Charset charset) {
        this.stream = in;
        this.charset = charset;
    }

    @Override
    public void copyTo(PathSink output) {
        try {
            Files.copy(stream, output.getPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private static final int BUF_SIZE = 1024 * 8;

    @Override
    public void copyTo(StreamSink output) {
        OutputStream out = output.getStream();
        try {
            byte[] buf = new byte[BUF_SIZE];
            while (true) {
                int r = stream.read(buf);
                if (r == -1) {
                    break;
                }
                out.write(buf, 0, r);
            }
            out.flush();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public void copyTo(LineWriterSink output) {
        LineSource lines = new LineSource(new InputStreamReader(stream, charset));
        lines.copyTo(output);
    }
}

class LineSource extends ProcessSource {

    private final Supplier<String> supplier;

    LineSource(CharSequence in) {
        this(new StringReader(in.toString()));
    }

    LineSource(Reader in) {
        this(newLineSupplier(in));
    }

    LineSource(Iterable<String> in) {
        this(in.iterator());
    }

    LineSource(Iterator<String> in) {
        this(newLineSupplier(in));
    }

    LineSource(Supplier<String> in) {
        this.supplier = in;
    }

    @Override
    public void copyTo(PathSink output) {
        try (LineWriter writer = output.asLineWriter()) {
            copyLines(writer);
        }
    }

    @Override
    public void copyTo(StreamSink output) {
        try (LineWriter writer = output.asLineWriter()) {
            copyLines(writer);
        }
    }

    /**
     * Writes all lines from the supplier to the consumer.
     */
    @Override
    public void copyTo(LineWriterSink output) {
        try (LineWriter writer = output.getWriter()) {
            copyLines(writer);
        }
    }

    /**
     * Reads line by line from in to out.
     */
    private void copyLines(LineWriter out) {
        while (true) {
            String line = supplier.get();
            if (line == null) {
                break;
            }
            out.accept(line);
        }
    }

    /**
     * Creates a new line-based supplier out of the inputstream reader.
     */
    private static Supplier<String> newLineSupplier(Reader reader) {
        BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        return () -> {
            try {
                return br.readLine();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        };
    }

    private static <E> Supplier<E> newLineSupplier(Iterator<E> iterator) {
        return () -> iterator.hasNext() ? iterator.next() : null;
    }

}

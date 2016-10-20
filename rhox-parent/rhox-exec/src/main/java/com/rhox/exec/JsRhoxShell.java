package com.rhox.exec;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Extends the Shell with countless overloads to allow for a simpler scripting
 * support in Nashorn.
 */
public class JsRhoxShell extends RhoxShell {

    public void setDir(File dir) {
        setDir(dir.toPath());
    }

    public void setDir(Path dir) {
        setDir(dir.toString());
    }

    public void setCharset(String charset) {
        setCharset(Charset.forName(charset));
    }

    public void setIn(File in) {
        setIn(in.toPath());
    }

    public void setIn(byte[] in) {
        setIn(new ByteArrayInputStream(in));
    }

    public void setIn(Iterable<String> in) {
        setIn(newLineReader(in));
    }

    public void setIn(String in) {
        setIn(newLineReader(in));
    }

    public void setIn(Reader in) {
        setIn(newLineReader(in));
    }

    public void setOut(File out) {
        setOut(out.toPath());
    }

    public void setOut(Appendable out) {
        setOut(newLineWriter(out));
    }

    public void setOut(Collection<String> out) {
        setOut(out::add);
    }

    public void setErr(File err) {
        setErr(err.toPath());
    }

    public void setErr(Appendable err) {
        setErr(newLineWriter(err));
    }

    public void setErr(Collection<String> err) {
        setErr(err::add);
    }

    private Supplier<String> newLineReader(Iterable<String> iterable) {
        Iterator<String> itr = iterable.iterator();
        return () -> itr.hasNext() ? itr.next() : null;
    }

    private Supplier<String> newLineReader(String content) {
        return newLineReader(new StringReader(content));
    }

    private Consumer<String> newLineWriter(Collection<String> lines) {
        return lines::add;
    }

}

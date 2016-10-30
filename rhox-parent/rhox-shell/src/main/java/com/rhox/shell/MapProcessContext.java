package com.rhox.shell;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * A ProcessContext compatible with more dynamic languages (like js!). It supports all bean-properties of the
 * ProcessContext, and even some more string-based properties typical for commandline scripting. Note that these special
 * operations are tested only after the default properties are used and in the order given here:
 * <ol>
 * <li><code>&amp;out</code> Defines both an out and redirects err to it.</li>
 * <li><code>&gt;&gt;&nbsp;&nbsp;</code> Redirects out to the file given, appends to existing content</li>
 * <li><code>&amp;&gt;&gt;&nbsp;</code> Redirects err to out and then both to the file given, appends to existing
 * content</li>
 * <li><code>&gt;&nbsp;&nbsp;&nbsp;</code> Redirects out to the file given, overwrites existing content</li>
 * <li><code>&amp;&gt;&nbsp;&nbsp;</code> Redirects err to out and then both to the file given, overwrites existing
 * content</li>
 * <li><code>&lt;&nbsp;&nbsp;&nbsp;</code> Sets in to the file given.</li>
 * </ol>
 *
 * @author x002664 (eg)
 */
public class MapProcessContext implements ProcessContext {

    private final Map<String, Object> properties;

    public MapProcessContext(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public Path getDir() {
        return getFile("dir");
    }

    @Override
    public Object getIn() {
        Object in = get("in");
        if (in != null) {
            return in;
        }
        Path file = getFile("<");
        if (file != null) {
            return ProcessRedirect.from(file);
        }
        return null;
    }

    @Override
    public Object getOut() {
        Object out = get("out", "&out");
        if (out != null) {
            return out;
        }
        Path file = get(this::getFile, ">", "&>");
        if (file != null) {
            return ProcessRedirect.to(file);
        }
        file = get(this::getFile, ">>", "&>>");
        if (file != null) {
            return ProcessRedirect.appendTo(file);
        }
        return null;
    }

    @Override
    public Object getErr() {
        return get("err");
    }

    @Override
    public Boolean getRedirectErr() {
        Boolean r = (Boolean) get("redirectErr");
        if (r != null) {
            return r;
        }
        Object v = get("&out", "&>", "&>>");
        if (v != null) {
            return true;
        }
        return null;
    }

    @Override
    public String getLineSeparator() {
        Object ln = get("lineSeparator");
        return ln == null ? null : ln.toString();
    }

    @Override
    public Charset getCharset() {
        Object cs = get("charset");
        if (cs instanceof CharSequence) {
            return Charset.forName(cs.toString());
        }
        return (Charset) cs;
    }

    private Object get(String key) {
        return properties.get(key);
    }

    private Path getFile(String key) {
        Object value = get(key);
        return value == null ? null : ProcessUtils.toPath(value);
    }

    private Object get(String... keys) {
        return get(this::get, keys);
    }

    private <T> T get(Function<String, T> mapper, String... keys) {
        for (String key : keys) {
            T value = mapper.apply(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

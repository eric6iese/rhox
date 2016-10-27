package com.rhox.exec;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

public class MapProcessContext implements ProcessContext {

    private final Map<String, Object> properties;

    public MapProcessContext(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public Path getDir() {
        Object dir = properties.get("dir");
        return dir == null ? null : ProcessUtils.toPath(dir);
    }

    @Override
    public Object getIn() {
        return properties.get("in");
    }

    @Override
    public Object getOut() {
        return properties.get("out");
    }

    @Override
    public Object getErr() {
        return properties.get("err");
    }

    @Override
    public Boolean getRedirectErr() {
        return (Boolean) properties.get("redirectErr");
    }

    @Override
    public String getLineSeparator() {
        Object ln = properties.get("lineSeparator");
        return ln == null ? null : ln.toString();
    }

    @Override
    public Charset getCharset() {
        Object cs = properties.get("charset");
        if (cs instanceof CharSequence) {
            return Charset.forName(cs.toString());
        }
        return (Charset) cs;
    }

    @Override
    public void setDir(Object dir) {
        properties.put("dir", dir);
    }

    @Override
    public void setIn(Object in) {
        properties.put("in", in);
    }

    @Override
    public void setOut(Object out) {
        properties.put("out", out);
    }

    @Override
    public void setErr(Object err) {
        properties.put("err", err);
    }

    @Override
    public void setRedirectErr(Boolean redirectErr) {
        properties.put("redirectErr", redirectErr);
    }

    @Override
    public void setLineSeparator(String lineSeparator) {
        properties.put("lineSeparator", lineSeparator);
    }

    @Override
    public void setCharset(Object charset) {
        properties.put("charset", charset);
    }
}

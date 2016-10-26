package com.rhox.exec;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines all properties required for a shell process execution context.
 */
public interface ProcessContext {

    /**
     * the currently set directory, or null if the default workdir should be
     * used.
     */
    Path getDir();

    Object getIn();

    Object getOut();

    Object getErr();

    Boolean getRedirectErr();

    String getLineSeparator();

    Charset getCharset();

    void setDir(Object dir);

    void setIn(Object in);

    /**
     * Nashorn compatibility overload.
     */
    default void setIn(List<String> in) {
        setIn((Object) in);
    }

    /**
     * Nashorn compatibility overload.
     */
    default void setIn(Supplier<String> in) {
        setIn((Object) in);
    }

    void setOut(Object out);

    /**
     * Nashorn compatibility overload.
     */
    default void setOut(List<String> out) {
        setOut((Object) out);
    }

    /**
     * Nashorn compatibility overload.
     */
    default void setOut(Consumer<String> out) {
        setOut((Object) out);
    }

    void setErr(Object err);

    /**
     * Nashorn compatibility overload.
     */
    default void setErr(List<String> err) {
        setErr((Object) err);
    }

    /**
     * Nashorn compatibility overload.
     */
    default void setErr(Consumer<String> err) {
        setErr((Object) err);
    }

    /**
     * If called, then err is redirected to the output stream.<br/>
     * This will overwrite any set error value.
     */
    void setRedirectErr(Boolean redirectErr);

    void setLineSeparator(String lineSeparator);

    void setCharset(Object charset);

}

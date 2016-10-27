package com.rhox.exec;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * Defines all properties required for a shell process execution context.
 */
public interface ProcessContext {

    /**
     * the currently set directory, or null if the default workdir should be used.
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

    void setOut(Object out);

    void setErr(Object err);

    /**
     * If called, then err is redirected to the output stream.<br/>
     * This will overwrite any set error value.
     */
    void setRedirectErr(Boolean redirectErr);

    void setLineSeparator(String lineSeparator);

    void setCharset(Object charset);

}

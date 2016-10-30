package com.rhox.shell;

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

    /**
     * If set, then err is redirected to the output stream.<br/>
     * This will overwrite any set error value.
     */
    Boolean getRedirectErr();

    String getLineSeparator();

    Charset getCharset();
}

package com.rhox.shell;

import java.io.Closeable;
import java.util.function.Consumer;

interface LineWriter extends Closeable, Consumer<String> {

    /**
     * Flushes or closes the underlying resource.
     */
    @Override
    default void close() {
    }
}

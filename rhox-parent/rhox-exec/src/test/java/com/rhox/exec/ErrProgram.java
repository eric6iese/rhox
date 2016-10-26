package com.rhox.exec;

import java.util.Arrays;
import java.util.List;

/**
 * Just sends some messages to out.
 */
class ErrProgram {

    public static final List<String> ERRORS = Arrays.asList("ERR1", "ERR2", "ERR3");

    public static void main(String[] args) {
        ERRORS.forEach(System.err::println);
    }
}

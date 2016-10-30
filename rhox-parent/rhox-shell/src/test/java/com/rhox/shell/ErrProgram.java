package com.rhox.shell;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Just sends some messages to out.
 */
class ErrProgram {

    public static final List<String> ERRORS = Arrays.asList("ERR1", "ERR2", "ERR3");

    public static void main(String[] args) {
        if (args.length > 0) {
            String out = Arrays.stream(args).collect(Collectors.joining(" "));
            System.out.println(out);
        }
        ERRORS.forEach(System.err::println);
    }
}

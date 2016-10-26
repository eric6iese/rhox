package com.rhox.exec;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Echoes to out.
 */
class OutProgram {

    public static void main(String[] args) {
        String out = Arrays.stream(args).collect(Collectors.joining(" "));
        System.out.println(out);
    }
}

package com.rhox.exec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * like cmd-sort: Sorts the std input then sends it to out.
 */
class InProgram {

    public static void main(String[] args) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        List<String> lines = new ArrayList<>();
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            lines.add(line);
        }
        lines.stream().sorted().forEach(System.out::println);
    }
}

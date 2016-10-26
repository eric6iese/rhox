package com.rhox.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Creates test configuration of all three example programs for testing.
 */
public class TestPrograms {

    /**
     * Workingdir for all testprograms.
     */
    public static final Path DIR;

    public static final List<String> CMD_IN;
    public static final List<String> CMD_OUT;
    public static final List<String> CMD_ERR;

    static {
        try {
            DIR = Files.createTempDirectory(null);
            DIR.toFile().deleteOnExit();
            Path dir = DIR;
            for (String part : TestPrograms.class.getPackage().getName().split("\\.")) {
                dir = dir.resolve(part);
                Files.createDirectory(dir);
                dir.toFile().deleteOnExit();
            }

            CMD_IN = createCommand(dir, InProgram.class);
            CMD_OUT = createCommand(dir, OutProgram.class);
            CMD_ERR = createCommand(dir, ErrProgram.class);

        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

    }

    private static List<String> createCommand(Path dir, Class<?> program) throws IOException {
        Path prog = dir.resolve(program.getSimpleName() + ".class");
        Arrays.asList(dir, prog).forEach(f -> f.toFile().deleteOnExit());

        try (InputStream is = program.getResourceAsStream(prog.getFileName().toString())) {
            Files.copy(is, prog);
        }
        prog.toFile().deleteOnExit();
        return Arrays.asList(Paths.get(System.getProperty("java.home"), "bin", "java").toString(), program.getName());
    }

}

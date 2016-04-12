/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.test;

import de.helwich.junit.JasmineDescriber;
import de.helwich.junit.JasmineReporter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

/**
 * This class is a editied and modified copy of the JasmineTestRunner
 from<br/>
 * https://github.com/hhelwich/junit-jasmine-runner/blob/master/src/main/java/de/helwich/junit/JasmineTestRunner.java
 * <br/>
 * I had to do this because I needed a functionality to always load my custom
 * boot-script.<br/>
 * This class will be removed as soon as I find a better (but still elegant) way
 * to do this.<br/>
 */
public class RhoxJasmineTestRunner extends Runner {

    /**
     * All rhox-tests will use the javascript-folder as the default test-folder
     * (instead of js).
     */
    static final String TEST_DIR = "src/test/javascript";

    /**
     * All rhox-tests are expected to end with this suffix (before .js). This
     * prevents manual naming of them.
     */
    static final Set<String> SUFFIXES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("Test.js", "Spec.js")));

    private final Class<?> testClass;
    private final ScriptEngine nashorn;
    private final JasmineReporter reporter;
    private final Description description;

    public RhoxJasmineTestRunner(Class<?> testClass) {
        try {
            this.testClass = testClass;

            ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
            nashorn = scriptEngineManager.getEngineByName("nashorn");
            if (nashorn == null) {
                throw new RuntimeException("please use java 8");
            }

            evalResource("/de/helwich/junit/timer.js");

            evalResource("/jasmine/jasmine.js");
            evalResource("/jasmine/boot.js");

            JasmineDescriber describer = (JasmineDescriber) nashorn.eval("jasmine.junitDescriber = new (Java.type(\""
                    + JasmineDescriber.class.getName() + "\")); ");
            describer.setRootName(testClass.getName());

            evalResource("/de/helwich/junit/describer.js");

            Path dir = projectDir();
            Path bootstrapJs = projectDir().resolve("../globals/bootstrap.js");

            evalPath(bootstrapJs);

            evalTests();

            description = describer.getDescription();
            describer.disable();
            reporter = (JasmineReporter) nashorn.eval("jasmine.junitReporter = new (Java.type(\""
                    + JasmineReporter.class.getName() + "\")); ");
            reporter.setDescription(description);
            evalResource("/de/helwich/junit/reporter.js");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Description getDescription() {
        return description;
    }

    private Path projectDir() {
        String relPath = testClass.getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../");
        return targetDir.getAbsoluteFile().toPath();
    }

    private void evalResource(String name) {
        URL url = testClass.getResource(name);
        load(url);
    }

    private void evalPath(Path file) {
        URL url;
        try {
            file = file.toAbsolutePath().toRealPath();
            url = file.toUri().toURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        load(url);
    }

    private void load(URL resource) {
        String src = resource.toExternalForm();
        try {
            nashorn.eval("load('" + src + "')");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private void evalTests() {
        Path dir = projectDir().resolve(TEST_DIR);
        List<Path> scripts;
        try (Stream<Path> stream = Files.walk(dir)) {
            scripts = stream.filter(f -> {
                if (Files.isRegularFile(f)) {
                    String name = f.getFileName().toString();
                    for (String suffix : SUFFIXES) {
                        if (name.endsWith(suffix)) {
                            return true;
                        }
                    }
                }
                return false;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        scripts.forEach(this::evalPath);
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            runThrows(notifier);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public void runThrows(RunNotifier notifier) throws ScriptException {
        reporter.setNotifier(notifier);
        nashorn.eval("jasmine.getEnv().execute();");
        nashorn.eval("setTimeout.wait()");
    }
}

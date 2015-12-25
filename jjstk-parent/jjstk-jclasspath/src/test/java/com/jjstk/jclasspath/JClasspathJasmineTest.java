package com.jjstk.jclasspath;

import com.jjstk.test.JjstkJasmineTestRunner;
import de.helwich.junit.JasmineTest;
import org.junit.runner.RunWith;

@RunWith(JjstkJasmineTestRunner.class)
@JasmineTest(
        // cfg
        srcDir = "/src/main/javascript",
        testDir = "/src/test/javascript",
        browser = false,
        // tests
        test = {"jclasspathSpec"}
)
public class JClasspathJasmineTest {

}

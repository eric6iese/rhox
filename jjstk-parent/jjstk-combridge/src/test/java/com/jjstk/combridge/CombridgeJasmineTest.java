package com.jjstk.combridge;

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
        test = {"combridgeSpec"}
)
public class CombridgeJasmineTest {

}

package org.partiql.transpiler.test

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import java.io.File

class TranspilerTestRunner {

    private val log = TranspilerTestLog(System.out)

    fun run() {
        log.info("Executing transpiler test suite")

        log.info("Loading test suite")
        val suites = loadSuites()

        for (suite in suites) {
            log.debug("Suite ${suite.name}")
            for (test in suite.tests) {
                log.debug("\t ${test.key}")
                log.debug("\t SQL: ${test.value.statement}")
                log.debug("\t Schema: ${test.value.schema}")
            }
        }
    }

    private fun loadSuites(): List<TranspilerTestSuite> {
        val testDirUri = TranspilerTest::class.java.getResource("/test/tests")?.toURI()!!
        log.info("Found tests in $testDirUri")
        val testDir = File(testDirUri.path)
        val testFiles = testDir.listFiles()!!
        log.info("Loading tests")
        return testFiles.map {
            val text = it.readText()
            val ion = loadSingleElement(text)
            assert(ion is StructElement) { "Test suite file must be a single struct" }
            TranspilerTestSuite.load(ion as StructElement)
        }
    }
}

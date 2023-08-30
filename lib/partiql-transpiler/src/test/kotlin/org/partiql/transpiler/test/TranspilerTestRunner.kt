package org.partiql.transpiler.test

import java.nio.file.Path

/**
 * Basic test runner to get things started.
 *
 * Nice to haves would be
 *  - Report generation
 *  - JUnit test provider wrapping each suite's tests
 *  -
 *
 */
class TranspilerTestRunner(root: Path? = null) {

    private val log = TranspilerTestLog(System.out)

    private val provider = TranspilerTestProvider(root)

    fun run() {
        log.info("Executing transpiler test suite")

        log.info("Loading test suite")
        val suites = provider.suites()
        for (suite in suites) {
            log.debug("Suite ${suite.name}")
            for (test in suite.tests) {
                log.debug("\t ${test.key}")
                log.debug("\t SQL: ${test.value.statement}")
                log.debug("\t Schema: ${test.value.schema}")
            }
        }

        log.info("Executing test suite")
        for (suite in suites) {
            execute(suite)
        }
        log.info("Done")
    }

    private fun execute(suite: TranspilerTestSuite) {

        for ((name, test) in suite.tests) {
        }
    }
}

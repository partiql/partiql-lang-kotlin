package org.partiql.transpiler.test

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import org.partiql.planner.PartiQLPlanner
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.targets.PartiQLTarget
import org.partiql.transpiler.test.plugin.TpPlugin
import java.io.File
import kotlin.io.path.toPath

/**
 * Basic test runner to get things started.
 *
 * Nice to haves would be
 *  - Report generation
 *  - JUnit test provider wrapping each suite's tests
 *  -
 *
 */
class TranspilerTestRunner {

    private val log = TranspilerTestLog(System.out)

    fun run() {
        log.info("Executing transpiler test suite")

        log.info("Loading test suite")
        val suites = loadSuites()

        // debug only
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

    private fun loadSuites(): List<TranspilerTestSuite> {
        val testDirUri = TranspilerTest::class.java.getResource("/tests")?.toURI()!!
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

    private fun execute(suite: TranspilerTestSuite) {
        val root = TranspilerTest::class.java.getResource("/catalog")!!.toURI().toPath()
        val plugin = TpPlugin(root)
        val transpiler = PartiQLTranspiler(listOf(plugin))
        // TODO replace targets
        val target = PartiQLTarget

        for ((name, test) in suite.tests) {
            val testName = "${suite.name}__$name"
            val session = PartiQLPlanner.Session(
                queryId = "q__$testName",
                userId = "transpiler_test_runner",
                currentCatalog = suite.session.catalog,
                currentDirectory = suite.session.path,
                catalogConfig = suite.session.config,
            )
            try {
                val result = transpiler.transpile(test.statement, target, session)
                log.debug("RESULT: ${result.output}")
            } catch (ex: Throwable) {
                log.error("FAILED: $testName")
                log.debug(ex.stackTraceToString())
            }
        }
    }
}

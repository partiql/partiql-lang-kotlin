package org.partiql.transpiler.test.targets.trino

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Assumptions
import org.partiql.planner.test.PlannerTest
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.targets.trino.TrinoTarget
import org.partiql.transpiler.test.TranspilerTestFactory
import kotlin.io.path.toPath
import kotlin.test.assertEquals

class TrinoTargetTestFactory : TranspilerTestFactory<String>(TrinoTarget) {

    private val suites: Map<String, TrinoTargetTestSuite>

    init {
        val testDir = TrinoTargetTest::class.java.getResource("/targets/trino")!!.toURI().toPath()
        val testFiles = testDir.toFile().listFiles()!!
        suites = testFiles.associate {
            val text = it.readText()
            val ion = loadSingleElement(text)
            assert(ion is StructElement) { "Test suite file must be a single struct" }
            val suite = TrinoTargetTestSuite.load(ion as StructElement)
            suite.name to suite
        }
    }

    override fun assert(
        suiteKey: String,
        testKey: String,
        test: PlannerTest,
        result: PartiQLTranspiler.Result<String>,
    ) {
        val expected = lookup(suiteKey, testKey)
        val expectedNormalized = normalize(expected.statement)
        val actualNormalized = normalize(result.output.value)
        assertEquals(expectedNormalized, actualNormalized)
    }

    private fun lookup(suiteKey: String, testKey: String): TrinoTargetTest {
        val suite = suites[suiteKey]
        Assumptions.assumeTrue(suite != null)
        val test = suite!!.tests[testKey]
        Assumptions.assumeTrue(test != null)
        return test!!
    }

    /**
     * We're comparing string equality now.
     */
    private fun normalize(query: String): String = query.lines().joinToString(" ") { it.trim() }.trim()
}

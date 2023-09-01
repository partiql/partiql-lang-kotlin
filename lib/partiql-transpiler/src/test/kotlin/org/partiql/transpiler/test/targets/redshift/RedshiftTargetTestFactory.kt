package org.partiql.transpiler.test.targets.redshift

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Assumptions
import org.partiql.planner.test.PlannerTest
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.targets.redshift.RedshiftTarget
import org.partiql.transpiler.test.TranspilerTestFactory
import kotlin.io.path.toPath
import kotlin.test.assertEquals

class RedshiftTargetTestFactory : TranspilerTestFactory<String>(RedshiftTarget) {

    private val suites: Map<String, RedshiftTargetTestSuite>

    init {
        val testDir = RedshiftTargetTest::class.java.getResource("/targets/redshift")!!.toURI().toPath()
        val testFiles = testDir.toFile().listFiles()!!
        suites = testFiles.associate {
            val text = it.readText()
            val ion = loadSingleElement(text)
            assert(ion is StructElement) { "Test suite file must be a single struct" }
            val suite = RedshiftTargetTestSuite.load(ion as StructElement)
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

    private fun lookup(suiteKey: String, testKey: String): RedshiftTargetTest {
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

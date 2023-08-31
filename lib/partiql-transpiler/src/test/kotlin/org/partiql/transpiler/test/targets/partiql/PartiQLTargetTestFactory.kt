package org.partiql.transpiler.test.targets.partiql

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.fail
import org.partiql.planner.test.PlannerTest
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.targets.PartiQLTarget
import org.partiql.transpiler.test.TranspilerTestFactory
import kotlin.io.path.toPath

class PartiQLTargetTestFactory : TranspilerTestFactory<String>(PartiQLTarget) {

    private val suites: Map<String, PartiQLTargetTestSuite>

    init {
        val testDir = PartiQLTargetTest::class.java.getResource("/targets/partiql")!!.toURI().toPath()
        val testFiles = testDir.toFile().listFiles()!!
        suites = testFiles.associate {
            val text = it.readText()
            val ion = loadSingleElement(text)
            assert(ion is StructElement) { "Test suite file must be a single struct" }
            val suite = PartiQLTargetTestSuite.load(ion as StructElement)
            suite.name to suite
        }
    }

    override fun assert(suiteKey: String, testKey: String, test: PlannerTest, result: PartiQLTranspiler.Result<String>) {
        val expected = lookup(suiteKey, testKey)
        Assumptions.assumeFalse(expected != null)
        fail { "Test made it this far!!" }
    }

    private fun lookup(suiteKey: String, testKey: String): PartiQLTargetTest? {
        val suite = suites[suiteKey] ?: return null
        return suite.tests[testKey]
    }
}

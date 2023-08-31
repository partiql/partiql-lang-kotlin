package org.partiql.planner.test

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.toPath

class PlannerTestProvider(root: Path? = null) {

    private val suites: List<PlannerTestSuite>

    init {
        val default = PlannerTest::class.java.getResource("/tests")!!.toURI().toPath()
        val testDir = (root ?: default).toFile()
        val testFiles = testDir.listFiles()!!
        suites = testFiles.map {
            val text = it.readText()
            val ion = loadSingleElement(text)
            assert(ion is StructElement) { "Test suite file must be a single struct" }
            PlannerTestSuite.load(ion as StructElement)
        }
    }

    public fun suites(): Stream<PlannerTestSuite> {
        return suites.stream()
    }
}

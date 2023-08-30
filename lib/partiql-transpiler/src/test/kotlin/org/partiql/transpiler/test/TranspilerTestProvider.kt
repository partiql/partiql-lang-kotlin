package org.partiql.transpiler.test

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadSingleElement
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.toPath

class TranspilerTestProvider(root: Path? = null) {

    private val suites: List<TranspilerTestSuite>

    init {
        val default = TranspilerTest::class.java.getResource("/tests")!!.toURI().toPath()
        val testDir = (root ?: default).toFile()
        val testFiles = testDir.listFiles()!!
        suites = testFiles.map {
            val text = it.readText()
            val ion = loadSingleElement(text)
            assert(ion is StructElement) { "Test suite file must be a single struct" }
            TranspilerTestSuite.load(ion as StructElement)
        }
    }

    public fun suites(): Stream<TranspilerTestSuite> {
        return suites.stream()
    }
}

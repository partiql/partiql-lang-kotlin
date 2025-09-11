package org.partiql.runner.report

import com.amazon.ion.IonType
import com.amazon.ion.system.IonTextWriterBuilder
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class ReportGenerator(
    private val engine: String
) : TestWatcher, AfterAllCallback{

    private val REPORT_TAG_PREFIX = "report:"

    data class TestResult(
        var passing: Set<String> = emptySet(),
        var failing: Set<String> = emptySet(),
        var ignored: Set<String> = emptySet()
    )
    private var testsResults: MutableMap<String,TestResult> = mutableMapOf()


    override fun testFailed(context: ExtensionContext?, cause: Throwable?) {
        val tag = getReportTag(context)
        testsResults.getOrPut(tag) { TestResult() }.failing += context?.displayName ?: ""
        super.testFailed(context, cause)
    }

    override fun testAborted(context: ExtensionContext?, cause: Throwable?) {
        val tag = getReportTag(context)
        testsResults.getOrPut(tag) { TestResult() }.ignored += context?.displayName ?: ""

        super.testAborted(context, cause)
    }

    override fun testSuccessful(context: ExtensionContext?) {
        val tag = getReportTag(context)
        testsResults.getOrPut(tag) { TestResult() }.passing += context?.displayName ?: ""
        super.testSuccessful(context)
    }

    override fun afterAll(p0: ExtensionContext?) {
        val basePath = System.getenv("conformanceReportDir") ?: "."
        val dir = Files.createDirectories(Path("$basePath/$engine")).toFile()
        val file = File(dir, "conformance_test_results.ion")
        val outputStream = file.outputStream()
        val writer = IonTextWriterBuilder.pretty().build(outputStream)
        writer.stepIn(IonType.STRUCT) // in: outer struct
        testsResults.keys.forEach { tag ->
            writer.setFieldName(tag) // engine name
            writer.stepIn(IonType.STRUCT) // in: engine struct

            val testResult = testsResults[tag]!!

            // set struct field for passing
            writer.setFieldName("passing")
            writer.stepIn(IonType.LIST)
            testResult.passing.forEach { passingTest ->
                writer.writeString(passingTest)
            }
            writer.stepOut()
            // set struct field for failing
            writer.setFieldName("failing")
            writer.stepIn(IonType.LIST)
            testResult.failing.forEach { failingTest ->
                writer.writeString(failingTest)
            }
            writer.stepOut()

            // set struct field for ignored
            writer.setFieldName("ignored")
            writer.stepIn(IonType.LIST)
            testResult.ignored.forEach { ignoredTest ->
                writer.writeString(ignoredTest)
            }
            writer.stepOut()

            writer.stepOut() // engine
        }

        writer.stepOut() // out: outer struct
    }

    private fun getReportTag(context: ExtensionContext?): String {
        val tags = context?.tags ?: emptyList()
        return tags.single { it.startsWith(REPORT_TAG_PREFIX) }.substring(REPORT_TAG_PREFIX.length)
    }
}

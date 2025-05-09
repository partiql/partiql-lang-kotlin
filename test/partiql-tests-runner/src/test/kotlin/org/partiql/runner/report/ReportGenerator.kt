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
) : TestWatcher, AfterAllCallback {

    private var failingTests = emptySet<String>()
    private var passingTests = emptySet<String>()
    private var ignoredTests = emptySet<String>()

    override fun testFailed(context: ExtensionContext?, cause: Throwable?) {
        failingTests += context?.displayName ?: ""
        super.testFailed(context, cause)
    }

    override fun testAborted(context: ExtensionContext?, cause: Throwable?) {
        ignoredTests += context?.displayName ?: ""
        super.testAborted(context, cause)
    }

    override fun testSuccessful(context: ExtensionContext?) {
        passingTests += context?.displayName ?: ""
        super.testSuccessful(context)
    }

    override fun afterAll(p0: ExtensionContext?) {
        val basePath = System.getenv("conformanceReportDir") ?: "."
        val dir = Files.createDirectories(Path("$basePath/$engine")).toFile()
        val file = File(dir, "conformance_test_results.ion")
        val outputStream = file.outputStream()
        val writer = IonTextWriterBuilder.pretty().build(outputStream)
        writer.stepIn(IonType.STRUCT) // in: outer struct

        // set struct field for passing
        writer.setFieldName("passing")
        writer.stepIn(IonType.LIST)
        passingTests.forEach { passingTest ->
            writer.writeString(passingTest)
        }
        writer.stepOut()
        // set struct field for failing
        writer.setFieldName("failing")
        writer.stepIn(IonType.LIST)
        failingTests.forEach { failingTest ->
            writer.writeString(failingTest)
        }
        writer.stepOut()

        // set struct field for ignored
        writer.setFieldName("ignored")
        writer.stepIn(IonType.LIST)
        ignoredTests.forEach { ignoredTest ->
            writer.writeString(ignoredTest)
        }
        writer.stepOut()

        writer.stepOut() // out: outer struct
    }
}

package org.partiql.testscript.junitRunner

import com.amazon.ion.system.IonSystemBuilder
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import org.partiql.testscript.parser.Parser
import org.partiql.testscript.evaluator.Evaluator
import org.partiql.testscript.evaluator.PtsEquality
import org.partiql.testscript.evaluator.TestFailure
import org.partiql.testscript.evaluator.TestResultSuccess
import org.partiql.testscript.parser.NamedInputStream
import java.io.File
import java.io.FileInputStream
import org.junit.runner.notification.Failure
import org.partiql.testscript.compiler.*
import java.io.FileFilter
import java.lang.AssertionError
import java.lang.IllegalArgumentException

private val ptsFileFilter = FileFilter { file ->
    (file.isFile && file.name.endsWith(".sqlts")) || file.isDirectory
}

/**
 * A Junit4 runner that integrates PTS tests into JUnit.
 */
class PtsRunner(private val testClass: Class<*>) : Runner() {
    private val ion = IonSystemBuilder.standard().build()

    private val testInstance = testClass
            .asSubclass(Junit4PtsTest::class.java)
            .getDeclaredConstructor()
            .newInstance()

    private val parser = Parser(ion)
    private val compiler = Compiler(ion)

    private val evaluator = testInstance.evaluator

    private val ionDocs = testInstance.ptsFilePaths
            .asSequence()
            .map { File(it) }
            .map { listRecursive(it) }
            .flatMap { it.asSequence() }
            .map { NamedInputStream(it.absolutePath, FileInputStream(it)) }
            .toList()

    private val ast = parser.parse(ionDocs)
    private val testExpressions = compiler.compile(ast) 

    override fun run(notifier: RunNotifier) = try {
        val testResults = evaluator.evaluate(testExpressions)

        testResults.forEach {
            val testDescription = Description.createTestDescription(testClass, it.test.id)

            notifier.fireTestStarted(testDescription)

            when (it) {
                is TestResultSuccess -> notifier.fireTestFinished(testDescription)
                is TestFailure -> {
                    val errorMessage = """
                        |Test failed: ${it.test.scriptLocation} - ${it.test.id} 
                        |Expected: ${it.test.expectedMessage()}
                        |  Actual: ${it.actualResult} 
                    """.trimMargin()

                    notifier.fireTestFailure(Failure(testDescription, AssertionError(errorMessage)))
                }
            }
        }
    } catch (e: Exception) {
        notifier.fireTestFailure(Failure(description, e))

        throw e
    }

    override fun getDescription(): Description {
        return Description.createSuiteDescription(testClass)
    }

    override fun testCount(): Int {
        return testExpressions.size
    }

    private fun TestScriptExpression.expectedMessage(): ExpectedResult = when (this) {
        is TestExpression -> this.expected
        is SkippedTestExpression -> this.original.expected
        is AppendedTestExpression -> this.original.expected
    }

    private fun listRecursive(file: File): List<File> = when {
        !file.exists() -> throw IllegalArgumentException("'${file.path}' not found")
        file.isDirectory -> file.listFiles(ptsFileFilter).flatMap { listRecursive(file) }
        file.isFile -> listOf(file)
        else -> throw IllegalArgumentException("couldn't read '${file.path}'. It's neither a file nor a directory")
    }
}

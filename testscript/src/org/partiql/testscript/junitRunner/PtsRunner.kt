package org.partiql.testscript.junitRunner

import com.amazon.ion.system.IonSystemBuilder
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import org.partiql.testscript.parser.Parser
import org.partiql.testscript.evaluator.TestFailure
import org.partiql.testscript.evaluator.TestResultSuccess
import org.partiql.testscript.parser.NamedInputStream
import java.io.File
import java.io.FileInputStream
import org.junit.runner.notification.Failure
import org.partiql.testscript.compiler.*
import org.partiql.testscript.extensions.listRecursive
import org.partiql.testscript.extensions.ptsFileFilter
import java.io.FileFilter
import java.lang.AssertionError
import java.lang.IllegalArgumentException

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
            .map { it.listRecursive(ptsFileFilter) }
            .flatMap { it.asSequence() }
            .map { NamedInputStream(it.absolutePath, FileInputStream(it)) }
            .toList()

    private val ast = parser.parse(ionDocs)
    private val testExpressions = compiler.compile(ast)

    override fun run(notifier: RunNotifier) {
        testExpressions.forEach { testExp ->
            val testResults = evaluator.evaluate(listOf(testExp))
            testResults.forEach {
                val testDescription = Description.createTestDescription(
                    testClass,
                    "${it.test.scriptLocation} ${it.test.id}")

                notifier.fireTestStarted(testDescription)
                try {

                    when (it) {
                        is TestResultSuccess -> { /* intentionally blank */
                        }
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
                catch (e: Exception) {
                    notifier.fireTestFailure(Failure(description, e))
                }
                finally {
                    notifier.fireTestFinished(testDescription)
                }
            }
        }
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
}

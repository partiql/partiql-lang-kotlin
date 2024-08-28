package org.partiql.runner.test

import org.junit.jupiter.api.Assumptions
import org.partiql.lang.eval.CompileOptions
import org.partiql.runner.schema.Assertion
import org.partiql.runner.schema.TestCase

/**
 * TestRunner delegates execution to the underlying TestExecutor, but orchestrates test assertions.
 */
class TestRunner<T, V>(private val factory: TestExecutor.Factory<T, V>) {

    fun test(case: TestCase.Eval, skipList: List<Pair<String, CompileOptions>>) {
        Assumptions.assumeFalse(skipList.contains((Pair(case.name, case.compileOptions))))
        val executor = factory.create(case.env, case.compileOptions)
        val input = case.statement
        run(input, case, executor)
    }

    fun test(case: TestCase.Equiv, skipList: List<Pair<String, CompileOptions>>) {
        Assumptions.assumeFalse(skipList.contains((Pair(case.name, case.compileOptions))))
        val executor = factory.create(case.env, case.compileOptions)
        case.statements.forEach { run(it, case, executor) }
    }

    private fun run(input: String, case: TestCase, executor: TestExecutor<T, V>) {
        when (val assertion = case.assertion) {
            is Assertion.EvaluationSuccess -> {
                val statement = executor.prepare(input)
                val actual = executor.execute(statement)
                val expect = executor.fromIon(assertion.expectedResult)
                if (!executor.compare(actual, expect)) {
                    val ion = executor.toIon(actual)
                    val message = buildString {
                        appendLine("*** EXPECTED != ACTUAL ***")
                        appendLine("Mode     : ${case.compileOptions.typingMode}")
                        appendLine("Expected : ${assertion.expectedResult}")
                        appendLine("Actual   : $ion")
                    }
                    error(message)
                }
            }
            is Assertion.EvaluationFailure -> {
                var thrown: Throwable? = null
                val ion = try {
                    val statement = executor.prepare(input)
                    val actual = executor.execute(statement)
                    executor.toIon(actual)
                } catch (t: Throwable) {
                    thrown = t
                }
                if (thrown == null) {
                    val message = buildString {
                        appendLine("Expected error to be thrown but none was thrown.")
                        appendLine("Actual Result: $ion")
                    }
                    error(message)
                }
            }
        }
    }
}

package org.partiql.runner.test

import org.partiql.lang.eval.CompileOptions
import org.partiql.runner.schema.Assertion
import org.partiql.runner.schema.TestCase

/**
 * TestRunner delegates execution to the underlying TestExecutor, but orchestrates test assertions.
 */
class TestRunner<T, V>(private val factory: TestExecutor.Factory<T, V>) {

    fun test(case: TestCase.Eval, skipList: List<Pair<String, CompileOptions>>) {
        if (skipList.contains((Pair(case.name, case.compileOptions)))) {
            return
        }
        val executor = factory.create(case.env, case.compileOptions)
        val input = case.statement
        run(input, case, executor)
    }

    fun test(case: TestCase.Equiv, skipList: List<Pair<String, CompileOptions>>) {
        if (skipList.contains((Pair(case.name, case.compileOptions)))) {
            return
        }
        val executor = factory.create(case.env, case.compileOptions)
        case.statements.forEach { run(it, case, executor) }
    }

    private fun run(input: String, case: TestCase, executor: TestExecutor<T, V>) {
        val assertion = case.assertion
        try {
            val statement = executor.prepare(input)
            val actual = executor.execute(statement)
            when (assertion) {
                is Assertion.EvaluationSuccess -> {
                    val expect = executor.fromIon(assertion.expectedResult)
                    if (!executor.compare(actual, expect)) {
                        val ion = executor.toIon(actual)
                        error("Expected: ${assertion.expectedResult}\nActual: $ion\nMode: ${case.compileOptions.typingMode}")
                    }
                }
                is Assertion.EvaluationFailure -> {
                    val ion = executor.toIon(actual)
                    error("Expected error to be thrown but none was thrown.\n${case.name}\nActual result: $ion")
                }
            }
        } catch (e: Exception) {
            when (case.assertion) {
                is Assertion.EvaluationSuccess -> throw IllegalStateException("Expected success but exception thrown.", e)
                is Assertion.EvaluationFailure -> {} // skip
            }
        }
    }
}

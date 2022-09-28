package org.partiql.testscript.evaluator

import org.partiql.testscript.compiler.TestScriptExpression

/**
 * Encapsulates the result of evaluating a test expression
 */
sealed class TestResult {
    abstract val test: TestScriptExpression
}

/**
 * A Test evaluation success
 *
 * @param test test that was evaluated
 */
data class TestResultSuccess(override val test: TestScriptExpression) : TestResult()

/**
 * A Test evaluation failure
 * @param test test that was evaluated
 * @param actualResult the actual result which can be an error message or the serialized PartiQL statement result
 * @param reason failure reason
 */
data class TestFailure(
    override val test: TestScriptExpression,
    val actualResult: String,
    val reason: FailureReason
) : TestResult() {

    enum class FailureReason {
        /** Evaluation threw an error when none was expected */
        UNEXPECTED_ERROR,

        /** Evaluation did not threw an error when an error was expected */
        EXPECTED_ERROR_NOT_THROWN,

        /** Actual PartiQL statement result is different than expected */
        ACTUAL_DIFFERENT_THAN_EXPECTED
    }
}

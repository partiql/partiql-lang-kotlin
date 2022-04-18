package org.partiql.lang.eval.evaluatortestframework

import org.opentest4j.AssertionFailedError

internal enum class EvaluatorTestFailureReason {
    FAILED_TO_PARSE_ION_EXPECTED_RESULT,
    FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
    FAILED_TO_EVALUATE_QUERY,
    UNEXPECTED_QUERY_RESULT,
    UNEXPECTED_PERMISSIVE_MODE_RESULT, // <-- only applies to EvaluatorErrorTestCase
    UNEXPECTED_ERROR_CODE,
    UNEXPECTED_ERROR_CONTEXT,
    UNEXPECTED_INTERNAL_FLAG,
    EXPECTED_SQL_EXCEPTION_BUT_THERE_WAS_NONE
}

/**
 * When this exception is thrown, the JUnit runner treats it like any failed assertion because it inherits from
 * [AssertionFailedError].  Because of [reason], the unit tests of [EvaluatorTestAdapter] implementations can assert
 * that test failures happen for a specific reason, instead of just that *any* assertion failed.  Without this, it is
 * extremely easy to have tests that falsely pass because the assertion failure happened for some other reason.
 */
internal class EvaluatorAssertionFailedError(
    val reason: EvaluatorTestFailureReason,
    private val testDetails: String,
    cause: Throwable? = null
) : AssertionFailedError(reason.toString() + testDetails, cause) {
    override val message: String? get() = "Failure reason: $reason\n$testDetails"
}

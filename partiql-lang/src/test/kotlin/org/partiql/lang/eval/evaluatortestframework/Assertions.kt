package org.partiql.lang.eval.evaluatortestframework

import org.partiql.errors.SqlException

internal fun assertEquals(
    expected: Any?,
    actual: Any?,
    reason: EvaluatorTestFailureReason,
    detailsBlock: () -> String
) {
    if (expected != actual) {
        throw EvaluatorAssertionFailedError(reason, detailsBlock())
    }
}

internal fun <T> assertDoesNotThrow(
    reason: EvaluatorTestFailureReason,
    detailsBlock: () -> String,
    block: () -> T
): T {
    try {
        return block()
    } catch (ex: Throwable) {
        throw EvaluatorAssertionFailedError(reason, detailsBlock(), ex)
    }
}

internal inline fun assertThrowsSqlException(
    reason: EvaluatorTestFailureReason,
    detailsBlock: () -> String,
    block: () -> Unit
): SqlException {
    try {
        block()
        // if we made it here, the test failed.
        throw EvaluatorAssertionFailedError(reason, detailsBlock())
    } catch (ex: SqlException) {
        return ex
    }
}

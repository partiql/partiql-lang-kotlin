package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.DATE_ANNOTATION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.TIME_ANNOTATION
import org.partiql.lang.util.propertyValueMapOf

private fun assertTestFails(
    testAdapter: PipelineEvaluatorTestAdapter,
    expectedReason: EvaluatorTestFailureReason,
    tc: EvaluatorTestCase
) {
    val ex = assertThrows<EvaluatorAssertionFailedError> {
        testAdapter.runEvaluatorTestCase(tc, EvaluationSession.standard())
    }
    assertEquals(expectedReason, ex.reason)
}

private fun assertErrorTestFails(
    testAdapter: PipelineEvaluatorTestAdapter,
    expectedReason: EvaluatorTestFailureReason,
    tc: EvaluatorErrorTestCase
) {
    val ex = assertThrows<EvaluatorAssertionFailedError> {
        testAdapter.runEvaluatorErrorTestCase(tc, EvaluationSession.standard())
    }

    assertEquals(expectedReason, ex.reason)
}

/**
 * These are "smoke tests" to ensure that the essential parts of [PipelineEvaluatorTestAdapterTests] are
 * working correctly.
 */
class PipelineEvaluatorTestAdapterTests {
    private val astPipelineTestAdapter = PipelineEvaluatorTestAdapter(CompilerPipelineFactory())

    private fun assertTestFails(expectedReason: EvaluatorTestFailureReason, tc: EvaluatorTestCase) {
        assertTestFails(astPipelineTestAdapter, expectedReason, tc)
    }

    private fun assertErrorTestFails(expectedReason: EvaluatorTestFailureReason, tc: EvaluatorErrorTestCase) {
        assertErrorTestFails(astPipelineTestAdapter, expectedReason, tc)
    }

    class FooException : Exception()

    //
    // runEvaluatorTestCase
    //

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION`() {
        assertDoesNotThrow("happy path - should not throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1",
                    expectedResult = "1",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - different permissive mode result - ExpectedResultFormat-ION`() {
        assertDoesNotThrow("happy path - should not throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1 + MISSING", // Note:unknown propagation works differently in legacy vs permissive modes.
                    expectedResult = "null",
                    expectedPermissiveModeResult = "$MISSING_ANNOTATION::null",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION (missing)`() {
        assertDoesNotThrow("happy path - should not throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "MISSING",
                    expectedResult = "$MISSING_ANNOTATION::null",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION (date)`() {
        assertDoesNotThrow("happy path - should not throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "DATE '2001-01-01'",
                    expectedResult = "$DATE_ANNOTATION::2001-01-01",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION (time)`() {
        assertDoesNotThrow("happy path - should not throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "TIME '12:12:01'",
                    expectedResult = "$TIME_ANNOTATION::{hour:12,minute:12,second:1.,timezone_hour:null.int,timezone_minute:null.int}",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-STRING mode`() {
        assertDoesNotThrow("happy path - should not throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "SEXP(1, 2, 3)",
                    expectedResult = "`(1 2 3)`", // <-- ExprValue.toString() produces this
                    expectedResultFormat = ExpectedResultFormat.STRING
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result does not match - ExpectedResultFormat-ION mode`() {
        assertTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
            EvaluatorTestCase(
                query = "1",
                expectedResult = "2",
                expectedResultFormat = ExpectedResultFormat.ION
            )
        )
    }

    @Test
    fun `runEvaluatorTestCase - expected result does not match - ExpectedResultFormat-PARTIQL mode`() {
        assertTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
            EvaluatorTestCase(
                query = "1",
                expectedResult = "2",
                expectedResultFormat = ExpectedResultFormat.STRICT
            )
        )
    }

    @Test
    fun `runEvaluatorTestCase - expected result does not match - ExpectedResultFormat-STRING mode`() {
        assertTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
            EvaluatorTestCase(
                query = "1",
                expectedResult = "2",
                expectedResultFormat = ExpectedResultFormat.STRING
            )
        )
    }

    @Test
    fun `runEvaluatorTestCase - syntax error in expected result - ExpectedResultFormat-PARTIQL mode`() {
        assertTestFails(
            EvaluatorTestFailureReason.FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
            EvaluatorTestCase(
                query = "true",
                expectedResult = "!@#$ syntax error intentional",
                expectedResultFormat = ExpectedResultFormat.STRICT
            )
        )
    }

    @Test
    fun `runEvaluatorTestCase - syntax error in expected result - ExpectedResultFormat-ION`() {
        assertTestFails(
            EvaluatorTestFailureReason.FAILED_TO_PARSE_ION_EXPECTED_RESULT,
            EvaluatorTestCase(
                query = "true",
                expectedResult = "!@#$ syntax error intentional",
                expectedResultFormat = ExpectedResultFormat.ION
            )
        )
    }

    @Test
    fun `runEvaluatorTestCase - syntax error in query`() {
        assertTestFails(
            EvaluatorTestFailureReason.FAILED_TO_EVALUATE_QUERY,
            EvaluatorTestCase(
                query = "!@#$ syntax error intentional",
                expectedResult = "Doesn't matter will throw before parsing this"
            )
        )
    }

    @Test
    fun `runEvaluatorTestCase - extraResultAssertions`() {
        assertThrows<FooException>("extraResultAssertions should throw") {
            astPipelineTestAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1",
                    expectedResult = "1"
                ) { throw FooException() },
                EvaluationSession.standard()
            )
        }
    }

    //
    // runEvaluatorErrorTestCase
    //

    @Test
    fun `runEvaluatorErrorTestCase - EXPECTED_SQL_EXCEPTION_BUT_THERE_WAS_NONE`() {
        assertErrorTestFails(
            EvaluatorTestFailureReason.EXPECTED_SQL_EXCEPTION_BUT_THERE_WAS_NONE,
            EvaluatorErrorTestCase(
                query = "1", // <-- does not throw an exception
                expectedErrorCode = ErrorCode.INTERNAL_ERROR,
                // ^ doesn't matter since now test correctly fails before this assertion is made
            )
        )
    }

    @Test
    fun `runEvaluatorErrorTestCase - UNEXPECTED_ERROR_CODE`() {
        assertErrorTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_ERROR_CODE,
            EvaluatorErrorTestCase(
                query = "undefined_function()",
                expectedErrorCode = ErrorCode.INTERNAL_ERROR,
                expectedPermissiveModeResult = "!@# syntax error"

            )
        )
    }

    @Test
    fun `runEvaluatorErrorTestCase - UNEXPECTED_ERROR_CONTEXT`() {
        assertErrorTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_ERROR_CONTEXT,
            EvaluatorErrorTestCase(
                query = "undefined_function()",
                expectedErrorCode = ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
                expectedErrorContext = propertyValueMapOf(1, 2) // <-- incorrect char offset
            )
        )
    }

    @Test
    fun `runEvaluatorErrorTestCase - UNEXPECTED_INTERNAL_FLAG`() {
        assertErrorTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_INTERNAL_FLAG,
            EvaluatorErrorTestCase(
                query = "undefined_function()",
                expectedErrorCode = ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
                expectedInternalFlag = true
            )
        )
    }

    @Test
    fun `runEvaluatorErrorTestCase - FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT`() {
        assertErrorTestFails(
            EvaluatorTestFailureReason.FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
            EvaluatorErrorTestCase(
                query = "upper(1)",
                expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                expectedPermissiveModeResult = "undefined_function()" // <-- throws
            )
        )
    }

    @Test
    fun `runEvaluatorErrorTestCase - UNEXPECTED_PERMISSIVE_MODE_RESULT`() {
        assertErrorTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_PERMISSIVE_MODE_RESULT,
            EvaluatorErrorTestCase(
                query = "upper(1)", // <-- throws in legacy mode but returns missing in permissive
                expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                expectedPermissiveModeResult = "42" // <-- result of upper(1) in permissive mode should be MISSING
            )
        )
    }

    @Test
    fun `runEvaluatorErrorTestCase - additionalExceptionAssertBlock`() {
        // No need to test both test adapters here since additionalExceptionAssertBlock is invoked by
        // PipelineEvaluatorTestAdapter.
        assertThrows<FooException>("additionalExceptionAssertBlock should throw") {
            astPipelineTestAdapter.runEvaluatorErrorTestCase(
                EvaluatorErrorTestCase(
                    query = "undefined_function()",
                    expectedErrorCode = ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
                    additionalExceptionAssertBlock = { throw FooException() }
                ),
                EvaluationSession.standard()
            )
        }
    }
}

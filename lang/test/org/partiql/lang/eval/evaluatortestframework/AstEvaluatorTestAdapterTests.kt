package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.util.propertyValueMapOf

/**
 * These are just some "smoke tests" to ensure that the essential parts of [AstEvaluatorTestAdapterTests] are
 * working correctly.
 */
class AstEvaluatorTestAdapterTests {
    private val testAdapter = AstEvaluatorTestAdapter()

    private fun assertTestFails(expectedReason: EvaluatorTestFailureReason, tc: EvaluatorTestCase) {
        val ex = assertThrows<EvaluatorAssertionFailedError> {
            testAdapter.runEvaluatorTestCase(tc, EvaluationSession.standard())
        }

        assertEquals(expectedReason, ex.reason)
    }

    private fun assertErrorTestFails(expectedReason: EvaluatorTestFailureReason, tc: EvaluatorErrorTestCase) {
        val ex = assertThrows<EvaluatorAssertionFailedError> {
            testAdapter.runEvaluatorErrorTestCase(tc, EvaluationSession.standard())
        }

        assertEquals(expectedReason, ex.reason)
    }

    class FooException : Exception()

    //
    // runEvaluatorTestCase
    //

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
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
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1 + MISSING", // Note:unknown propagation works differently in legacy vs permissive modes.
                    expectedResult = "null",
                    expectedPermissiveModeResult = "\$partiql_missing::null",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION (missing)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "MISSING",
                    expectedResult = "\$partiql_missing::null",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION (date)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "DATE '2001-01-01'",
                    expectedResult = "\$partiql_date::2001-01-01",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION (time)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "TIME '12:12:01'",
                    expectedResult = "\$partiql_time::{hour:12,minute:12,second:1.,timezone_hour:null.int,timezone_minute:null.int}",
                    expectedResultFormat = ExpectedResultFormat.ION
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS mode (int)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1",
                    expectedResult = "1",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - different permissive mode result - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1 + MISSING",
                    expectedResult = "null",
                    // note: In this ExpectedResultFormat we lose the fact that this is MISSING and not an
                    // ordinary Ion null. This is unfortunate, but expected.
                    expectedPermissiveModeResult = "null",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS mode (bag)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "<<1>>",
                    // note: In this ExpectedResultFormat we lose the fact that this a BAG and not an
                    // ordinary Ion list. This is unfortunate, but expected.
                    expectedResult = "[1]",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS mode (missing)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "MISSING",
                    // note: In this ExpectedResultFormat we lose the fact that this MISSING and not an
                    // ordinary Ion null. This is unfortunate, but expected.
                    expectedResult = "null",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS mode (date)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "DATE '2001-01-01'",
                    expectedResult = "\$partiql_date::2001-01-01",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS mode (time)`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "TIME '12:12:01'",
                    expectedResult = "\$partiql_time::{hour:12,minute:12,second:1.,timezone_hour:null.int,timezone_minute:null.int}",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-PARTIQL mode`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "<<1>>",
                    expectedResult = "[1]",
                    expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
                ),
                EvaluationSession.standard()
            )
        }
    }

    @Test
    fun `runEvaluatorTestCase - expected result matches - ExpectedResultFormat-STRING mode`() {
        assertDoesNotThrow("happy path - should not throw") {
            testAdapter.runEvaluatorTestCase(
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
    fun `runEvaluatorTestCase - expected result does not match - ExpectedResultFormat-ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS mode`() {
        assertTestFails(
            EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
            EvaluatorTestCase(
                query = "1",
                expectedResult = "2",
                expectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
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
                expectedResultFormat = ExpectedResultFormat.PARTIQL
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
                expectedResultFormat = ExpectedResultFormat.PARTIQL
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
            testAdapter.runEvaluatorTestCase(
                EvaluatorTestCase(
                    query = "1",
                    expectedResult = "1",
                    extraResultAssertions = { throw FooException() }
                ),
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
        assertThrows<FooException>("additionalExceptionAssertBlock should throw") {
            testAdapter.runEvaluatorErrorTestCase(
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

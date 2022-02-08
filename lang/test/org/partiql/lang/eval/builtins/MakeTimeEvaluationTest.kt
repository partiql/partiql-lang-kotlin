package org.partiql.lang.eval.builtins

import junitparams.Parameters
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.RequiredWithOptional
import org.partiql.lang.eval.call
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.timeValue
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.to

class MakeTimeEvaluationTest : EvaluatorTestBase() {
    private val env = Environment.standard()
    private val subject = MakeTimeExprFunction(valueFactory)

    data class MakeTimeTestCase(val query: String, val expected: String?, val errorCode: ErrorCode?)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForTimeLiterals::class)
    fun testMakeTime(tc: MakeTimeTestCase)  {
        when (tc.errorCode) {
            null -> {
                val originalExprValue = eval(tc.query)
                assertEquals(originalExprValue.toString(), tc.expected)
            }
            else -> {
                try {
                    voidEval(tc.query)
                    fail("Expected evaluation error")
                } catch (e: EvaluationException) {
                    assertEquals(tc.errorCode, e.errorCode)
                }
            }
        }
    }

    private class ArgumentsForTimeLiterals : ArgumentsProviderBase() {
        private fun case(query: String, expected: String) = MakeTimeTestCase(query, expected, null)
        private fun case(query: String, errorCode: ErrorCode) = MakeTimeTestCase(query, null, errorCode)

        override fun getParameters() = listOf(
            case("MAKE_TIME(0,0,0.)", "00:00:00"),
            case("MAKE_TIME(0,0,0.,0)", "00:00:00+00:00"),
            case("MAKE_TIME(23,12,59.12345)", "23:12:59.12345"),
            case("MAKE_TIME(23,12,59.12345,800)", "23:12:59.12345+13:20"),
            case("MAKE_TIME(23,59,59.999999999)", "23:59:59.999999999"),
            case("MAKE_TIME(23,12,59.12345,-800)", "23:12:59.12345-13:20"),
            case("MAKE_TIME(23,59,59.999999999,-1080)", "23:59:59.999999999-18:00"),
            case("MAKE_TIME(23,59,59.999999999,1080)", "23:59:59.999999999+18:00"),
            case("MAKE_TIME(NULL,59,59.999999999)", "NULL"),
            case("MAKE_TIME(23,NULL,59.999999999)", "NULL"),
            case("MAKE_TIME(23,59,NULL)", "NULL"),
            case("MAKE_TIME(NULL,59,59.999999999,1080)", "NULL"),
            case("MAKE_TIME(23,NULL,59.999999999,1080)", "NULL"),
            case("MAKE_TIME(23,59,NULL,1080)", "NULL"),
            case("MAKE_TIME(23,59,59.999999999,NULL)", "NULL"),
            case("MAKE_TIME(MISSING,59,59.999999999,1080)", "NULL"),
            case("MAKE_TIME(23,MISSING,59.999999999,1080)", "NULL"),
            case("MAKE_TIME(23,59,MISSING,1080)", "NULL"),
            case("MAKE_TIME(23,59,MISSING,1080)", "NULL"),
            case("MAKE_TIME(23,59,59.999999999,MISSING)", "NULL"),
            case("MAKE_TIME(23,59,MISSING,NULL)", "NULL"),
            case("MAKE_TIME(24,0,0.)", ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE),
            case("MAKE_TIME(23,60,0.)", ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE),
            case("MAKE_TIME(23,59,60.)", ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE),
            case("MAKE_TIME(23,59,59.999999999,-1081)", ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE),
            case("MAKE_TIME(23,59,59.999999999,1081)", ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE),
            case("MAKE_TIME('23',59,59.999999999,1080)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_TIME(23,'59',59.999999999,1080)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_TIME(23,59,59,1080)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_TIME(23,59,'59.999999999',1080)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_TIME(23,59,59.999999999,'1080')", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_TIME(23.,59,59.999999999,1080)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_TIME('twenty-three',59,59.999999999,1080)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL)
        )
    }

    private fun callMakeTime(vararg args: Any): Time? {
        val required = args.take(3).map { anyToExprValue(it) }
        val opt = args.drop(3)
        val args = when(opt.firstOrNull()) {
            null -> RequiredArgs(required)
            else -> RequiredWithOptional(required, anyToExprValue(opt.first()))
        }
        val value = subject.call(env, args)
        return when(value.type) {
            ExprValueType.NULL -> null
            else -> value.timeValue()
        }
    }

    @Test
    fun wrongTypeOfArgumentForHour() {
        checkInputThrowingEvaluationException("MAKE_TIME('23', 2, 28.0)",
            ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 1,
                Property.ACTUAL_ARGUMENT_TYPES to "STRING",
                Property.FUNCTION_NAME to "make_time",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")
/*
        Assertions.assertThatThrownBy { callMakeTime("23", 2, 28.toBigDecimal()) }
            .hasMessage("Invalid argument type for make_time")
            .isExactlyInstanceOf(EvaluationException::class.java)

 */
    }

    @Test
    fun wrongTypeOfArgumentForMinute() {
        checkInputThrowingEvaluationException("MAKE_TIME(23, 2.0, 28.0)",
            ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 2,
                Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL",
                Property.FUNCTION_NAME to "make_time",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")
    }

    @Test
    fun wrongTypeOfArgumentForSecond() {
        checkInputThrowingEvaluationException("MAKE_TIME(23, 2, 28)",
            ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.EXPECTED_ARGUMENT_TYPES to "DECIMAL",
                Property.ARGUMENT_POSITION to 3,
                Property.ACTUAL_ARGUMENT_TYPES to "INT",
                Property.FUNCTION_NAME to "make_time",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")
    }

    @Test
    fun wrongTypeOfArgumentForTzMinutes() {
        checkInputThrowingEvaluationException("MAKE_TIME(23, 2, 28.0, 12.0)",
            ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            mapOf(Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 4,
                Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL",
                Property.FUNCTION_NAME to "make_time",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")
    }

    fun parametersForMakeTime(): List<Pair<Time?, () -> Time?>> = listOf(
        Time.of(23, 3,26,0,0) to {callMakeTime(23, 3, 26.toBigDecimal())},
        Time.of(23, 3,26,123450000,5) to {callMakeTime(23, 3, 26.12345.toBigDecimal())},
        Time.of(23, 3,26,123450000,5, 630) to {callMakeTime(23, 3, 26.12345.toBigDecimal(), 630)}
    )

    @Test
    @Parameters
    fun makeTime(params: Pair<Time?, () -> Time?>) {
        val (expected, call) = params

        assertEquals(expected, call.invoke())
    }
}
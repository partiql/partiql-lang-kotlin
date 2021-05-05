package org.partiql.lang.eval.builtins

import junitparams.Parameters
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.*
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.ArgumentsProviderBase

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
        val value = subject.call(env, args.map { anyToExprValue(it) }.toList())
        return when(value.type) {
            ExprValueType.NULL -> null
            else -> value.timeValue()
        }
    }

    @Test
    fun lessArguments() {
        Assertions.assertThatThrownBy { callMakeTime(23) }
            .hasMessage("make_time takes between 3 and 4 arguments, received: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun lessArguments2() {
        Assertions.assertThatThrownBy { callMakeTime(23, 2) }
            .hasMessage("make_time takes between 3 and 4 arguments, received: 2")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun moreArguments() {
        Assertions.assertThatThrownBy { callMakeTime(22, 2, 29.123.toBigDecimal(), 128, 45) }
            .hasMessage("make_time takes between 3 and 4 arguments, received: 5")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForHour() {
        Assertions.assertThatThrownBy { callMakeTime("23", 2, 28.toBigDecimal()) }
            .hasMessage("Invalid argument type for make_time")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForMinute() {
        Assertions.assertThatThrownBy { callMakeTime(23, 2.0, 28.toBigDecimal()) }
            .hasMessage("Invalid argument type for make_time")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForSecond() {
        Assertions.assertThatThrownBy { callMakeTime(23, 2, 28) }
            .hasMessage("Invalid argument type for make_time")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForTzMinutes() {
        Assertions.assertThatThrownBy { callMakeTime(23, 2, 28.toBigDecimal(), 12.0) }
            .hasMessage("Invalid argument type for make_time")
            .isExactlyInstanceOf(EvaluationException::class.java)
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
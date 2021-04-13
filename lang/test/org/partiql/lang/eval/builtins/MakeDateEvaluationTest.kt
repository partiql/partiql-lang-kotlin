package org.partiql.lang.eval.builtins

import com.amazon.ion.IonTimestamp
import junitparams.Parameters
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.*
import org.partiql.lang.util.ArgumentsProviderBase
import java.time.LocalDate

class MakeDateEvaluationTest : EvaluatorTestBase() {
    private val env = Environment.standard()
    private val subject = MakeDateExprFunction(valueFactory)

    data class MakeDateTestCase(val query: String, val expected: String?, val errorCode: ErrorCode?)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForDateLiterals::class)
    fun testMakeDate(tc: MakeDateTestCase)  {
        when (tc.errorCode) {
            null -> {
                val originalExprValue = eval(tc.query)
                assertEquals(originalExprValue.toString(), tc.expected)
                if (originalExprValue.type == ExprValueType.DATE) {
                    val (year, month, day) = tc.expected!!.split("-")
                    val dateIonValue = originalExprValue.ionValue
                    dateIonValue as IonTimestamp
                    val timestamp = dateIonValue.timestampValue()
                    assertEquals("Expected year to be $year", year.toInt(), timestamp.year)
                    assertEquals("Expected month to be $month", month.toInt(), timestamp.month)
                    assertEquals("Expected day to be $day", day.toInt(), timestamp.day)
                }
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

    private class ArgumentsForDateLiterals : ArgumentsProviderBase() {
        private fun case(query: String, expected: String) = MakeDateTestCase(query, expected, null)
        private fun case(query: String, errorCode: ErrorCode) = MakeDateTestCase(query, null, errorCode)

        override fun getParameters() = listOf(
            case("MAKE_DATE(2012,02,29)", "2012-02-29"),
            case("MAKE_DATE(2021,02,28)", "2021-02-28"),
            case("MAKE_DATE(2021,03,17) IS DATE", "true"),
            case("MAKE_DATE(NULL,02,28)", "NULL"),
            case("MAKE_DATE(2021,NULL,28)", "NULL"),
            case("MAKE_DATE(2021,02,NULL)", "NULL"),
            case("MAKE_DATE(MISSING,02,28)", "NULL"),
            case("MAKE_DATE(2021,MISSING,28)", "NULL"),
            case("MAKE_DATE(2021,02,MISSING)", "NULL"),
            case("MAKE_DATE(NULL,MISSING,28)", "NULL"),
            case("MAKE_DATE(MISSING,NULL,28)", "NULL"),
            case("MAKE_DATE(MISSING,02,NULL)", "NULL"),
            case("MAKE_DATE(NULL,NULL,28)", "NULL"),
            case("MAKE_DATE(NULL,NULL,28)", "NULL"),
            case("MAKE_DATE(MISSING,MISSING,MISSING)", "NULL"),
            case("MAKE_DATE(2021,02,29)", ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE),
            case("MAKE_DATE(2021,04,31)", ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE),
            case("MAKE_DATE(2021,02,27.999)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_DATE(2021,02,'27')", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_DATE(2021,02,'twenty-seven')", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL),
            case("MAKE_DATE('2021',02,27)", ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL)
        )
    }

    private fun callMakeDate(vararg args: Any): LocalDate? {
        val value = subject.call(env, args.map { anyToExprValue(it) }.toList())
        return when(value.type) {
            ExprValueType.NULL -> null
            else -> value.dateValue()
        }
    }

    @Test
    fun lessArguments() {
        Assertions.assertThatThrownBy { callMakeDate(2021) }
            .hasMessage("make_date takes exactly 3 arguments, received: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun lessArguments2() {
        Assertions.assertThatThrownBy { callMakeDate(2021, 2) }
            .hasMessage("make_date takes exactly 3 arguments, received: 2")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun moreArguments() {
        Assertions.assertThatThrownBy { callMakeDate(2021, 2, 29, 1.123) }
            .hasMessage("make_date takes exactly 3 arguments, received: 4")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForYear() {
        Assertions.assertThatThrownBy { callMakeDate("2021", 2, 28) }
            .hasMessage("Invalid argument type for make_date")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForMonth() {
        Assertions.assertThatThrownBy { callMakeDate(2021, 2.0, 28) }
            .hasMessage("Invalid argument type for make_date")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfArgumentForDay() {
        Assertions.assertThatThrownBy { callMakeDate(2021, 2, "twenty-eight") }
            .hasMessage("Invalid argument type for make_date")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    fun parametersForMakeDate(): List<Pair<LocalDate?, () -> LocalDate?>> = listOf(
        LocalDate.of(2021, 3,26) to {callMakeDate(2021, 3, 26)},
        LocalDate.of(2021, 2,28) to {callMakeDate(2021, 2, 28)},
        LocalDate.of(2020, 2,29) to {callMakeDate(2020, 2, 29)}
    )

    @Test
    @Parameters
    fun makeDate(params: Pair<LocalDate?, () -> LocalDate?>) {
        val (expected, call) = params

        assertEquals(expected, call.invoke())
    }
}
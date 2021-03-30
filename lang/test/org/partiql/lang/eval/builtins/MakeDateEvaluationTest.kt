package org.partiql.lang.eval.builtins

import com.amazon.ion.IonStruct
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.get

class MakeDateEvaluationTest : EvaluatorTestBase() {

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
                    assert(dateIonValue is IonStruct) { "Expected ionValue to be IonStruct" }
                    assertEquals("Expected year to be $year", ion.newInt(year.toInt()), dateIonValue["year"])
                    assertEquals("Expected month to be $month", ion.newInt(month.toInt()), dateIonValue["month"])
                    assertEquals("Expected day to be $day", ion.newInt(day.toInt()), dateIonValue["day"])
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
}
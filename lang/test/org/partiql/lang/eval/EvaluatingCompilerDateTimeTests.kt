package org.partiql.lang.eval

import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.get


class EvaluatingCompilerDateTimeTests : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForDateLiterals::class)
    fun testDate(tc: EvaluatorTestCase)  {
        val originalExprValue = eval(tc.sqlUnderTest)
        assertEquals(originalExprValue.toString(), tc.expectedSql)
        if (originalExprValue.type == ExprValueType.DATE) {
            val (year, month, day) = tc.expectedSql.split("-")
            val dateIonValue = originalExprValue.ionValue
            dateIonValue as IonStruct
            assertEquals("Expected year to be $year", ion.newInt(year.toInt()), dateIonValue["year"])
            assertEquals("Expected month to be $month", ion.newInt(month.toInt()), dateIonValue["month"])
            assertEquals("Expected day to be $day", ion.newInt(day.toInt()), dateIonValue["day"])
        }
    }

    private class ArgumentsForDateLiterals : ArgumentsProviderBase() {
        private fun case(query: String, expected: String) = EvaluatorTestCase(query, expected)

        override fun getParameters() = listOf(
            case("DATE '2012-02-29'", "2012-02-29"),
            case("DATE '2021-02-28'", "2021-02-28"),
            case("DATE '2021-03-17' IS DATE", "true"),
            case("'2021-03-17' IS DATE", "false")
        )
    }
}
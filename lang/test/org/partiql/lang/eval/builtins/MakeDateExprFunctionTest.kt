package org.partiql.lang.eval.builtins

import junitparams.Parameters
import org.assertj.core.api.Assertions
import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.eval.*
import java.time.LocalDate

class MakeDateExprFunctionTest : TestBase() {
    private val env = Environment.standard()

    private val subject = MakeDateExprFunction(valueFactory)

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
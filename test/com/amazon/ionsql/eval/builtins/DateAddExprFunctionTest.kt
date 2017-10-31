package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import junitparams.*
import org.assertj.core.api.Assertions.*
import org.junit.*

class DateAddExprFunctionTest : Base() {
    private fun Any.exprValue() = when (this) {
        is String    -> this.exprValue(ion)
        is Int       -> this.exprValue(ion)
        is Decimal   -> this.exprValue(ion)
        is Timestamp -> this.exprValue(ion)
        else         -> throw RuntimeException()
    }

    private val env = Environment(locals = Bindings.empty(),
                                  session = EvaluationSession.default(),
                                  registers = RegisterBank(0))

    private val subject = DateAddExprFunction(ion)

    private fun callDateAdd(vararg args: Any) = subject.call(env, args.map { it.exprValue() }.toList()).timestampValue()

    @Test
    fun lessArguments() {
        assertThatThrownBy { callDateAdd("year", 1) }
            .hasMessage("date_add takes 3 arguments, received: 2")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test fun moreArguments() {
        assertThatThrownBy { callDateAdd("year", 1, Timestamp.valueOf("2017T"), 2) }
            .hasMessage("date_add takes 3 arguments, received: 4")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfFirstArgument() {
        assertThatThrownBy { callDateAdd(1, 1, Timestamp.valueOf("2017T")) }
            .hasMessage("Expected text: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfSecondArgument() {
        assertThatThrownBy { callDateAdd("year", "a", Timestamp.valueOf("2017T")) }
            .hasMessage("Expected number: \"a\"")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfThirdArgument() {
        assertThatThrownBy { callDateAdd("year", 1, "foo") }
            .hasMessage("Expected timestamp: \"foo\"")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    @Parameters
    fun dateAdd(params: Pair<String, () -> Timestamp>) {
        val (expected, call) = params
        assertEquals(Timestamp.valueOf(expected), call.invoke())
    }

    fun parametersForDateAdd(): List<Pair<String, () -> Timestamp>> = listOf(
        // add 1 at different precision levels
        "2018T" to { callDateAdd("year",        1, Timestamp.valueOf("2017T")) },
        "2017T" to { callDateAdd("month",       1, Timestamp.valueOf("2017T")) },
        "2017T" to { callDateAdd("day",         1, Timestamp.valueOf("2017T")) },
        "2017T" to { callDateAdd("hour",        1, Timestamp.valueOf("2017T")) },
        "2017T" to { callDateAdd("minute",      1, Timestamp.valueOf("2017T")) },
        "2017T" to { callDateAdd("second",      1, Timestamp.valueOf("2017T")) },

        "2018-01T" to { callDateAdd("year",        1, Timestamp.valueOf("2017-01T")) },
        "2017-02T" to { callDateAdd("month",       1, Timestamp.valueOf("2017-01T")) },
        "2017-01T" to { callDateAdd("day",         1, Timestamp.valueOf("2017-01T")) },
        "2017-01T" to { callDateAdd("hour",        1, Timestamp.valueOf("2017-01T")) },
        "2017-01T" to { callDateAdd("minute",      1, Timestamp.valueOf("2017-01T")) },
        "2017-01T" to { callDateAdd("second",      1, Timestamp.valueOf("2017-01T")) },

        "2018-01-02T" to { callDateAdd("year",        1, Timestamp.valueOf("2017-01-02T")) },
        "2017-02-02T" to { callDateAdd("month",       1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-03T" to { callDateAdd("day",         1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-02T" to { callDateAdd("hour",        1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-02T" to { callDateAdd("minute",      1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-02T" to { callDateAdd("second",      1, Timestamp.valueOf("2017-01-02T")) },

        "2018-01-02T03:04Z" to { callDateAdd("year",        1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-02-02T03:04Z" to { callDateAdd("month",       1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-03T03:04Z" to { callDateAdd("day",         1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-02T04:04Z" to { callDateAdd("hour",        1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-02T03:05Z" to { callDateAdd("minute",      1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-02T03:04Z" to { callDateAdd("second",      1, Timestamp.valueOf("2017-01-02T03:04Z")) },

        "2018-01-02T03:04:05Z" to { callDateAdd("year",        1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-02-02T03:04:05Z" to { callDateAdd("month",       1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-03T03:04:05Z" to { callDateAdd("day",         1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-02T04:04:05Z" to { callDateAdd("hour",        1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-02T03:05:05Z" to { callDateAdd("minute",      1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-02T03:04:06Z" to { callDateAdd("second",      1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },

        "2018-01-02T03:04:05.006Z" to { callDateAdd("year",        1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-02-02T03:04:05.006Z" to { callDateAdd("month",       1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-03T03:04:05.006Z" to { callDateAdd("day",         1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-02T04:04:05.006Z" to { callDateAdd("hour",        1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-02T03:05:05.006Z" to { callDateAdd("minute",      1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-02T03:04:06.006Z" to { callDateAdd("second",      1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },

        // add enough to flip a year. Skipping milliseconds as it overflows Long
        "2018T" to { callDateAdd("month",  12, Timestamp.valueOf("2017T")) },
        "2018T" to { callDateAdd("day",    12 * 35, Timestamp.valueOf("2017T")) },
        "2018T" to { callDateAdd("hour",   12 * 35 * 24, Timestamp.valueOf("2017T")) },
        "2018T" to { callDateAdd("minute", 12 * 35 * 24 * 60, Timestamp.valueOf("2017T")) },
        "2018T" to { callDateAdd("second", 12 * 35 * 24 * 60 * 60, Timestamp.valueOf("2017T")) },

        // add enough to flip a month. Skipping milliseconds as it overflows Long
        "2017-02T" to { callDateAdd("day",    35, Timestamp.valueOf("2017-01T")) },
        "2017-02T" to { callDateAdd("hour",   35 * 24 , Timestamp.valueOf("2017-01T")) },
        "2017-02T" to { callDateAdd("minute", 35 * 24 * 60 , Timestamp.valueOf("2017-01T")) },
        "2017-02T" to { callDateAdd("second", 35 * 24 * 60 * 60, Timestamp.valueOf("2017-01T")) },

        // add enough to flip a day
        "2017-02-04T" to { callDateAdd("hour",        24 , Timestamp.valueOf("2017-02-03T")) },
        "2017-02-04T" to { callDateAdd("minute",      24 * 60 , Timestamp.valueOf("2017-02-03T")) },
        "2017-02-04T" to { callDateAdd("second",      24 * 60 * 60, Timestamp.valueOf("2017-02-03T")) },

        // add enough to flip the hour
        "2017-02-04T06:06Z" to { callDateAdd("minute",      60 , Timestamp.valueOf("2017-02-04T05:06Z")) },
        "2017-02-04T06:06Z" to { callDateAdd("second",      60 * 60, Timestamp.valueOf("2017-02-04T05:06Z")) },

        // add enough to flip the minute
        "2017-02-04T05:07Z" to { callDateAdd("second",      60, Timestamp.valueOf("2017-02-04T05:06Z")) },

        // subtract 1 at different precision levels
        "2016T" to { callDateAdd("year",        -1, Timestamp.valueOf("2017T")) },
        "2016T" to { callDateAdd("month",       -1, Timestamp.valueOf("2017T")) },
        "2016T" to { callDateAdd("day",         -1, Timestamp.valueOf("2017T")) },
        "2016T" to { callDateAdd("hour",        -1, Timestamp.valueOf("2017T")) },
        "2016T" to { callDateAdd("minute",      -1, Timestamp.valueOf("2017T")) },
        "2016T" to { callDateAdd("second",      -1, Timestamp.valueOf("2017T")) },

        "2016-02T" to { callDateAdd("year",        -1, Timestamp.valueOf("2017-02T")) },
        "2017-01T" to { callDateAdd("month",       -1, Timestamp.valueOf("2017-02T")) },
        "2017-01T" to { callDateAdd("day",         -1, Timestamp.valueOf("2017-02T")) },
        "2017-01T" to { callDateAdd("hour",        -1, Timestamp.valueOf("2017-02T")) },
        "2017-01T" to { callDateAdd("minute",      -1, Timestamp.valueOf("2017-02T")) },
        "2017-01T" to { callDateAdd("second",      -1, Timestamp.valueOf("2017-02T")) },

        "2016-02-03T" to { callDateAdd("year",        -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-01-03T" to { callDateAdd("month",       -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T" to { callDateAdd("day",         -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T" to { callDateAdd("hour",        -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T" to { callDateAdd("minute",      -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T" to { callDateAdd("second",      -1, Timestamp.valueOf("2017-02-03T")) },

        "2016-02-03T04:05Z" to { callDateAdd("year",        -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-01-03T04:05Z" to { callDateAdd("month",       -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-02T04:05Z" to { callDateAdd("day",         -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-03T03:05Z" to { callDateAdd("hour",        -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-03T04:04Z" to { callDateAdd("minute",      -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-03T04:04Z" to { callDateAdd("second",      -1, Timestamp.valueOf("2017-02-03T04:05Z")) },

        "2016-02-03T04:05:06Z" to { callDateAdd("year",        -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-01-03T04:05:06Z" to { callDateAdd("month",       -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-02T04:05:06Z" to { callDateAdd("day",         -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-03T03:05:06Z" to { callDateAdd("hour",        -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-03T04:04:06Z" to { callDateAdd("minute",      -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-03T04:05:05Z" to { callDateAdd("second",      -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },

        "2016-02-03T04:05:06.007Z" to { callDateAdd("year",        -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-01-03T04:05:06.007Z" to { callDateAdd("month",       -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-02T04:05:06.007Z" to { callDateAdd("day",         -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-03T03:05:06.007Z" to { callDateAdd("hour",        -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-03T04:04:06.007Z" to { callDateAdd("minute",      -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-03T04:05:05.007Z" to { callDateAdd("second",      -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) }
     )
}
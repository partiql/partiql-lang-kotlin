package org.partiql.lang.eval.builtins

import com.amazon.ion.Timestamp
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.call
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.syntax.DateTimePart

class DateAddExprFunctionTest : TestBase() {


    private val env = Environment.standard()

    private val subject = DateAddExprFunction(valueFactory)

    private fun callDateAdd(vararg args: Any) = subject.call(env, RequiredArgs(args.map { anyToExprValue(it) })).timestampValue()

    @Test
    fun wrongTypeOfFirstArgument() {
        assertThatThrownBy { callDateAdd(1, 1, Timestamp.valueOf("2017T")) }
            .hasMessage("Expected text: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun nonExistingDateTimePart() {
        assertThatThrownBy { callDateAdd("foobar", 1, Timestamp.valueOf("2017T")) }
            .hasMessage("invalid datetime part, valid values: [year, month, day, hour, minute, second, timezone_hour, timezone_minute]")
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

    fun parametersForInvalidDateTimePart() = listOf(DateTimePart.TIMEZONE_HOUR,
                                                DateTimePart.TIMEZONE_MINUTE).map { it.toString().toLowerCase() }

    @Test
    @Parameters
    fun invalidDateTimePart(dateTimePart: String) {
        assertThatThrownBy { callDateAdd(dateTimePart, 1, Timestamp.valueOf("2017T")) }
            .hasMessage("invalid datetime part for date_add: $dateTimePart")
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
        "2018T"                     to { callDateAdd("year",   1, Timestamp.valueOf("2017T")) },
        "2017-02T"                  to { callDateAdd("month",  1, Timestamp.valueOf("2017T")) },
        "2017-01-02T"               to { callDateAdd("day",    1, Timestamp.valueOf("2017T")) },
        "2017-01-01T01:00-00:00"    to { callDateAdd("hour",   1, Timestamp.valueOf("2017T")) },
        "2017-01-01T00:01-00:00"    to { callDateAdd("minute", 1, Timestamp.valueOf("2017T")) },
        "2017-01-01T00:00:01-00:00" to { callDateAdd("second", 1, Timestamp.valueOf("2017T")) },

        "2018-01T"                  to { callDateAdd("year",   1, Timestamp.valueOf("2017-01T")) },
        "2017-02T"                  to { callDateAdd("month",  1, Timestamp.valueOf("2017-01T")) },
        "2017-01-02T"               to { callDateAdd("day",    1, Timestamp.valueOf("2017-01T")) },
        "2017-01-01T01:00-00:00"    to { callDateAdd("hour",   1, Timestamp.valueOf("2017-01T")) },
        "2017-01-01T00:01-00:00"    to { callDateAdd("minute", 1, Timestamp.valueOf("2017-01T")) },
        "2017-01-01T00:00:01-00:00" to { callDateAdd("second", 1, Timestamp.valueOf("2017-01T")) },

        "2018-01-02T"               to { callDateAdd("year",   1, Timestamp.valueOf("2017-01-02T")) },
        "2017-02-02T"               to { callDateAdd("month",  1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-03T"               to { callDateAdd("day",    1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-02T01:00-00:00"    to { callDateAdd("hour",   1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-02T00:01-00:00"    to { callDateAdd("minute", 1, Timestamp.valueOf("2017-01-02T")) },
        "2017-01-02T00:00:01-00:00" to { callDateAdd("second", 1, Timestamp.valueOf("2017-01-02T")) },

        "2018-01-02T03:04Z"    to { callDateAdd("year",   1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-02-02T03:04Z"    to { callDateAdd("month",  1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-03T03:04Z"    to { callDateAdd("day",    1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-02T04:04Z"    to { callDateAdd("hour",   1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-02T03:05Z"    to { callDateAdd("minute", 1, Timestamp.valueOf("2017-01-02T03:04Z")) },
        "2017-01-02T03:04:01Z" to { callDateAdd("second", 1, Timestamp.valueOf("2017-01-02T03:04Z")) },

        "2018-01-02T03:04:05Z" to { callDateAdd("year",   1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-02-02T03:04:05Z" to { callDateAdd("month",  1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-03T03:04:05Z" to { callDateAdd("day",    1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-02T04:04:05Z" to { callDateAdd("hour",   1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-02T03:05:05Z" to { callDateAdd("minute", 1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        "2017-01-02T03:04:06Z" to { callDateAdd("second", 1, Timestamp.valueOf("2017-01-02T03:04:05Z")) },

        "2018-01-02T03:04:05.006Z" to { callDateAdd("year",   1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-02-02T03:04:05.006Z" to { callDateAdd("month",  1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-03T03:04:05.006Z" to { callDateAdd("day",    1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-02T04:04:05.006Z" to { callDateAdd("hour",   1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-02T03:05:05.006Z" to { callDateAdd("minute", 1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },
        "2017-01-02T03:04:06.006Z" to { callDateAdd("second", 1, Timestamp.valueOf("2017-01-02T03:04:05.006Z")) },

        // add enough to flip a year. Skipping milliseconds as it overflows Long
        "2018-01T"                  to { callDateAdd("month",  12, Timestamp.valueOf("2017T")) },
        "2018-01-01T"               to { callDateAdd("day",    365, Timestamp.valueOf("2017T")) },
        "2018-01-01T00:00-00:00"    to { callDateAdd("hour",   365 * 24, Timestamp.valueOf("2017T")) },
        "2018-01-01T00:00-00:00"    to { callDateAdd("minute", 365 * 24 * 60, Timestamp.valueOf("2017T")) },
        "2018-01-01T00:00:00-00:00" to { callDateAdd("second", 365 * 24 * 60 * 60, Timestamp.valueOf("2017T")) },

        // add enough to flip a month. Skipping milliseconds as it overflows Long
        "2017-02-01T"               to { callDateAdd("day",    31, Timestamp.valueOf("2017-01T")) },
        "2017-02-01T00:00-00:00"    to { callDateAdd("hour",   31 * 24 , Timestamp.valueOf("2017-01T")) },
        "2017-02-01T00:00-00:00"    to { callDateAdd("minute", 31 * 24 * 60 , Timestamp.valueOf("2017-01T")) },
        "2017-02-01T00:00:00-00:00" to { callDateAdd("second", 31 * 24 * 60 * 60, Timestamp.valueOf("2017-01T")) },

        // add enough to flip a day
        "2017-02-04T00:00-00:00"    to { callDateAdd("hour",   24 , Timestamp.valueOf("2017-02-03T")) },
        "2017-02-04T00:00-00:00"    to { callDateAdd("minute", 24 * 60 , Timestamp.valueOf("2017-02-03T")) },
        "2017-02-04T00:00:00-00:00" to { callDateAdd("second", 24 * 60 * 60, Timestamp.valueOf("2017-02-03T")) },

        // add enough to flip the hour
        "2017-02-04T06:06Z"    to { callDateAdd("minute", 60 , Timestamp.valueOf("2017-02-04T05:06Z")) },
        "2017-02-04T06:06:00Z" to { callDateAdd("second", 60 * 60, Timestamp.valueOf("2017-02-04T05:06Z")) },

        // add enough to flip the minute
        "2017-02-04T05:07:00Z" to { callDateAdd("second", 60, Timestamp.valueOf("2017-02-04T05:06Z")) },

        // subtract 1 at different precision levels
        "2016T"                     to { callDateAdd("year",   -1, Timestamp.valueOf("2017T")) },
        "2016-12T"                  to { callDateAdd("month",  -1, Timestamp.valueOf("2017T")) },
        "2016-12-31T"               to { callDateAdd("day",    -1, Timestamp.valueOf("2017T")) },
        "2016-12-31T23:00-00:00"    to { callDateAdd("hour",   -1, Timestamp.valueOf("2017T")) },
        "2016-12-31T23:59-00:00"    to { callDateAdd("minute", -1, Timestamp.valueOf("2017T")) },
        "2016-12-31T23:59:59-00:00" to { callDateAdd("second", -1, Timestamp.valueOf("2017T")) },

        "2016-02T"                  to { callDateAdd("year",   -1, Timestamp.valueOf("2017-02T")) },
        "2017-01T"                  to { callDateAdd("month",  -1, Timestamp.valueOf("2017-02T")) },
        "2017-01-31T"               to { callDateAdd("day",    -1, Timestamp.valueOf("2017-02T")) },
        "2017-01-31T23:00-00:00"    to { callDateAdd("hour",   -1, Timestamp.valueOf("2017-02T")) },
        "2017-01-31T23:59-00:00"    to { callDateAdd("minute", -1, Timestamp.valueOf("2017-02T")) },
        "2017-01-31T23:59:59-00:00" to { callDateAdd("second", -1, Timestamp.valueOf("2017-02T")) },

        "2016-02-03T"               to { callDateAdd("year",   -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-01-03T"               to { callDateAdd("month",  -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T"               to { callDateAdd("day",    -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T23:00-00:00"    to { callDateAdd("hour",   -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T23:59-00:00"    to { callDateAdd("minute", -1, Timestamp.valueOf("2017-02-03T")) },
        "2017-02-02T23:59:59-00:00" to { callDateAdd("second", -1, Timestamp.valueOf("2017-02-03T")) },

        "2016-02-03T04:05Z"    to { callDateAdd("year",   -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-01-03T04:05Z"    to { callDateAdd("month",  -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-02T04:05Z"    to { callDateAdd("day",    -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-03T03:05Z"    to { callDateAdd("hour",   -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-03T04:04Z"    to { callDateAdd("minute", -1, Timestamp.valueOf("2017-02-03T04:05Z")) },
        "2017-02-03T04:04:59Z" to { callDateAdd("second", -1, Timestamp.valueOf("2017-02-03T04:05Z")) },

        "2016-02-03T04:05:06Z" to { callDateAdd("year",   -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-01-03T04:05:06Z" to { callDateAdd("month",  -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-02T04:05:06Z" to { callDateAdd("day",    -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-03T03:05:06Z" to { callDateAdd("hour",   -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-03T04:04:06Z" to { callDateAdd("minute", -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },
        "2017-02-03T04:05:05Z" to { callDateAdd("second", -1, Timestamp.valueOf("2017-02-03T04:05:06Z")) },

        "2016-02-03T04:05:06.007Z" to { callDateAdd("year",   -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-01-03T04:05:06.007Z" to { callDateAdd("month",  -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-02T04:05:06.007Z" to { callDateAdd("day",    -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-03T03:05:06.007Z" to { callDateAdd("hour",   -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-03T04:04:06.007Z" to { callDateAdd("minute", -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) },
        "2017-02-03T04:05:05.007Z" to { callDateAdd("second", -1, Timestamp.valueOf("2017-02-03T04:05:06.007Z")) }
     )
}
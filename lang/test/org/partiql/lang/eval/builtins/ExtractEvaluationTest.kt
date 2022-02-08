package org.partiql.lang.eval.builtins

import com.amazon.ion.Timestamp
import junitparams.Parameters
import org.assertj.core.api.Assertions
import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.call
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.time.Time
import org.partiql.lang.syntax.DateTimePart
import org.partiql.lang.util.to
import java.time.LocalDate

/**
 * Parsing related tests in [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class ExtractEvaluationTest : EvaluatorTestBase() {

    private val env = Environment.standard()

    private val subject = ExtractExprFunction(valueFactory)

    @Test
    fun extractYear() = assertEval("extract(year from `2017-01-10T05:30:55Z`)", "2017.")

    @Test
    fun extractMonth() = assertEval("extract(month from `2017-01-10T05:30:55Z`)", "1.")

    @Test
    fun extractDay() = assertEval("extract(day from `2017-01-10T05:30:55Z`)", "10.")

    @Test
    fun extractHour() = assertEval("extract(hour from `2017-01-10T05:30:55Z`)", "5.")

    @Test
    fun extractMinute() = assertEval("extract(minute from `2017-01-10T05:30:55Z`)", "30.")

    @Test
    fun extractSecond() = assertEval("extract(second from `2017-01-10T05:30:55Z`)", "55.")

    @Test
    fun extractTimezoneHour() = assertEval("extract(timezone_hour from `2017-01-10T05:30:55+11:30`)", "11.")

    @Test
    fun extractTimezoneMinute() = assertEval("extract(timezone_minute from `2017-01-10T05:30:55+11:30`)", "30.")

    @Test
    fun extractFromNull() = assertEval("extract(timezone_minute from null)", "null")

    @Test
    fun extractFromMissing() = assertEval("extract(timezone_minute from missing)", "null")

    @Test
    fun extractTimezoneHourNegativeOffset() =
        assertEval("extract(timezone_hour from `2017-01-10T05:30:55-11:30`)", "-11.")

    @Test
    fun extractTimezoneMinuteNegativeOffset() =
        assertEval("extract(timezone_minute from `2017-01-10T05:30:55-11:30`)", "-30.")

    @Test
    fun extractWithBindings() = assertEval("extract(second from a)",
                                           "55.",
                                           mapOf("a" to "2017-01-10T05:30:55Z").toSession())

    @Test
    fun wrongArgumentTypes() =
    checkInputThrowingEvaluationException(
        input = "extract(year from 1)",
        errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
        expectErrorContextValues = mapOf<Property, Any>(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 1L,
            Property.FUNCTION_NAME to "extract",
            Property.EXPECTED_ARGUMENT_TYPES to "TIMESTAMP, TIME, or DATE",
            Property.ARGUMENT_POSITION to 2,
            Property.ACTUAL_ARGUMENT_TYPES to "INT"),
        expectedPermissiveModeResult = "MISSING"
    )

    data class ExtractFromDateTC(val source: String, val expected: ExprValue?)

    private fun createDateTC(source: String, expected: LocalDate) =
        DateTimePart.values()
            .map { dateTimePart ->
                ExtractFromDateTC(
                    source = "EXTRACT(${dateTimePart.name} FROM $source)",
                    expected = when (dateTimePart) {
                        DateTimePart.YEAR -> valueFactory.newDecimal(expected.year)
                        DateTimePart.MONTH -> valueFactory.newDecimal(expected.monthValue)
                        DateTimePart.DAY -> valueFactory.newDecimal(expected.dayOfMonth)
                        DateTimePart.HOUR -> valueFactory.newDecimal(0)
                        DateTimePart.MINUTE -> valueFactory.newDecimal(0)
                        DateTimePart.SECOND -> valueFactory.newDecimal(0)
                        DateTimePart.TIMEZONE_HOUR -> null
                        DateTimePart.TIMEZONE_MINUTE -> null
                    }
                )
            }

    fun parametersForRunTests() = listOf(
        createDateTC("DATE '2012-12-12'", LocalDate.of(2012, 12, 12)),
        createDateTC("DATE '2020-02-29'", LocalDate.of(2020, 2, 29)),
        createDateTC("DATE '2021-03-24'", LocalDate.of(2021, 3, 24))
    ).flatten()

    @Test
    @Parameters
    fun runTests(tc: ExtractFromDateTC) = when (tc.expected) {
        null -> {
            try {
                voidEval(tc.source)
                fail("Expected evaluation error")
            } catch (e: EvaluationException) {
                // Do nothing
            }
        }
        else -> assertExprEquals(eval(tc.source), tc.expected, "Expected exprValues to be equal.")
    }

    data class ExtractFromTimeTC(val source: String, val expected: ExprValue?)

    private fun createTimeTC(source: String, expected: Time) =
        DateTimePart.values()
            .map { dateTimePart ->
                ExtractFromTimeTC(
                    source = "EXTRACT(${dateTimePart.name} FROM $source)",
                    expected = when (dateTimePart) {
                        DateTimePart.YEAR -> null
                        DateTimePart.MONTH -> null
                        DateTimePart.DAY -> null
                        DateTimePart.HOUR -> valueFactory.newDecimal(expected.localTime.hour)
                        DateTimePart.MINUTE -> valueFactory.newDecimal(expected.localTime.minute)
                        DateTimePart.SECOND -> valueFactory.newDecimal(expected.secondsWithFractionalPart)
                        DateTimePart.TIMEZONE_HOUR -> expected.timezoneHour?.let { valueFactory.newDecimal(it) }
                        DateTimePart.TIMEZONE_MINUTE -> expected.timezoneMinute?.let { valueFactory.newDecimal(it) }
                    }
                )
            }

    fun parametersForRunTimeTests() = listOf(
        createTimeTC("TIME '23:12:59.128'", Time.of(23, 12, 59, 128000000, 3, null)),
        createTimeTC("TIME WITH TIME ZONE '23:12:59.128-06:30'", Time.of(23, 12, 59, 128000000, 3, -390)),
        createTimeTC("TIME WITH TIME ZONE '23:12:59.12800-00:00'", Time.of(23, 12, 59, 128000000, 5, 0)),
        createTimeTC("TIME (2) '23:12:59.128'", Time.of(23, 12, 59, 130000000, 2, null)),
        createTimeTC("TIME (2) WITH TIME ZONE '23:12:59.128-06:30'", Time.of(23, 12, 59, 130000000, 2, -390)),
        createTimeTC("TIME (3) WITH TIME ZONE '23:12:59.128-06:30'", Time.of(23, 12, 59, 128000000, 3, -390)),
        createTimeTC("TIME (3) WITH TIME ZONE '23:59:59.9998-18:00'", Time.of(0, 0, 0, 0, 3, -1080))
    ).flatten()

    @Test
    @Parameters
    fun runTimeTests(tc: ExtractFromTimeTC) = when (tc.expected) {
        null -> {
            try {
                voidEval(tc.source)
                fail("Expected evaluation error")
            } catch (e: EvaluationException) {
                // Do nothing
            }
        }
        else -> assertExprEquals(eval(tc.source), tc.expected, "Expected exprValues to be equal.")
    }

    private fun callExtract(vararg args: Any): Number? {
        val value = subject.call(env, RequiredArgs(args.map { anyToExprValue(it) }))
        return when(value.type) {
            ExprValueType.NULL -> null
            else -> value.numberValue()
        }
    }

    @Test
    fun wrongTypeOfFirstArgument() {
        Assertions.assertThatThrownBy { callExtract("foobar", 1) }
            .hasMessage("invalid datetime part, valid values: [year, month, day, hour, minute, second, timezone_hour, timezone_minute]")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfSecondArgument() {
        Assertions.assertThatThrownBy { callExtract("year", "999") }
            .hasMessage("Expected date, time or timestamp: '999'")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    fun parametersForExtract(): List<Pair<Number?, () -> Number?>> = listOf(
        // just year
        2017 to { callExtract("year", Timestamp.valueOf("2017T")) },
        1 to { callExtract("month", Timestamp.valueOf("2017T")) },
        1 to { callExtract("day", Timestamp.valueOf("2017T")) },
        0 to { callExtract("hour", Timestamp.valueOf("2017T")) },
        0 to { callExtract("minute", Timestamp.valueOf("2017T")) },
        0 to { callExtract("second", Timestamp.valueOf("2017T")) },
        0 to { callExtract("timezone_hour", Timestamp.valueOf("2017T")) },
        0 to { callExtract("timezone_minute", Timestamp.valueOf("2017T")) },
        // year, month
        2017 to { callExtract("year", Timestamp.valueOf("2017-01T")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01T")) },
        1 to { callExtract("day", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("hour", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("minute", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("second", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("timezone_hour", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01T")) },

        // year, month, day
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("hour", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("minute", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("second", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T")) },

        // year, month, day, hour, minute
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04Z")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04Z")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04Z")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04Z")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04Z")) },
        0    to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04Z")) },
        0    to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04Z")) },
        0    to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04Z")) },

        // year, month, day, hour, minute, second
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        5    to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0    to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0    to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05Z")) },

        // year, month, day, hour, minute, second, local offset
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        5    to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        7    to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        8    to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },

        // negative offset
        -7 to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04:05-07:08")) },
        -8 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05-07:08")) },

        // extract year, month, day, hour, minute, second from DATE literals
        2021 to { callExtract("year", LocalDate.of(2021, 3, 24)) },
        3 to { callExtract("month", LocalDate.of(2021, 3, 24)) },
        24 to { callExtract("day", LocalDate.of(2021, 3, 24)) },
        0 to { callExtract("hour", LocalDate.of(2021, 3, 24)) },
        0 to { callExtract("minute", LocalDate.of(2021, 3, 24)) },
        0 to { callExtract("second", LocalDate.of(2021, 3, 24)) },

        // extract hour, minute, second, timezone_hour, timezone_minute from TIME literals
        23 to { callExtract("hour", Time.of(23, 12, 59, 128000000, 2, -510))},
        12 to { callExtract("minute", Time.of(23, 12, 59, 128000000, 2, -510))},
        59.13 to { callExtract("second", Time.of(23, 12, 59, 128000000, 2, -510))},
        -8 to { callExtract("timezone_hour", Time.of(23, 12, 59, 128000000, 2, -510))},
        -30 to { callExtract("timezone_minute", Time.of(23, 12, 59, 128000000, 2, -510))}
    )

    @Test
    @Parameters
    fun extract(params: Pair<Number?, () -> Number?>) {
        val (expected, call) = params

        assertEquals(expected?.toLong(), call.invoke()?.toLong())
    }
}
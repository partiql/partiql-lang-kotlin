package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import junitparams.*
import org.assertj.core.api.*
import org.junit.*

class ExtractExprFunctionTest : Base() {
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

    private val subject = ExtractExprFunction(ion)

    private fun callExtract(vararg args: Any): Number? {
        val value = subject.call(env, args.map { it.exprValue() }.toList())
        return when(value.type) {
            ExprValueType.NULL -> null
            else -> value.numberValue()
        }
    }

    @Test
    fun lessArguments() {
        Assertions.assertThatThrownBy { callExtract("year") }
            .hasMessage("extract takes exactly 2 arguments, received: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test fun moreArguments() {
        Assertions.assertThatThrownBy { callExtract("year", 1, 1) }
            .hasMessage("extract takes exactly 2 arguments, received: 3")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfFirstArgument() {
        Assertions.assertThatThrownBy { callExtract("foobar", 1) }
            .hasMessage("invalid date part, valid values: [year, month, day, hour, minute, second, timezone_hour, timezone_minute]")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfSecondArgument() {
        Assertions.assertThatThrownBy { callExtract("year", "999") }
            .hasMessage("Expected timestamp: \"999\"")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    fun parametersForExtract(): List<Pair<Number?, () -> Number?>> = listOf(
        // just year
        2017 to { callExtract("year", Timestamp.valueOf("2017T")) },
        null to { callExtract("month", Timestamp.valueOf("2017T")) },
        null to { callExtract("day", Timestamp.valueOf("2017T")) },
        null to { callExtract("hour", Timestamp.valueOf("2017T")) },
        null to { callExtract("minute", Timestamp.valueOf("2017T")) },
        null to { callExtract("second", Timestamp.valueOf("2017T")) },
        null to { callExtract("timezone_hour", Timestamp.valueOf("2017T")) },
        null to { callExtract("timezone_minute", Timestamp.valueOf("2017T")) },

        // year, month
        2017 to { callExtract("year", Timestamp.valueOf("2017-01T")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01T")) },
        null to { callExtract("day", Timestamp.valueOf("2017-01T")) },
        null to { callExtract("hour", Timestamp.valueOf("2017-01T")) },
        null to { callExtract("minute", Timestamp.valueOf("2017-01T")) },
        null to { callExtract("second", Timestamp.valueOf("2017-01T")) },
        null to { callExtract("timezone_hour", Timestamp.valueOf("2017-01T")) },
        null to { callExtract("timezone_minute", Timestamp.valueOf("2017-01T")) },

        // year, month, day
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T")) },
        null to { callExtract("hour", Timestamp.valueOf("2017-01-02T")) },
        null to { callExtract("minute", Timestamp.valueOf("2017-01-02T")) },
        null to { callExtract("second", Timestamp.valueOf("2017-01-02T")) },
        null to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T")) },
        null to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T")) },

        // year, month, day, hour, minute
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04Z")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04Z")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04Z")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04Z")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04Z")) },
        null to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04Z")) },
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
        -8 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05-07:08")) }
    )

    @Test
    @Parameters
    fun extract(params: Pair<Number?, () -> Number?>) {
        val (expected, call) = params

        assertEquals(expected?.toLong(), call.invoke()?.toLong())
    }
}
/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins

import com.amazon.ion.*
import org.partiql.lang.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*
import junitparams.*
import org.assertj.core.api.*
import org.junit.*
import java.time.LocalDate

class ExtractExprFunctionTest : TestBase() {

    private val env = Environment.standard()

    private val subject = ExtractExprFunction(valueFactory)

    private fun callExtract(vararg args: Any): Number? {
        val value = subject.call(env, args.map { anyToExprValue(it) }.toList())
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
            .hasMessage("Expected date or timestamp: '999'")
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
        0 to { callExtract("second", LocalDate.of(2021, 3, 24)) }
    )

    @Test
    @Parameters
    fun extract(params: Pair<Number?, () -> Number?>) {
        val (expected, call) = params

        assertEquals(expected?.toLong(), call.invoke()?.toLong())
    }
}
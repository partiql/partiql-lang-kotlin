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
import org.assertj.core.api.Assertions.*
import org.junit.*

class DateDiffExprFunctionTest : TestBase() {
    private val env = Environment.standard()

    private val subject = DateDiffExprFunction(valueFactory)

    private fun callDateDiff(vararg args: Any) = subject.call(env, args.map { anyToExprValue(it) }.toList()).numberValue()

    @Test
    fun lessArguments() {
        assertThatThrownBy { callDateDiff("year") }
            .hasMessage("date_diff takes exactly 3 arguments, received: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun moreArguments() {
        assertThatThrownBy { callDateDiff("year", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T"), 1) }
            .hasMessage("date_diff takes exactly 3 arguments, received: 4")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfFirstArgument() {
        assertThatThrownBy { callDateDiff("foobar", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) }
            .hasMessage("invalid datetime part, valid values: [year, month, day, hour, minute, second, timezone_hour, timezone_minute]")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfSecondArgument() {
        assertThatThrownBy { callDateDiff("year", 1, Timestamp.valueOf("2017T")) }
            .hasMessage("Expected timestamp: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfThirdArgument() {
        assertThatThrownBy { callDateDiff("year", Timestamp.valueOf("2017T"), 1) }
            .hasMessage("Expected timestamp: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    fun parametersForDateDiff(): List<Pair<Number, () -> Number>> = listOf(
        // same dates
        0 to { callDateDiff("year", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017T")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01T")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01T")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01T")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01T")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01T")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01T")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-02T")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-02T")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-02T")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-02T")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-02T")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-02T")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:05.007+08:09"), Timestamp.valueOf("2017-01-02T03:04:05.007+08:09")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:05.007+08:09"), Timestamp.valueOf("2017-01-02T03:04:05.007+08:09")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:05.007+08:09"), Timestamp.valueOf("2017-01-02T03:04:05.007+08:09")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:05.007+08:09"), Timestamp.valueOf("2017-01-02T03:04:05.007+08:09")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:05.007+08:09"), Timestamp.valueOf("2017-01-02T03:04:05.007+08:09")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:05.007+08:09"), Timestamp.valueOf("2017-01-02T03:04:05.007+08:09")) },

        // later - earlier
        1                  to { callDateDiff("year", Timestamp.valueOf("2017T"), Timestamp.valueOf("2018T")) },
        12                 to { callDateDiff("month", Timestamp.valueOf("2017T"), Timestamp.valueOf("2018T")) },
        365                to { callDateDiff("day", Timestamp.valueOf("2017T"), Timestamp.valueOf("2018T")) },
        365 * 24           to { callDateDiff("hour", Timestamp.valueOf("2017T"), Timestamp.valueOf("2018T")) },
        365 * 24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2017T"), Timestamp.valueOf("2018T")) },
        365 * 24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017T"), Timestamp.valueOf("2018T")) },

        0                 to { callDateDiff("year", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-02T")) },
        1                 to { callDateDiff("month", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-02T")) },
        31                to { callDateDiff("day", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-02T")) },
        31 * 24           to { callDateDiff("hour", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-02T")) },
        31 * 24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-02T")) },
        31 * 24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-02T")) },

        0            to { callDateDiff("year", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-03T")) },
        0            to { callDateDiff("month", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-03T")) },
        1            to { callDateDiff("day", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-03T")) },
        24           to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-03T")) },
        24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-03T")) },
        24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T"), Timestamp.valueOf("2017-01-03T")) },

        0       to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T04:04Z")) },
        0       to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T04:04Z")) },
        0       to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T04:04Z")) },
        1       to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T04:04Z")) },
        60      to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T04:04Z")) },
        60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T04:04Z")) },

        0  to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:05Z")) },
        0  to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:05Z")) },
        0  to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:05Z")) },
        0  to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:05Z")) },
        1  to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:05Z")) },
        60 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04Z"), Timestamp.valueOf("2017-01-02T03:05Z")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:06Z")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:06Z")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:06Z")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:06Z")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:06Z")) },
        1 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:05Z"), Timestamp.valueOf("2017-01-02T03:04:06Z")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.008Z")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.008Z")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.008Z")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.008Z")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.008Z")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:05.007Z"), Timestamp.valueOf("2017-01-02T03:04:05.008Z")) },

        // earlier - later
        -1                  to { callDateDiff("year", Timestamp.valueOf("2018T"), Timestamp.valueOf("2017T")) },
        -12                 to { callDateDiff("month", Timestamp.valueOf("2018T"), Timestamp.valueOf("2017T")) },
        -365                to { callDateDiff("day", Timestamp.valueOf("2018T"), Timestamp.valueOf("2017T")) },
        -365 * 24           to { callDateDiff("hour", Timestamp.valueOf("2018T"), Timestamp.valueOf("2017T")) },
        -365 * 24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2018T"), Timestamp.valueOf("2017T")) },
        -365 * 24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2018T"), Timestamp.valueOf("2017T")) },

        0                  to { callDateDiff("year", Timestamp.valueOf("2017-02T"), Timestamp.valueOf("2017-01T")) },
        -1                 to { callDateDiff("month", Timestamp.valueOf("2017-02T"), Timestamp.valueOf("2017-01T")) },
        -31                to { callDateDiff("day", Timestamp.valueOf("2017-02T"), Timestamp.valueOf("2017-01T")) },
        -31 * 24           to { callDateDiff("hour", Timestamp.valueOf("2017-02T"), Timestamp.valueOf("2017-01T")) },
        -31 * 24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2017-02T"), Timestamp.valueOf("2017-01T")) },
        -31 * 24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017-02T"), Timestamp.valueOf("2017-01T")) },

        0             to { callDateDiff("year", Timestamp.valueOf("2017-01-03T"), Timestamp.valueOf("2017-01-02T")) },
        0             to { callDateDiff("month", Timestamp.valueOf("2017-01-03T"), Timestamp.valueOf("2017-01-02T")) },
        -1            to { callDateDiff("day", Timestamp.valueOf("2017-01-03T"), Timestamp.valueOf("2017-01-02T")) },
        -24           to { callDateDiff("hour", Timestamp.valueOf("2017-01-03T"), Timestamp.valueOf("2017-01-02T")) },
        -24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2017-01-03T"), Timestamp.valueOf("2017-01-02T")) },
        -24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017-01-03T"), Timestamp.valueOf("2017-01-02T")) },

        0        to { callDateDiff("year", Timestamp.valueOf("2017-01-02T04:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0        to { callDateDiff("month", Timestamp.valueOf("2017-01-02T04:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0        to { callDateDiff("day", Timestamp.valueOf("2017-01-02T04:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        -1       to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T04:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        -60      to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T04:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        -60 * 60 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T04:04Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },

        0   to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:05Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0   to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:05Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0   to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:05Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        0   to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:05Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        -1  to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:05Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },
        -60 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:05Z"), Timestamp.valueOf("2017-01-02T03:04Z")) },

        0  to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:06Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0  to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:06Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0  to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:06Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0  to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:06Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0  to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:06Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        -1 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:06Z"), Timestamp.valueOf("2017-01-02T03:04:05Z")) },

        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04:05.008Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04:05.008Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T03:04:05.008Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04:05.008Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04:05.008Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },
        0 to { callDateDiff("second", Timestamp.valueOf("2017-01-02T03:04:05.008Z"), Timestamp.valueOf("2017-01-02T03:04:05.007Z")) },

        // on different local offsets
        0 to { callDateDiff("year", Timestamp.valueOf("2017-01-02T03:04+01:02"), Timestamp.valueOf("2017-01-02T03:04+00:00")) },
        0 to { callDateDiff("month", Timestamp.valueOf("2017-01-02T03:04+00:02"), Timestamp.valueOf("2017-01-02T03:04+00:00")) },
        0 to { callDateDiff("day", Timestamp.valueOf("2017-01-02T01:00+10:00"), Timestamp.valueOf("2017-01-02T01:00+00:00")) },
        1 to { callDateDiff("hour", Timestamp.valueOf("2017-01-02T03:04+01:02"), Timestamp.valueOf("2017-01-02T03:04+00:00")) },
        2 to { callDateDiff("minute", Timestamp.valueOf("2017-01-02T03:04+00:02"), Timestamp.valueOf("2017-01-02T03:04+00:00")) },

        // different precisions
        // year
        1 to { callDateDiff("month", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017-02T")) },
        1 to { callDateDiff("day", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017-01-02T")) },
        1 to { callDateDiff("hour", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017-01-01T01:00Z")) },
        1 to { callDateDiff("minute", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017-01-01T00:01Z")) },
        1 to { callDateDiff("second", Timestamp.valueOf("2017T"), Timestamp.valueOf("2017-01-01T00:00:01Z")) },

        // month
        1 to { callDateDiff("day", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01-02T")) },
        1 to { callDateDiff("hour", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01-01T01:00Z")) },
        1 to { callDateDiff("minute", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01-01T00:01Z")) },
        1 to { callDateDiff("second", Timestamp.valueOf("2017-01T"), Timestamp.valueOf("2017-01-01T00:00:01Z")) },

        // day
        1 to { callDateDiff("hour", Timestamp.valueOf("2017-01-01T"), Timestamp.valueOf("2017-01-01T01:00Z")) },
        1 to { callDateDiff("minute", Timestamp.valueOf("2017-01-01T"), Timestamp.valueOf("2017-01-01T00:01Z")) },
        1 to { callDateDiff("second", Timestamp.valueOf("2017-01-01T"), Timestamp.valueOf("2017-01-01T00:00:01Z")) },

        // minute
        1 to { callDateDiff("second", Timestamp.valueOf("2017-01-01T00:00Z"), Timestamp.valueOf("2017-01-01T00:00:01Z")) },

        // leap year
        366                to { callDateDiff("day", Timestamp.valueOf("2016-01-01T"), Timestamp.valueOf("2017-01-01T")) },
        366 * 24           to { callDateDiff("hour", Timestamp.valueOf("2016-01-01T"), Timestamp.valueOf("2017-01-01T")) },
        366 * 24 * 60      to { callDateDiff("minute", Timestamp.valueOf("2016-01-01T"), Timestamp.valueOf("2017-01-01T")) },
        366 * 24 * 60 * 60 to { callDateDiff("second", Timestamp.valueOf("2016-01-01T"), Timestamp.valueOf("2017-01-01T")) },

        // Days in a month
        31 to { callDateDiff("day", Timestamp.valueOf("2017-01-01T"), Timestamp.valueOf("2017-02-01T")) }, // January
        28 to { callDateDiff("day", Timestamp.valueOf("2017-02-01T"), Timestamp.valueOf("2017-03-01T")) }, // February
        29 to { callDateDiff("day", Timestamp.valueOf("2016-02-01T"), Timestamp.valueOf("2016-03-01T")) }, // February leap year
        31 to { callDateDiff("day", Timestamp.valueOf("2017-03-01T"), Timestamp.valueOf("2017-04-01T")) }, // March
        30 to { callDateDiff("day", Timestamp.valueOf("2017-04-01T"), Timestamp.valueOf("2017-05-01T")) }, // April
        31 to { callDateDiff("day", Timestamp.valueOf("2017-05-01T"), Timestamp.valueOf("2017-06-01T")) }, // May
        30 to { callDateDiff("day", Timestamp.valueOf("2017-06-01T"), Timestamp.valueOf("2017-07-01T")) }, // June
        31 to { callDateDiff("day", Timestamp.valueOf("2017-07-01T"), Timestamp.valueOf("2017-08-01T")) }, // July
        31 to { callDateDiff("day", Timestamp.valueOf("2017-08-01T"), Timestamp.valueOf("2017-09-01T")) }, // August
        30 to { callDateDiff("day", Timestamp.valueOf("2017-09-01T"), Timestamp.valueOf("2017-10-01T")) }, // September
        31 to { callDateDiff("day", Timestamp.valueOf("2017-10-01T"), Timestamp.valueOf("2017-11-01T")) }, // October
        30 to { callDateDiff("day", Timestamp.valueOf("2017-11-01T"), Timestamp.valueOf("2017-12-01T")) }, // November
        31 to { callDateDiff("day", Timestamp.valueOf("2017-12-01T"), Timestamp.valueOf("2018-01-01T")) }  // December
    )

    @Test
    @Parameters
    fun dateDiff(params: Pair<Number, () -> Number>) {
        val (expected, call) = params

        assertEquals(expected.toLong(), call.invoke().toLong())
    }
}

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
import junitparams.*
import junitparams.naming.*
import org.junit.*
import org.junit.runner.*
import java.time.format.*
import java.util.*
import kotlin.test.*
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.time.*
import java.time.temporal.*

@RunWith(JUnitParamsRunner::class)
class TimestampTemporalAccessorTests {
    private val ITERATION_COUNT = 1000000

    fun createRng(): Random {
        val rng = Random()
        val seed = rng.nextLong()
        System.out.println("Randomly generated seed is ${seed}.  Use this to reproduce failures in dev environment.")
        rng.setSeed(seed)
        return rng
    }

    @Test
    @Parameters
    @TestCaseName("formatRandomTimesWithSymbol_{0}")
    fun formatRandomTimesWithAllDateFormatSymbolsTest(formatSymbol: String) {
        System.out.println(String.format("Generating %,d random dates, formatting each of them with \"%s\" comparing the result...",
                ITERATION_COUNT, formatSymbol))

        val rng = createRng()

        val formatter = DateTimeFormatter.ofPattern(formatSymbol);

        (0..ITERATION_COUNT).toList().parallelStream().forEach { _ ->
            val timestamp = rng.nextTimestamp()
            val offsetDatetime = timestamp.toOffsetDateTime()

            val temporalAccessor = timestamp.toTemporalAccessor()

            val formattedTimestamp = formatter.format(temporalAccessor)
            val formattedOffsetDateTime = formatter.format(offsetDatetime)
            assertEquals(formattedOffsetDateTime, formattedTimestamp)
        }
    }
    fun parametersForFormatRandomTimesWithAllDateFormatSymbolsTest() : Set<Char> = TIMESTAMP_FORMAT_SYMBOLS

    @Test
    fun timestampWithUnknownOffset() {
        //Note:  Ion spec allows representation of unknown offset with "0"
        val timestamp = Timestamp.forSecond(1969, 7, 20, 20, 18, 36, null)
        assertNull(timestamp.localOffset)

        val temporalAccessor = timestamp.toTemporalAccessor()
        val formatter = DateTimeFormatter.ofPattern("Z")

        assertEquals("+0000", formatter.format(temporalAccessor))
    }

    data class UnsupportedSymbolTestCase(val formatSymbol: String, val expectedExceptionType: Class<*>) {
        override fun toString(): String = formatSymbol
    }

    @Test
    @Parameters
    @TestCaseName("handleUnsupportedFormatSymbols_{0}")
    fun handleUnsupportedFormatSymbolsTest(testCase: UnsupportedSymbolTestCase) {
        val timestamp = Timestamp.forSecond(1969, 7, 20, 20, 18, 36, 0)
        val temporalAccessor = timestamp.toTemporalAccessor()
        val formatter = DateTimeFormatter.ofPattern(testCase.formatSymbol)

        assertThatThrownBy { formatter.format(temporalAccessor) }
            .isInstanceOf(testCase.expectedExceptionType)
    }

    /** Most of these format symbols are unsupported either because Ion's Timestamp doesn't store the information required
     * (i.e. timezone) or as a risk mitigation strategy that reduces the complexity should we need to completely replace
     * Java's DateTimeFormatter with something else later.
     */
    fun parametersForHandleUnsupportedFormatSymbolsTest(): List<UnsupportedSymbolTestCase> = listOf(
        UnsupportedSymbolTestCase("G", UnsupportedTemporalTypeException::class.java),    //Era, e.g. "AD"
        UnsupportedSymbolTestCase("u", UnsupportedTemporalTypeException::class.java),    //Year of era, e.g. "1978"; "78", (this is always positive, even for BC values)
        UnsupportedSymbolTestCase("Q", UnsupportedTemporalTypeException::class.java),    //Quarter of year (1-4)
        UnsupportedSymbolTestCase("q", UnsupportedTemporalTypeException::class.java),    //Quarter of year e.g. "Q3" "3rd quarter",
        UnsupportedSymbolTestCase("E", UnsupportedTemporalTypeException::class.java),    //Day of week
        UnsupportedSymbolTestCase("F", UnsupportedTemporalTypeException::class.java),    //Week of month
        UnsupportedSymbolTestCase("K", UnsupportedTemporalTypeException::class.java),    //hour of am-pm (0-11)
        UnsupportedSymbolTestCase("k", UnsupportedTemporalTypeException::class.java),    //clock of am-pm (1-24)
        UnsupportedSymbolTestCase("A", UnsupportedTemporalTypeException::class.java),    //Millsecond of day (0-85,499,999)
        UnsupportedSymbolTestCase("N", UnsupportedTemporalTypeException::class.java),    //Nano of day (0-85,499,999,999,999)
        UnsupportedSymbolTestCase("Y", UnsupportedTemporalTypeException::class.java),    //Week based year
        UnsupportedSymbolTestCase("w", UnsupportedTemporalTypeException::class.java),    //Week of week based year
        UnsupportedSymbolTestCase("W", UnsupportedTemporalTypeException::class.java),    //Week of month
        UnsupportedSymbolTestCase("e", UnsupportedTemporalTypeException::class.java),    //Localized day of week (number)
        UnsupportedSymbolTestCase("c", UnsupportedTemporalTypeException::class.java),    //Localized day of week (week name, e.g. "Tue" or "Tuesday")
        UnsupportedSymbolTestCase("VV", DateTimeException::class.java),   //time zone id, e.g "America/Los_Angeles; Z; -08:30" - ion timestamp does not know timezone, only offset
        UnsupportedSymbolTestCase("z", DateTimeException::class.java)     //time zone name, e.g. "Pacific Standard Time" - ion timestamp does not know timezone, only offset
    )
}


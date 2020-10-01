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

package org.partiql.lang.eval.builtins.timestamp

import org.partiql.lang.util.*
import junitparams.*
import org.junit.*
import org.junit.Test
import org.junit.runner.*
import kotlin.test.*

@RunWith(JUnitParamsRunner::class)
internal class TimestampFormatPatternParserTest {

    fun parametersForParse(): List<Pair<String, List<FormatItem>>> = listOf(
        "y" to listOf(YearPatternSymbol(YearFormat.FOUR_DIGIT)),
        "yy" to listOf(YearPatternSymbol(YearFormat.TWO_DIGIT)),
        "yyyy" to listOf(YearPatternSymbol(YearFormat.FOUR_DIGIT_ZERO_PADDED)),

        "M" to listOf(MonthPatternSymbol(MonthFormat.MONTH_NUMBER)),
        "MM" to listOf(MonthPatternSymbol(MonthFormat.MONTH_NUMBER_ZERO_PADDED)),
        "MMM" to listOf(MonthPatternSymbol(MonthFormat.ABBREVIATED_MONTH_NAME)),
        "MMMM" to listOf(MonthPatternSymbol(MonthFormat.FULL_MONTH_NAME)),
        "MMMMM" to listOf(MonthPatternSymbol(MonthFormat.FIRST_LETTER_OF_MONTH_NAME)),

        "d" to listOf(DayOfMonthPatternSymbol(TimestampFieldFormat.NUMBER)),
        "dd" to listOf(DayOfMonthPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER)),

        "h" to listOf(HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.NUMBER_12_HOUR)),
        "hh" to listOf(HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.ZERO_PADDED_NUMBER_12_HOUR)),
        "H" to listOf(HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.NUMBER_24_HOUR)),
        "HH" to listOf(HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.ZERO_PADDED_NUMBER_24_HOUR)),

        "a" to listOf(AmPmPatternSymbol()),

        "m" to listOf(MinuteOfHourPatternSymbol(TimestampFieldFormat.NUMBER)),
        "mm" to listOf(MinuteOfHourPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER)),

        "s" to listOf(SecondOfMinutePatternPatternSymbol(TimestampFieldFormat.NUMBER)),
        "ss" to listOf(SecondOfMinutePatternPatternSymbol( TimestampFieldFormat.ZERO_PADDED_NUMBER)),

        "x" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR)),

        "xx" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_MINUTE)),
        "xxxx" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_MINUTE)),

        "xxx" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE)),
        "xxxxx" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE)),

        "X" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_OR_Z)),

        "XX" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_MINUTE_OR_Z)),
        "XXXX" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_MINUTE_OR_Z)),

        "XXX" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE_OR_Z)),
        "XXXXX" to listOf(OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE_OR_Z)),

        "n" to listOf(NanoOfSecondPatternSymbol()),

        "S" to listOf(FractionOfSecondPatternSymbol(1)),
        "SS" to listOf(FractionOfSecondPatternSymbol(2)),
        "SSS" to listOf(FractionOfSecondPatternSymbol(3)),
        "SSSS" to listOf(FractionOfSecondPatternSymbol(4)),
        "SSSSS" to listOf(FractionOfSecondPatternSymbol(5)),
        "SSSSSS" to listOf(FractionOfSecondPatternSymbol(6)),
        "SSSSSSS" to listOf(FractionOfSecondPatternSymbol(7)),
        "SSSSSSSS" to listOf(FractionOfSecondPatternSymbol(8)),
        "SSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(9)),
        "SSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(10)),
        "SSSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(11)),
        "SSSSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(12)),
        "SSSSSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(13)),
        "SSSSSSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(14)),
        "SSSSSSSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(15)),
        "SSSSSSSSSSSSSSSS" to listOf(FractionOfSecondPatternSymbol(16)),

        "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX" to listOf(
            YearPatternSymbol(YearFormat.FOUR_DIGIT_ZERO_PADDED),
            TextItem("-"),
            MonthPatternSymbol(MonthFormat.MONTH_NUMBER_ZERO_PADDED),
            TextItem("-"),
            DayOfMonthPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER),
            TextItem("'T'"),
            HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.ZERO_PADDED_NUMBER_24_HOUR),
            TextItem(":"),
            MinuteOfHourPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER),
            TextItem(":"),
            SecondOfMinutePatternPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER),
            TextItem("."),
            FractionOfSecondPatternSymbol(3),
            OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE_OR_Z)),

        "yyyyMMddHHmmssSSSXXXXX" to listOf(
            YearPatternSymbol(YearFormat.FOUR_DIGIT_ZERO_PADDED),
            MonthPatternSymbol(MonthFormat.MONTH_NUMBER_ZERO_PADDED),
            DayOfMonthPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER),
            HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.ZERO_PADDED_NUMBER_24_HOUR),
            MinuteOfHourPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER),
            SecondOfMinutePatternPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER),
            FractionOfSecondPatternSymbol(3),
            OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE_OR_Z))
    )

    @Test
    @Parameters
    fun parse(pair: Pair<String, List<FormatItem>>) {
        val formatPattern = FormatPattern.fromString(pair.first)
        assertEquals(pair.second, formatPattern.formatItems)
    }


    @Test
    fun mostPreciseField() {
        //NOTE: we can't parameterize this unless we want to expose TimestampParser.FormatPatternPrecision as public.
        softAssert {
            for((pattern, expectedResult, expectedHas2DigitYear) in parametersForExaminePatternTest) {
                val result = FormatPattern.fromString(pattern)
                assertThat(result.leastSignificantField)
                    .withFailMessage("Pattern '${pattern}' was used, '${expectedResult}' was expected but result was '${result.leastSignificantField}'")
                    .isEqualTo(expectedResult)
                assertThat(result.has2DigitYear)
                    .withFailMessage("has2DigitYear expected: ${expectedHas2DigitYear} but was ${result.has2DigitYear}, pattern was: '${pattern}'")
                    .isEqualTo(expectedHas2DigitYear)
            }
        }
    }


    private data class MostPreciseFieldTestCase(
        val pattern: String,
        val expectedResult: TimestampField,
        val expectedHas2DigitYear: Boolean = false)

    private val parametersForExaminePatternTest = listOf(

        MostPreciseFieldTestCase("y", TimestampField.YEAR),
        MostPreciseFieldTestCase("yy", TimestampField.YEAR, expectedHas2DigitYear = true),
        MostPreciseFieldTestCase("yyy", TimestampField.YEAR),
        MostPreciseFieldTestCase("yyyy", TimestampField.YEAR),
        MostPreciseFieldTestCase("y M", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("y M d", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("y M d s", TimestampField.SECOND_OF_MINUTE),

        MostPreciseFieldTestCase("M d, y", TimestampField.DAY_OF_MONTH),

        //Delimited with "/"
        MostPreciseFieldTestCase("y/M", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("y/M/d", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("y/M/d/s", TimestampField.SECOND_OF_MINUTE),

        //delimited with "-"
        MostPreciseFieldTestCase("y-M", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("yy-M", TimestampField.MONTH_OF_YEAR, expectedHas2DigitYear = true),
        MostPreciseFieldTestCase("y-M-d", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("y-M-d-s", TimestampField.SECOND_OF_MINUTE),

        //delimited with "':'"
        MostPreciseFieldTestCase("y:M", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("yy:M", TimestampField.MONTH_OF_YEAR, expectedHas2DigitYear = true),
        MostPreciseFieldTestCase("y:M:d", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("y:M:d:s", TimestampField.SECOND_OF_MINUTE),

        //delimited with "'1'"
        MostPreciseFieldTestCase("'1'y'1'", TimestampField.YEAR),
        MostPreciseFieldTestCase("'1'yy'1'", TimestampField.YEAR, expectedHas2DigitYear = true),
        MostPreciseFieldTestCase("'1'y'1'M'1'", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("'1'y'1'M'1'd'1'", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("'1'y'1'M'1'd'1's'1'", TimestampField.SECOND_OF_MINUTE),

        //delimited with "'😸'"
        MostPreciseFieldTestCase("'😸'y'😸'", TimestampField.YEAR),
        MostPreciseFieldTestCase("'😸'yy'😸'", TimestampField.YEAR, expectedHas2DigitYear = true),
        MostPreciseFieldTestCase("'😸'y'😸'M'😸'", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("'😸'y'😸'M'😸'd'😸'", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("'😸'y'😸'M'😸'd'😸's'😸'", TimestampField.SECOND_OF_MINUTE),

        //delimited with "'話家'"
        MostPreciseFieldTestCase("'話家'y'話家'", TimestampField.YEAR),
        MostPreciseFieldTestCase("'話家'yy'話家'", TimestampField.YEAR, expectedHas2DigitYear = true),
        MostPreciseFieldTestCase("'話家'y'話家'M'話家'", TimestampField.MONTH_OF_YEAR),
        MostPreciseFieldTestCase("'話家'y'話家'M'話家'd'話家'", TimestampField.DAY_OF_MONTH),
        MostPreciseFieldTestCase("'話家'y'話家'M'話家'd'話家's'話家'", TimestampField.SECOND_OF_MINUTE),

        //Valid symbols within quotes should not influence the result
        MostPreciseFieldTestCase("y'M d s'", TimestampField.YEAR),
        MostPreciseFieldTestCase("y'y'", TimestampField.YEAR))


}
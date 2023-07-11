package org.partiql.lang.eval.builtins

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.datetime.TimestampParser
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.lang.reflect.Type
import java.math.BigDecimal
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

@RunWith(JUnitParamsRunner::class)
class TimestampParserTest {

    data class ParseTimestampTestCase(
        val pattern: String,
        val timestamp: String,
        val expectedResult: Timestamp
    )

    @Test
    @Parameters
    @TestCaseName
    fun parseTimestampTest(testCase: ParseTimestampTestCase) {
        val result = TimestampParser.parseTimestamp(testCase.timestamp, testCase.pattern)
        // Routing those test case to use org.partiql.value.datetime.Timestamp
        assertEquals(testCase.expectedResult, result)
    }

    // Note: for timestamp fields that may be 1 or 2 digits (i.e. hour, minute, day, month) a single
    // instance of the specifier parses zero padded values without difficulty.  HOWEVER...
    // Multiple repeated values REQUIRE zero padding.  For example, a pattern of "y MM" will parse "2007 06"
    // without difficulty but not "2007 6".  However, a pattern of "y M" will parse either.
    fun parametersForParseTimestampTest(): List<ParseTimestampTestCase> =
        listOf(
            // Year
            // Single "y" symbol parses arbitrary year.
            ParseTimestampTestCase("y", "7", DateTimeValue.timestamp(7)),
            ParseTimestampTestCase("y", "0007", DateTimeValue.timestamp(7)),
            ParseTimestampTestCase("y", "2007", DateTimeValue.timestamp(2007)),
            // Zero padding is required when three or four "y" symbols are used.
            ParseTimestampTestCase("yyy", "0007", DateTimeValue.timestamp(7)),
            ParseTimestampTestCase("yyyy", "0007", DateTimeValue.timestamp(7)),

            // Two "y" symbols parses 2 digit year
            ParseTimestampTestCase("yy", "00", DateTimeValue.timestamp(2000)),
            ParseTimestampTestCase("yy", "01", DateTimeValue.timestamp(2001)),
            ParseTimestampTestCase("yy", "69", DateTimeValue.timestamp(2069)),
            ParseTimestampTestCase("yy", "70", DateTimeValue.timestamp(1970)),
            ParseTimestampTestCase("yy", "71", DateTimeValue.timestamp(1971)),
            ParseTimestampTestCase("yy", "99", DateTimeValue.timestamp(1999)),

            // Month
            // Zero padding is optional with single "M" symbol
            ParseTimestampTestCase("y M", "2007 6", DateTimeValue.timestamp(2007, 6)),
            ParseTimestampTestCase("y M", "2007 6", DateTimeValue.timestamp(2007, 6)),
            ParseTimestampTestCase("y M", "2007 06", DateTimeValue.timestamp(2007, 6)),
            // Two "M" symbols requires zero padding
            ParseTimestampTestCase("y MM", "2007 06", DateTimeValue.timestamp(2007, 6)),
            // Three "M" symbols require three letter month abbreviation
            ParseTimestampTestCase("y MMM", "2007 Jun", DateTimeValue.timestamp(2007, 6)),
            ParseTimestampTestCase("y MMM", "2007 jun", DateTimeValue.timestamp(2007, 6)),
            // Four "M" symbols requires full month name
            ParseTimestampTestCase("y MMMM", "2007 june", DateTimeValue.timestamp(2007, 6)),

            // Day
            // Zero padding is optional with a single "d" symbol
            ParseTimestampTestCase("y M d", "2007 6 5", DateTimeValue.timestamp(2007, 6, 5)),
            ParseTimestampTestCase("y M d", "2007 6 05", DateTimeValue.timestamp(2007, 6, 5)),
            // Two "d" symbols require zero padding
            ParseTimestampTestCase("y M dd", "2007 6 05", DateTimeValue.timestamp(2007, 6, 5)),

            // Hour
            ParseTimestampTestCase("y M d H", "2007 6 5 9", DateTimeValue.timestamp(2007, 6, 5, 9, 0)),
            ParseTimestampTestCase("y M d h a", "2007 6 5 9 am", DateTimeValue.timestamp(2007, 6, 5, 9, 0)),
            ParseTimestampTestCase("y M d h a", "2007 6 5 9 pm", DateTimeValue.timestamp(2007, 6, 5, 21, 0)),
            ParseTimestampTestCase("y M d H", "2007 6 5 09", DateTimeValue.timestamp(2007, 6, 5, 9, 0)),
            ParseTimestampTestCase("y M d HH", "2007 6 5 09", DateTimeValue.timestamp(2007, 6, 5, 9, 0)),

            // Minute (same rules with 1 "m" vs "mm")
            ParseTimestampTestCase("y M d H m", "2007 6 5 9 8", DateTimeValue.timestamp(2007, 6, 5, 9, 8)),
            ParseTimestampTestCase("y M d H m", "2007 6 5 9 08", DateTimeValue.timestamp(2007, 6, 5, 9, 8)),
            ParseTimestampTestCase("y M d H mm", "2007 6 5 9 08", DateTimeValue.timestamp(2007, 6, 5, 9, 8)),

            // Second
            ParseTimestampTestCase("y M d H m s", "2007 6 5 9 8 6", DateTimeValue.timestamp(2007, 6, 5, 9, 8, 6)),
            ParseTimestampTestCase("y M d H m s", "2007 6 5 9 8 06", DateTimeValue.timestamp(2007, 6, 5, 9, 8, 6)),
            ParseTimestampTestCase("y M d H m ss", "2007 6 5 9 8 06", DateTimeValue.timestamp(2007, 6, 5, 9, 8, 6)),

            // 12-hour mode
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 PM", DateTimeValue.timestamp(2007, 6, 5, 13, 2, 6)),
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 AM", DateTimeValue.timestamp(2007, 6, 5, 1, 2, 6)),
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 pm", DateTimeValue.timestamp(2007, 6, 5, 13, 2, 6)),
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 am", DateTimeValue.timestamp(2007, 6, 5, 1, 2, 6)),

            // Second fraction, where precision of the fraction is specified by the number of S symbols.
            //  S   -> 1/10th of a decimalSecond
            //  SS  -> 1/100th of a decimalSecond
            //  SSS -> 1/1000th of a decimalSecond (millisecond)
            //  ...
            //  up to 1 nanosecond (9 'S' symbols)
            // Zero padding is required in this here because the value is intended to be on the right of a decimal point.
            ParseTimestampTestCase("y M d H m s S", "2007 6 5 9 8 6 2", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(62, 1))),
            ParseTimestampTestCase("y M d H m s SS", "2007 6 5 9 8 6 25", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(625, 2))),
            ParseTimestampTestCase("y M d H m s SSS", "2007 6 5 9 8 6 256", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(6256, 3))),
            ParseTimestampTestCase("y M d H m s SSSSSSSSS", "2007 6 5 9 8 6 123456789", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(6123456789, 9))),

            // Nanosecond
            // Zero padding is optional
            ParseTimestampTestCase("y M d H m s n", "2007 6 5 9 8 6 100", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(100, 9))),
            ParseTimestampTestCase("y M d H m s n", "2007 6 5 9 8 6 00100", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(100, 9))),
            ParseTimestampTestCase("y M d H m s n", "2007 6 5 9 8 6 123456789", DateTimeValue.timestamp(2007, 6, 5, 9, 8, BigDecimal.valueOf(6123456789, 9))),

            // Ion timestamp precision variants
            ParseTimestampTestCase("y'T'", "1969T", DateTimeValue.timestamp(1969)),
            ParseTimestampTestCase("y-MM'T'", "1969-07T", DateTimeValue.timestamp(1969, 7)),
            ParseTimestampTestCase("y-MM-dd'T'", "1969-07-20T", DateTimeValue.timestamp(1969, 7, 20)),
            ParseTimestampTestCase("y-MM-dd'T'H:m", "1969-07-20T20:18", DateTimeValue.timestamp(1969, 7, 20, 20, 18)),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss", "1969-07-20T20:18:13", DateTimeValue.timestamp(1969, 7, 20, 20, 18, 13)),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.S", "1969-07-20T20:18:00.1", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(1, 1))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SS", "1969-07-20T20:18:00.12", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(12, 2))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSS", "1969-07-20T20:18:00.123", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(123, 3))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSS", "1969-07-20T20:18:00.1234", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(1234, 4))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSS", "1969-07-20T20:18:00.12345", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(12345, 5))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSS", "1969-07-20T20:18:00.123456", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(123456, 6))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSSS", "1969-07-20T20:18:00.1234567", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(1234567, 7))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSSSS", "1969-07-20T20:18:00.12345678", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(12345678, 8))),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSSSSS", "1969-07-20T20:18:00.123456789", DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(123456789, 9))),

            // Ion timestamp with explicit unknown offset.  The "" at the end of the timestamp string signifies
            // an unknown offset.  ("+00:00" signifies UTC/GMT.)
            // Note:  these are tests removed because there's no way I can determine to reliably handle negative zero offset
            // indicating unknown offset, even with an ugly hack.
//            ParseTimestampTestCase("y-MM-dd'T'H:m:ssXXXXX", "1969-07-20T20:18:00-00:00", Timestamp.valueOf("1969-07-20T20:18:00-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -00:00", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -0", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -00", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -0000", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -00:00", Timestamp.valueOf("1969-07-20T20:01-00:00")),

            // Known offsets (caveat:  DateTimeFormatter throws exception if offset is longer than +/- 18h while Ion timestamp is +/- 24h)
            // Note that DateTimeFormatter is unfortunately unable to recognize a negative zero offset as an unknown offset like Ion does.

            // Capital X allows the use of "Z" to represent zero offset from GMT.
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 Z", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m XX", "1969 07 20 20 01 Z", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m XXX", "1969 07 20 20 01 Z", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m XXXX", "1969 07 20 20 01 Z", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 Z", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),

            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 Z", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +0000", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -0000", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +0500", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(5, 0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -0500", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-5, 0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +02", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(2, 0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -02", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-2, 0))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +0203", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(2, 3))),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -0203", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-2, -3))),
            ParseTimestampTestCase("y M d H m XXX", "1969 07 20 20 01 +02:03", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(2, 3))),
            ParseTimestampTestCase("y M d H m XXX", "1969 07 20 20 01 -02:03", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-2, -3))),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -01:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-1, 0))),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 +01:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(1, 0))),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -18:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-18, 0))),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 +18:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(18, 0))),

            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 -00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m xx", "1969 07 20 20 01 +0000", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m xx", "1969 07 20 20 01 -0000", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 +00:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 -00:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(0))),

            // This might be a bug in Java's DateTimeFormatter, but lowercase 'x' cannot parse "+0000" like uppercase "X" can
            // even though by all appearances, it should.
            // ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +0100", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(1, 0))),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +02", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(2, 0))),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 -02", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-2, 0))),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +0203", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(2, 3))),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 -0203", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-2, -3))),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 +02:03", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(2, 3))),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 -02:03", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-2, -3))),

            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 -01:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-1, 0))),
            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 +01:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(1, 0))),
            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 -18:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(-18, 0))),
            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 +18:00", DateTimeValue.timestamp(1969, 7, 20, 20, 1, BigDecimal.ZERO, TimeZone.UtcOffset.of(18, 0))),

            // Date format with whitespace surrounding the string
            ParseTimestampTestCase(" y M d ", " 2007 6 5 ", DateTimeValue.timestamp(2007, 6, 5)),
            ParseTimestampTestCase("'\t'y M d'\t'", "\t2007 6 5\t", DateTimeValue.timestamp(2007, 6, 5)),

            // Crazy delimiters
            ParseTimestampTestCase("'Some'y'crazy'M'delimiter'd'here'", "Some2007crazy6delimiter5here", DateTimeValue.timestamp(2007, 6, 5)),
            ParseTimestampTestCase("'😸'y'😸'M'😸'd'😸'", "😸2007😸6😸5😸", DateTimeValue.timestamp(2007, 6, 5)),

            // No delimiters at all
            ParseTimestampTestCase("yyyyMMddHHmmss", "20070605040302", DateTimeValue.timestamp(2007, 6, 5, 4, 3, 2))
        )

    @Test
    @Parameters
    @TestCaseName
    fun parseTimestampExceptionTest(testCase: ParseFailureTestCase) {
        try {
            val ts = TimestampParser.parseTimestamp(testCase.timestampString, testCase.formatPattern)
            fail("The unexpectedly parsed timestamp was: " + ts)
        } catch (ex: EvaluationException) {
            assertEquals(testCase.expectedErrorCode, ex.errorCode)
            if (testCase.expectedCauseType == null) {
                assertNull(ex.cause)
            } else {
                assertNotNull(ex.cause)
                val actualType: Type = (ex.cause as Exception).javaClass
                assertEquals(testCase.expectedCauseType, actualType)

                // Another disadvantage of using DateTimeFormatter the lack of detailed information about errors that we can
                // get out of it -- there is no good way to assert these error cases were in error for the expected reason...
                // It would be highly useful to have discrete properties for error code and index into the timestamp where
                // the parsing failure occurred, similar to what we have for EvaluationException.  This information *does*
                // exist in the exception message, which we are asserting on below *only* for the purposes of making sure that
                // the test case failed for the expected reason.  The error messages are not meant to be part of the
                // contract we expose to the client.

                if (testCase.expectedCauseMessage != null) {
                    assertEquals(testCase.expectedCauseMessage, ex.cause!!.message)
                }
            }
        }
    }

    data class ParseFailureTestCase(
        val formatPattern: String,
        val timestampString: String,
        val expectedCauseType: Type? = null,
        val expectedCauseMessage: String? = null,
        val expectedErrorCode: ErrorCode = ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE
    )

    fun parametersForParseTimestampExceptionTest() = listOf(

        // Year outside of range (year is 0)
        ParseFailureTestCase(
            "yyyy-MM-dd",
            "0000,1,1",
            DateTimeParseException::class.java,
            "Text '0000,1,1' could not be parsed: Invalid value for YearOfEra (valid values 1 - 999999999/1000000000): 0"
        ),

        // Month outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017,0,1",
            DateTimeParseException::class.java,
            "Text '2017,0,1' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 0"
        ),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-13,1",
            DateTimeParseException::class.java,
            "Text '2017-13,1' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13"
        ),

        // Day outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017,1,0",
            DateTimeParseException::class.java,
            "Text '2017,1,0' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 0"
        ),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017,1-32",
            DateTimeParseException::class.java,
            "Text '2017,1-32' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 32"
        ),

        // Hour outside of range (AM/PM)
        // ParseFailureTestCase("2017,1,1 00:01 PM", "yyyy-MM-dd hh:mm a", ""), //In 12 hour mode, 0 is considered 12...
        ParseFailureTestCase(
            "yyyy-MM-dd hh:mm a",
            "2017,1,1 13,1 PM",
            DateTimeParseException::class.java,
            "Text '2017,1,1 13,1 PM' could not be parsed: Invalid value for ClockHourOfAmPm (valid values 1 - 12): 13"
        ),

        // Hour outside of range (24hr)
        ParseFailureTestCase(
            "yyyy-MM-dd HH:mm",
            "2017,1,1 24,1",
            DateTimeParseException::class.java,
            "Text '2017,1,1 24,1' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24"
        ),

        // Minute outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd HH:mm",
            "2017,1,1 01:60",
            DateTimeParseException::class.java,
            "Text '2017,1,1 01:60' could not be parsed: Invalid value for MinuteOfHour (valid values 0 - 59): 60"
        ),

        // Second outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd HH:mm:ss",
            "2017,1,1 01,1:60",
            DateTimeParseException::class.java,
            "Text '2017,1,1 01,1:60' could not be parsed: Invalid value for SecondOfMinute (valid values 0 - 59): 60"
        ),

        // Whitespace surrounding custom timestamp
        ParseFailureTestCase(
            "yyyy-MM-dd",
            " 2017,1,1",
            DateTimeParseException::class.java,
            "Text ' 2017,1,1' could not be parsed at index 0"
        ),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017,1,1 ",
            DateTimeParseException::class.java,
            "Text '2017,1,1 ' could not be parsed, unparsed text found at index 10"
        ),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            " 2017,1,1 ",
            DateTimeParseException::class.java,
            "Text ' 2017,1,1 ' could not be parsed at index 0"
        ),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017,1,1 ",
            DateTimeParseException::class.java,
            "Text '2017,1,1 ' could not be parsed, unparsed text found at index 10"
        ),

        // Required zero padding not present (Zero padding required because 2 or more consecutive format symbols)
        ParseFailureTestCase(
            "yyy M d H m s", // a 3 digit year doesn't seem to make sense but the DateTimeFormatter allows it.
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 0"
        ),

        ParseFailureTestCase(
            "yyyy M d H m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 0"
        ),

        ParseFailureTestCase(
            "y MM d H m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 2"
        ),

        ParseFailureTestCase(
            "y M dd H m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 4"
        ),

        ParseFailureTestCase(
            "y M d HH m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 6"
        ),

        ParseFailureTestCase(
            "y M d H mm s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 8"
        ),

        ParseFailureTestCase(
            "y M d H m ss",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 10"
        ),

        // 1 digit offset.  Ideally this would not be a failure case but they appear to have left 1 digit offsets
        // out of the JDK8 spec:  https://bugs.openjdk.java.net/browse/JDK-8066806
        ParseFailureTestCase(
            "y M d H m x",
            "1969 07 20 20 01 +2",
            DateTimeParseException::class.java,
            "Text '1969 07 20 20 01 +2' could not be parsed at index 17"
        ),

        // Offset exceeds allowable range
        // Note:  Java's DateTimeFormatter only allows +/- 18h but IonJava's Timestamp allows +/- 23:59.
        ParseFailureTestCase(
            "y M d H m x",
            "1969 07 20 20 01 +2400",
            DateTimeParseException::class.java
        ),
        // Note: exception message differs in JDK versions later than 1.8
        // "Text '1969 07 20 20 01 +2400' could not be parsed: Zone offset not in valid range: -18:00 to +18:00"),

        ParseFailureTestCase(
            "yyyy M d H m x",
            "1969 07 20 20 01 -2400",
            DateTimeParseException::class.java
        ),
        // Note: exception message differs in JDK versions later than 1.8
        // "Text '1969 07 20 20 01 -2400' could not be parsed: Zone offset not in valid range: -18:00 to +18:00"),

        // Offset not ending on a minute boundary (error condition detected by TimestampParser)
        ParseFailureTestCase(
            "yyyy M d H m xxxxx",
            "1969 07 20 20 01 +01,0,1",
            expectedErrorCode = ErrorCode.EVALUATOR_PRECISION_LOSS_WHEN_PARSING_TIMESTAMP
        ),

        // Three digit offset
        ParseFailureTestCase(
            "yyyy M d H m x",
            "1969 07 20 20 01 -240",
            DateTimeParseException::class.java
        )
        // Note: exception message differs in JDK versions later than 1.8
        // "Text '1969 07 20 20 01 -240' could not be parsed, unparsed text found at index 20")
    )

    @Test
    @Parameters
    fun invalidFormatPatternTest(testCase: InvalidFormatPatternTestCase) {
        try {
            TimestampParser.parseTimestamp("doesn't matter shouldn't get parsed anyway", testCase.pattern)
            fail("didn't throw")
        } catch (ex: EvaluationException) {
            assertEquals(testCase.expectedErrorCode, ex.errorCode)
            assertNull(ex.cause)
        }
    }

    data class InvalidFormatPatternTestCase(val pattern: String, val expectedErrorCode: ErrorCode)

    fun parametersForInvalidFormatPatternTest() =
        listOf(
            InvalidFormatPatternTestCase("", ErrorCode.EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN),
            InvalidFormatPatternTestCase("asdf", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_TOKEN),
            InvalidFormatPatternTestCase("yy-mm-dd-'Thh:mm", ErrorCode.EVALUATOR_UNTERMINATED_TIMESTAMP_FORMAT_PATTERN_TOKEN),
            InvalidFormatPatternTestCase("MMMMM", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL_FOR_PARSING),
            InvalidFormatPatternTestCase("ddd", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL),
            InvalidFormatPatternTestCase("m hhh", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL),
            InvalidFormatPatternTestCase("m HHH", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL),
            InvalidFormatPatternTestCase("mmm", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL),
            InvalidFormatPatternTestCase("sss", ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL)
        )
    // unterminated quote
}

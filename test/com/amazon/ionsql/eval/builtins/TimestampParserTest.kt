package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.eval.builtins.TimestampParser.*
import com.amazon.ionsql.util.*
import junitparams.*
import junitparams.naming.*
import org.junit.*
import org.junit.runner.*
import java.lang.reflect.*
import java.time.format.*
import java.time.temporal.*
import kotlin.test.*

@RunWith(JUnitParamsRunner::class)
class TimestampParserTest {

    @Test
    fun fromPattern() {
        //NOTE: we can't parameterize this unless we want to expose TimestampParser.FormatPatternPrecision as public.
        softAssert {
            for((pattern, expectedResult, expectedHas2DigitYear) in parametersForExaminePatternTest) {
                val result = TimestampParser.FormatPatternInfo.fromPattern(pattern)
                assertThat(result.precision)
                    .withFailMessage("Pattern '${pattern}' was used, '${expectedResult}' was expected but result was '${result.precision}'")
                    .isEqualTo(expectedResult)
                assertThat(result.has2DigitYear)
                    .withFailMessage("has2DigitYear expected: ${expectedHas2DigitYear} but was ${result.has2DigitYear}, pattern was: '${pattern}'")
                    .isEqualTo(expectedHas2DigitYear)
            }
        }
    }

    private data class FromPatternTestCase(
        val pattern: String,
        val expectedResult: FormatPatternPrecision,
        val expectedHas2DigitYear: Boolean = false)

    private val parametersForExaminePatternTest = listOf(
            FromPatternTestCase("", TimestampParser.FormatPatternPrecision.UNKNOWN),

            FromPatternTestCase("y", FormatPatternPrecision.YEAR),
            FromPatternTestCase("yy", FormatPatternPrecision.YEAR, expectedHas2DigitYear = true),
            FromPatternTestCase("yyy", FormatPatternPrecision.YEAR),
            FromPatternTestCase("yyyy", FormatPatternPrecision.YEAR),
            FromPatternTestCase("y M", FormatPatternPrecision.MONTH),
            FromPatternTestCase("y M d", FormatPatternPrecision.DAY),
            FromPatternTestCase("y M d s", FormatPatternPrecision.SECOND),

            FromPatternTestCase("M d, y", FormatPatternPrecision.DAY),

            //Delimited with "/"
            FromPatternTestCase("y/M", FormatPatternPrecision.MONTH),
            FromPatternTestCase("y/M/d", FormatPatternPrecision.DAY),
            FromPatternTestCase("y/M/d/s", FormatPatternPrecision.SECOND),

            //delimited with "-"
            FromPatternTestCase("y-M", FormatPatternPrecision.MONTH),
            FromPatternTestCase("yy-M", FormatPatternPrecision.MONTH, expectedHas2DigitYear = true),
            FromPatternTestCase("y-M-d", FormatPatternPrecision.DAY),
            FromPatternTestCase("y-M-d-s", FormatPatternPrecision.SECOND),

            //delimited with "':'"
            FromPatternTestCase("y:M", FormatPatternPrecision.MONTH),
            FromPatternTestCase("yy:M", FormatPatternPrecision.MONTH, expectedHas2DigitYear = true),
            FromPatternTestCase("y:M:d", FormatPatternPrecision.DAY),
            FromPatternTestCase("y:M:d:s", FormatPatternPrecision.SECOND),

            //delimited with "'1'"
            FromPatternTestCase("'1'y'1'", FormatPatternPrecision.YEAR),
            FromPatternTestCase("'1'yy'1'", FormatPatternPrecision.YEAR, expectedHas2DigitYear = true),
            FromPatternTestCase("'1'y'1'M'1'", FormatPatternPrecision.MONTH),
            FromPatternTestCase("'1'y'1'M'1'd'1'", FormatPatternPrecision.DAY),
            FromPatternTestCase("'1'y'1'M'1'd'1's'1'", FormatPatternPrecision.SECOND),

            //delimited with "'😸'"
            FromPatternTestCase("'😸'y'😸'", FormatPatternPrecision.YEAR),
            FromPatternTestCase("'😸'yy'😸'", FormatPatternPrecision.YEAR, expectedHas2DigitYear = true),
            FromPatternTestCase("'😸'y'😸'M'😸'", FormatPatternPrecision.MONTH),
            FromPatternTestCase("'😸'y'😸'M'😸'd'😸'", FormatPatternPrecision.DAY),
            FromPatternTestCase("'😸'y'😸'M'😸'd'😸's'😸'", FormatPatternPrecision.SECOND),

            //delimited with "'話家'"
            FromPatternTestCase("'話家'y'話家'", FormatPatternPrecision.YEAR),
            FromPatternTestCase("'話家'yy'話家'", FormatPatternPrecision.YEAR, expectedHas2DigitYear = true),
            FromPatternTestCase("'話家'y'話家'M'話家'", FormatPatternPrecision.MONTH),
            FromPatternTestCase("'話家'y'話家'M'話家'd'話家'", FormatPatternPrecision.DAY),
            FromPatternTestCase("'話家'y'話家'M'話家'd'話家's'話家'", FormatPatternPrecision.SECOND),

            //Valid symbols within quotes should not influence the result
            FromPatternTestCase("y'M d s'", FormatPatternPrecision.YEAR),
            FromPatternTestCase("y'y'", FormatPatternPrecision.YEAR),

            //Unsupported symbols
            FromPatternTestCase("y M d s q", FormatPatternPrecision.UNKNOWN),
            FromPatternTestCase("q y M d s", FormatPatternPrecision.UNKNOWN),
            FromPatternTestCase("1", FormatPatternPrecision.UNKNOWN),
            FromPatternTestCase("😸", FormatPatternPrecision.UNKNOWN),
            FromPatternTestCase("話", FormatPatternPrecision.UNKNOWN))


     data class ParseTimestampTestCase(
        val pattern: String,
        val timestamp: String,
        val expectedResult: Timestamp)

     @Test
     @Parameters
     @TestCaseName
     fun parseTimestampTest(testCase: ParseTimestampTestCase) {
         val result = TimestampParser.parseTimestamp(testCase.timestamp, testCase.pattern)
         assertEquals(testCase.expectedResult, result)
     }

    //Note: for timestamp fields that may be 1 or 2 digits (i.e. hour, minute, day, month) a single
    //instance of the specifier parses zero padded values without difficulty.  HOWEVER...
    //Multiple repeated values REQUIRE zero padding.  For example, a pattern of "y MM" will parse "2007 06"
    //without difficulty but not "2007 6".  However, a pattern of "y M" will parse either.
    fun parametersForParseTimestampTest(): List<ParseTimestampTestCase> =
        listOf(
            //Year
            //Single "y" symbol parses arbitary year.
            ParseTimestampTestCase("y", "7", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("y", "0007", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("y", "2007", Timestamp.valueOf("2007T")),
            //Zero padding is required when three or four "y" symbols are used.
            ParseTimestampTestCase("yyy", "0007", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("yyyy", "0007", Timestamp.valueOf("0007T")),

            //Two "y" symbols parses 2 digit year
            ParseTimestampTestCase("yy", "00", Timestamp.valueOf("2000T")),
            ParseTimestampTestCase("yy", "01", Timestamp.valueOf("2001T")),
            ParseTimestampTestCase("yy", "69", Timestamp.valueOf("2069T")),
            ParseTimestampTestCase("yy", "70", Timestamp.valueOf("1970T")),
            ParseTimestampTestCase("yy", "71", Timestamp.valueOf("1971T")),
            ParseTimestampTestCase("yy", "99", Timestamp.valueOf("1999T")),

            //Month
            //Zero padding is optional with single "M" symbol
            ParseTimestampTestCase("y M", "2007 6", Timestamp.valueOf("2007-06T")),
            ParseTimestampTestCase("y M", "2007 6", Timestamp.valueOf("2007-06T")),
            ParseTimestampTestCase("y M", "2007 06", Timestamp.valueOf("2007-06T")),
            //Two "M" symbols requires zero padding
            ParseTimestampTestCase("y MM", "2007 06", Timestamp.valueOf("2007-06T")),
            //Three "M" symbols require three letter month abbreviation
            ParseTimestampTestCase("y MMM", "2007 Jun", Timestamp.valueOf("2007-06T")),
            ParseTimestampTestCase("y MMM", "2007 jun", Timestamp.valueOf("2007-06T")),
            //Four "M" symbols requires full month name
            ParseTimestampTestCase("y MMMM", "2007 june", Timestamp.valueOf("2007-06T")),

            //Day
            //Zero padding is optional with a single "d" symbol
            ParseTimestampTestCase("y M d", "2007 6 5", Timestamp.valueOf("2007-06-05T")),
            ParseTimestampTestCase("y M d", "2007 6 05", Timestamp.valueOf("2007-06-05T")),
            //Two "d" symbols require zero padding
            ParseTimestampTestCase("y M dd", "2007 6 05", Timestamp.valueOf("2007-06-05T")),

            //Hour
            //Hour must also be specified with minutes at least. Both of them follow same rules above where a
            //single symbol specifies that zero padding is optional and 2 symbols requires it.
            ParseTimestampTestCase("y M d H m", "2007 6 5 9 8", Timestamp.valueOf("2007-06-05T09:08-00:00")),
            ParseTimestampTestCase("y M d H m", "2007 6 5 09 8", Timestamp.valueOf("2007-06-05T09:08-00:00")),
            ParseTimestampTestCase("y M d HH m", "2007 6 5 09 8", Timestamp.valueOf("2007-06-05T09:08-00:00")),

            //Minute (same rules with 1 "m" vs "mm")
            ParseTimestampTestCase("y M d H m", "2007 6 5 9 8", Timestamp.valueOf("2007-06-05T09:08-00:00")),
            ParseTimestampTestCase("y M d H m", "2007 6 5 9 08", Timestamp.valueOf("2007-06-05T09:08-00:00")),
            ParseTimestampTestCase("y M d H mm", "2007 6 5 9 08", Timestamp.valueOf("2007-06-05T09:08-00:00")),

            //Second
            ParseTimestampTestCase("y M d H m s", "2007 6 5 9 8 6", Timestamp.valueOf("2007-06-05T09:08:06-00:00")),
            ParseTimestampTestCase("y M d H m s", "2007 6 5 9 8 06", Timestamp.valueOf("2007-06-05T09:08:06-00:00")),
            ParseTimestampTestCase("y M d H m ss", "2007 6 5 9 8 06", Timestamp.valueOf("2007-06-05T09:08:06-00:00")),

            //12-hour mode
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 PM", Timestamp.valueOf("2007-06-05T13:02:06-00:00")),
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 AM", Timestamp.valueOf("2007-06-05T01:02:06-00:00")),
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 pm", Timestamp.valueOf("2007-06-05T13:02:06-00:00")),
            ParseTimestampTestCase("y M d h m s a", "2007 6 5 1 2 6 am", Timestamp.valueOf("2007-06-05T01:02:06-00:00")),

            //Second fraction, where precision of the fraction is specified by the number of S symbols.
            //  S   -> 1/10th of a second
            //  SS  -> 1/100th of a second
            //  SSS -> 1/1000th of a second (millisecond)
            //  ...
            //  up to 1 nanosecond (9 'S' symbols)
            //Zero padding is required in this here because the value is intended to be on the right of a decimal point.
            ParseTimestampTestCase("y M d H m s S", "2007 6 5 9 8 6 2", Timestamp.valueOf("2007-06-05T09:08:06.2-00:00")),
            ParseTimestampTestCase("y M d H m s SS", "2007 6 5 9 8 6 25", Timestamp.valueOf("2007-06-05T09:08:06.25-00:00")),
            ParseTimestampTestCase("y M d H m s SSS", "2007 6 5 9 8 6 256", Timestamp.valueOf("2007-06-05T09:08:06.256-00:00")),
            ParseTimestampTestCase("y M d H m s SSSSSSSSS", "2007 6 5 9 8 6 123456789", Timestamp.valueOf("2007-06-05T09:08:06.123456789-00:00")),

            //Nanosecond
            //Zero padding is optional
            ParseTimestampTestCase("y M d H m s n", "2007 6 5 9 8 6 100", Timestamp.valueOf("2007-06-05T09:08:06.0000001-00:00")),
            ParseTimestampTestCase("y M d H m s n", "2007 6 5 9 8 6 00100", Timestamp.valueOf("2007-06-05T09:08:06.0000001-00:00")),
            ParseTimestampTestCase("y M d H m s n", "2007 6 5 9 8 6 123456789", Timestamp.valueOf("2007-06-05T09:08:06.123456789-00:00")),
            //10 consecutive n's are is nonsensical but DateTimeFormatter accepts it
            ParseTimestampTestCase("y M d H m s nnnnnnnnnn", "2007 6 5 9 8 6 0123456789", Timestamp.valueOf("2007-06-05T09:08:06.123456789-00:00")),

            //Nonsensical numbers of leading zeros in year field (while Java's DateTimeFormatter supports this but
            //Ion Timestamp will barf if an attempt is made to instantiate a Timestamp with a year > 9999.)
            ParseTimestampTestCase("yyyyy", "00007", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("yyyyyy", "000007", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("yyyyyyy", "0000007", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("yyyyyyyyyyyyyyyyy", "00000000000000007", Timestamp.valueOf("0007T")),

            //Ion timestamp precision variants
            ParseTimestampTestCase("y'T'", "1969T", Timestamp.valueOf("1969T")),
            ParseTimestampTestCase("y-MM'T'", "1969-07T", Timestamp.valueOf("1969-07T")),
            ParseTimestampTestCase("y-MM-dd'T'", "1969-07-20T", Timestamp.valueOf("1969-07-20T")),
            ParseTimestampTestCase("y-MM-dd'T'H:m", "1969-07-20T20:18", Timestamp.valueOf("1969-07-20T20:18-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss", "1969-07-20T20:18:13", Timestamp.valueOf("1969-07-20T20:18:13-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.S", "1969-07-20T20:18:00.1", Timestamp.valueOf("1969-07-20T20:18:00.1-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SS", "1969-07-20T20:18:00.12", Timestamp.valueOf("1969-07-20T20:18:00.12-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSS", "1969-07-20T20:18:00.123", Timestamp.valueOf("1969-07-20T20:18:00.123-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSS", "1969-07-20T20:18:00.1234", Timestamp.valueOf("1969-07-20T20:18:00.1234-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSS", "1969-07-20T20:18:00.12345", Timestamp.valueOf("1969-07-20T20:18:00.12345-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSS", "1969-07-20T20:18:00.123456", Timestamp.valueOf("1969-07-20T20:18:00.123456-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSSS", "1969-07-20T20:18:00.1234567", Timestamp.valueOf("1969-07-20T20:18:00.1234567-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSSSS", "1969-07-20T20:18:00.12345678", Timestamp.valueOf("1969-07-20T20:18:00.12345678-00:00")),
            ParseTimestampTestCase("y-MM-dd'T'H:m:ss.SSSSSSSSS", "1969-07-20T20:18:00.123456789", Timestamp.valueOf("1969-07-20T20:18:00.123456789-00:00")),

            //Ion timestamp with explicit unknown offset.  The "-00:00" at the end of the timestamp string signifies
            //an unknown offset.  ("+00:00" signifies UTC/GMT.)
            //Note:  these are tests removed because there's no way I can determine to reliably handle negative zero offset
            //indicating unknown offset, even with an ugly hack.
//            ParseTimestampTestCase("y-MM-dd'T'H:m:ssXXXXX", "1969-07-20T20:18:00-00:00", Timestamp.valueOf("1969-07-20T20:18:00-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -00:00", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -0", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -00", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -0000", Timestamp.valueOf("1969-07-20T20:01-00:00")),
//            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -00:00", Timestamp.valueOf("1969-07-20T20:01-00:00")),

            //Known offsets (caveat:  DateTimeFormatter throws exception if offset is longer than +/- 18h while Ion timestamp is +/- 24h)
            //Note that DateTimeFormatter is unfortunately unable to recognize a negative zero offset as an unknown offset like Ion does.

            //Capital X allows the use of "Z" to represent zero offset from GMT.
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 Z", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m XX", "1969 07 20 20 01 Z", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m XXX", "1969 07 20 20 01 Z", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m XXXX", "1969 07 20 20 01 Z", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 Z", Timestamp.valueOf("1969-07-20T20:01Z")),
            
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 Z", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +0500", Timestamp.valueOf("1969-07-20T20:01+05:00")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -0500", Timestamp.valueOf("1969-07-20T20:01-05:00")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +02", Timestamp.valueOf("1969-07-20T20:01+02:00")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -02", Timestamp.valueOf("1969-07-20T20:01-02:00")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 +0203", Timestamp.valueOf("1969-07-20T20:01+02:03")),
            ParseTimestampTestCase("y M d H m X", "1969 07 20 20 01 -0203", Timestamp.valueOf("1969-07-20T20:01-02:03")),
            ParseTimestampTestCase("y M d H m XXX", "1969 07 20 20 01 +02:03", Timestamp.valueOf("1969-07-20T20:01+02:03")),
            ParseTimestampTestCase("y M d H m XXX", "1969 07 20 20 01 -02:03", Timestamp.valueOf("1969-07-20T20:01-02:03")),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -01:00", Timestamp.valueOf("1969-07-20T20:01-01:00")),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 +01:00", Timestamp.valueOf("1969-07-20T20:01+01:00")),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 -18:00", Timestamp.valueOf("1969-07-20T20:01-18:00")),
            ParseTimestampTestCase("y M d H m XXXXX", "1969 07 20 20 01 +18:00", Timestamp.valueOf("1969-07-20T20:01+18:00")),

            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +00", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 -00", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m xx", "1969 07 20 20 01 +0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m xx", "1969 07 20 20 01 -0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 +00:00", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 -00:00", Timestamp.valueOf("1969-07-20T20:01Z")),

            //This might be a bug in Java's DateTimeFormatter, but lowercase 'x' cannot parse "+0000" like uppercase "X" can
            //even though by all appearances, it should.
            //ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +0100", Timestamp.valueOf("1969-07-20T20:01+01:00")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +02", Timestamp.valueOf("1969-07-20T20:01+02:00")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 -02", Timestamp.valueOf("1969-07-20T20:01-02:00")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 +0203", Timestamp.valueOf("1969-07-20T20:01+02:03")),
            ParseTimestampTestCase("y M d H m x", "1969 07 20 20 01 -0203", Timestamp.valueOf("1969-07-20T20:01-02:03")),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 +02:03", Timestamp.valueOf("1969-07-20T20:01+02:03")),
            ParseTimestampTestCase("y M d H m xxx", "1969 07 20 20 01 -02:03", Timestamp.valueOf("1969-07-20T20:01-02:03")),

            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 -01:00", Timestamp.valueOf("1969-07-20T20:01-01:00")),
            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 +01:00", Timestamp.valueOf("1969-07-20T20:01+01:00")),
            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 -18:00", Timestamp.valueOf("1969-07-20T20:01-18:00")),
            ParseTimestampTestCase("y M d H m xxxxx", "1969 07 20 20 01 +18:00", Timestamp.valueOf("1969-07-20T20:01+18:00")),

            ParseTimestampTestCase("y M d H m Z", "1969 07 20 20 01 +0000", Timestamp.valueOf("1969-07-20T20:01Z")),
            ParseTimestampTestCase("y M d H m Z", "1969 07 20 20 01 +0800", Timestamp.valueOf("1969-07-20T20:01+08:00")),
            ParseTimestampTestCase("y M d H m Z", "1969 07 20 20 01 -0800", Timestamp.valueOf("1969-07-20T20:01-08:00")),

            //Date format with whitespace surrounding the string
            ParseTimestampTestCase(" y M d ", " 2007 6 5 ", Timestamp.valueOf("2007-06-05T")),
            ParseTimestampTestCase("'\t'y M d'\t'", "\t2007 6 5\t", Timestamp.valueOf("2007-06-05T")),

            //Crazy delimiters
            ParseTimestampTestCase("'Some'y'crazy'M'delimiter'd'here'", "Some2007crazy6delimiter5here", Timestamp.valueOf("2007-06-05T")),
            ParseTimestampTestCase("'😸'y'😸'M'😸'd'😸'", "😸2007😸6😸5😸", Timestamp.valueOf("2007-06-05T")),

            //No delimiters at all
            ParseTimestampTestCase("yyyyMMddHHmmss", "20070605040302", Timestamp.valueOf("2007-06-05T04:03:02-00:00")),

            //These cases probably shouldn't be allowed at all but DateTimeFormatter allows it as long as the values
            //parsed by each of the duplicate symbols match.  (!)
            ParseTimestampTestCase("y y", "7 7", Timestamp.valueOf("0007T")),
            ParseTimestampTestCase("y M M", "1 7 7", Timestamp.valueOf("0001-07T")),
            ParseTimestampTestCase("y M MMMM", "1 7 July", Timestamp.valueOf("0001-07T")),
            ParseTimestampTestCase("y yyyy M MM d dd H HH m mm s ss", "1 0001 7 07 8 08 9 09 6 06 1 01", Timestamp.valueOf("0001-07-08T09:06:01-00:00"))
        )

    @Test
    @Parameters
    @TestCaseName
    fun parseTimestampExceptionTest(testCase: ParseFailureTestCase) {
        try {
            val ts = TimestampParser.parseTimestamp(testCase.timestampString, testCase.formatPattern)
            fail("The unexpectedly parsed timestamp was: " + ts)
        } catch(ex: EvaluationException) {
            assertEquals(testCase.expectedErrorCode, ex.errorCode)

            if(testCase.expectedCauseType == null) {
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
                assertEquals(testCase.expectedCauseMessage, ex.cause!!.message)
            }
        }
    }

    data class ParseFailureTestCase(val formatPattern: String,
                                    val timestampString: String,
                                    val expectedCauseType: Type? = null,
                                    val expectedCauseMessage: String? = null,
                                    val expectedErrorCode: ErrorCode = ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE)


    fun parametersForParseTimestampExceptionTest() = listOf(

        //Year outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd",
            "0000-01-01",
            DateTimeParseException::class.java,
            "Text '0000-01-01' could not be parsed: Invalid value for YearOfEra (valid values 1 - 999999999/1000000000): 0"),

        ParseFailureTestCase(
            "yyyyy-MM-dd",
            "10000-01-01",
            IllegalArgumentException::class.java,
            "Year 10000 must be between 1 and 9999 inclusive"),

        //Month outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-00-01",
            DateTimeParseException::class.java,
            "Text '2017-00-01' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 0"),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-13-01",
            DateTimeParseException::class.java,
            "Text '2017-13-01' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13"),

        //Day outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-01-00",
            DateTimeParseException::class.java,
            "Text '2017-01-00' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 0"),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-01-32",
            DateTimeParseException::class.java,
            "Text '2017-01-32' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 32"),

        //Hour outside of range (AM/PM)
        //ParseFailureTestCase("2017-01-01 00:01 PM", "yyyy-MM-dd hh:mm a", ""), //In 12 hour mode, 0 is considered 12...
        ParseFailureTestCase(
            "yyyy-MM-dd hh:mm a",
            "2017-01-01 13:01 PM",
            DateTimeParseException::class.java,
            "Text '2017-01-01 13:01 PM' could not be parsed: Invalid value for ClockHourOfAmPm (valid values 1 - 12): 13"),

        //Hour outside of range (24hr)
        ParseFailureTestCase(
            "yyyy-MM-dd HH:mm a",
            "2017-01-01 24:01 PM",
            DateTimeParseException::class.java,
            "Text '2017-01-01 24:01 PM' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24"),

        //Minute outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd hh:mm",
            "2017-01-01 01:60",
            DateTimeParseException::class.java,
            "Text '2017-01-01 01:60' could not be parsed: Invalid value for MinuteOfHour (valid values 0 - 59): 60"),

        //Second outside of range
        ParseFailureTestCase(
            "yyyy-MM-dd hh:mm:ss",
            "2017-01-01 01:01:60",
            DateTimeParseException::class.java,
            "Text '2017-01-01 01:01:60' could not be parsed: Invalid value for SecondOfMinute (valid values 0 - 59): 60"),

        //Whitespace surrounding custom timestamp
        ParseFailureTestCase(
            "yyyy-MM-dd",
            " 2017-01-01",
            DateTimeParseException::class.java,
            "Text ' 2017-01-01' could not be parsed at index 0"),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-01-01 ",
            DateTimeParseException::class.java,
            "Text '2017-01-01 ' could not be parsed, unparsed text found at index 10"),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            " 2017-01-01 ",
            DateTimeParseException::class.java,
            "Text ' 2017-01-01 ' could not be parsed at index 0"),

        ParseFailureTestCase(
            "yyyy-MM-dd",
            "2017-01-01 ",
            DateTimeParseException::class.java,
            "Text '2017-01-01 ' could not be parsed, unparsed text found at index 10"),

        //Required zero padding not present (Zero padding required because 2 or more consecutive format symbols)
        ParseFailureTestCase(
            "yyy M d H m s",  //a 3 digit year doesn't seem to make sense but the DateTimeFormatter allows it.
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 0"),

        ParseFailureTestCase(
            "yyyy M d H m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 0"),

        ParseFailureTestCase(
            "y MM d H m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 2"),

        ParseFailureTestCase(
            "y M dd H m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 4"),

        ParseFailureTestCase(
            "y M d HH m s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 6"),

        ParseFailureTestCase(
            "y M d H mm s",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 8"),

        ParseFailureTestCase(
            "y M d H m ss",
            "7 6 5 9 8 6",
            DateTimeParseException::class.java,
            "Text '7 6 5 9 8 6' could not be parsed at index 10"),

        //1 digit offset.  Ideally this would not be a failure case but they appear to have left 1 digit offsets
        //out of the JDK8 spec:  https://bugs.openjdk.java.net/browse/JDK-8066806
        ParseFailureTestCase(
            "y M d H m x",
            "1969 07 20 20 01 +2",
            DateTimeParseException::class.java,
            "Text '1969 07 20 20 01 +2' could not be parsed at index 17"),

        //Offset exceeds allowable range
        //Note:  Java's DateTimeFormatter only allows +/- 18h but IonJava's Timestamp allows +/- 23:59.
        ParseFailureTestCase(
            "y M d H m x",
            "1969 07 20 20 01 +2400",
            DateTimeParseException::class.java,
            "Text '1969 07 20 20 01 +2400' could not be parsed: Zone offset not in valid range: -18:00 to +18:00"),

        ParseFailureTestCase(
            "yyyy M d H m x",
            "1969 07 20 20 01 -2400",
            DateTimeParseException::class.java,
            "Text '1969 07 20 20 01 -2400' could not be parsed: Zone offset not in valid range: -18:00 to +18:00"),

        //Offset not ending on a minute boundary (error condition detected by TimestampParser)
        ParseFailureTestCase(
            "yyyy M d H m xxxxx",
            "1969 07 20 20 01 +01:00:01",
            expectedErrorCode = ErrorCode.EVALUATOR_PRECISION_LOSS_WHEN_PARSING_TIMESTAMP),

        //Three digit offset
        ParseFailureTestCase(
            "yyyy M d H m x",
            "1969 07 20 20 01 -240",
            DateTimeParseException::class.java,
            "Text '1969 07 20 20 01 -240' could not be parsed, unparsed text found at index 20"),

        // Ideally this fail while during the call to DateTimeFormatter.ofPattern() because the format pattern
        // specifies a 12-hour hour ('h') but doesn't also include an 'a' (AM/PM) symbol.  Unfortunately an exception
        // is not thrown by DateTimeFormatter but by the TemporalAccessor returned from `DateTimeFormatter.parse(...)
        // when an attempt is made to obtain the TemporalField.HOUR_OF_DAY field.  The result of this is that
        // we cannot easily distinguish between an invalid timestamp for the specified pattern and the aforementioned
        // scenario.  The ultimate impact of this is that the EvaluationException's error code will be
        // ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE instead of the more logical
        // EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN.  This is unfortunate but difficult to fix in a clean way.
        ParseFailureTestCase(
            "y M d h m",
            "1969 07 20 01 01",
            UnsupportedTemporalTypeException::class.java,
            "Unsupported field: HourOfDay"),

        // Similar to above, this *should* fail because the format pattern specifies a 24-hour hour ('H') *and* also
        // includes an 'a' (AM/PM) symbol.  The error code of the resulting EvaluationException should be
        // EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN, but is instead is EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE.
        // The most insidious part of this is that when the timestamp being parsed is in the AM, there is no failure
        // at all!  It will only throw an exception when it is in the PM!
        ParseFailureTestCase(
            "y M d H m a",
            "1969 07 20 01 01 PM",
            DateTimeParseException::class.java,
            "Text '1969 07 20 01 01 PM' could not be parsed: Conflict found: Field AmPmOfDay 0 differs from AmPmOfDay 1 derived from 01:01"),

        //Incomplete format patterns
        ParseFailureTestCase(
            "M-d", //No year
            "1-1",
            UnsupportedTemporalTypeException::class.java,
            "Unsupported field: Year"),

        ParseFailureTestCase(
            "y-d", //No month
            "1-1",
            UnsupportedTemporalTypeException::class.java,
            "Unsupported field: MonthOfYear"),

        ParseFailureTestCase(
            "y-M h:m", //No day
            "1-1 10:10",
            UnsupportedTemporalTypeException::class.java,
            "Unsupported field: DayOfMonth"),

        ParseFailureTestCase(
            "y-M-d m", //No hour
            "1-1-1 10",
            UnsupportedTemporalTypeException::class.java,
            "Unsupported field: HourOfDay"),

        ParseFailureTestCase(
            "y-M-d h:s", //No minute
            "1-1-1 10:10",
            UnsupportedTemporalTypeException::class.java,
            //You'd think this would be MinuteOfHour...
            "Unsupported field: HourOfDay"),

        ParseFailureTestCase(
            "y-M-d h:m.n", //No second
            "1-1-1 10:10.10",
            UnsupportedTemporalTypeException::class.java,
            //You'd think this would be MinuteOfHour...
            "Unsupported field: HourOfDay")
        )

    @Test
    @Parameters
    @TestCaseName
    fun invalidFormatPatternTest(testCase: InvalidFormatPatternTestCase) {
        try {
            TimestampParser.parseTimestamp("doesn't matter shouldn't get parsed anyway", testCase.pattern)
            fail("didn't throw")
        } catch(ex: EvaluationException) {
            assertEquals(ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN, ex.errorCode)

            if (testCase.expectedCauseMessage == null) {
                assertNull(ex.cause)
            } else {
                assertNotNull(ex.cause, "EvaluationException shouuld have a cause")
                val actualType: Type = (ex.cause as Exception).javaClass
                assertEquals(IllegalArgumentException::class.java, actualType)

                //Again, as above, we are only asserting the expected cause message here to be certain that
                //the test case is failing due to the intended reason.  The message is not part of the contract
                //we expose to the client.
                assertEquals(testCase.expectedCauseMessage, ex.cause!!.message)
            }
        }
    }

    data class InvalidFormatPatternTestCase(val pattern: String, val expectedCauseMessage: String? = null)

    fun parametersForInvalidFormatPatternTest() =
        listOf(
            InvalidFormatPatternTestCase(""),
            InvalidFormatPatternTestCase("asdf"),
            InvalidFormatPatternTestCase("yy-mm-dd-'Thh:mm", "Pattern ends with an incomplete string literal: yy-mm-dd-'Thh:mm"),
            InvalidFormatPatternTestCase("MMMMMM", "Too many pattern letters: M"),
            InvalidFormatPatternTestCase("ddd", "Too many pattern letters: d"),
            InvalidFormatPatternTestCase("m hhh", "Too many pattern letters: h"),
            InvalidFormatPatternTestCase("m HHH", "Too many pattern letters: H"),
            InvalidFormatPatternTestCase("mmm", "Too many pattern letters: m"),
            InvalidFormatPatternTestCase("sss", "Too many pattern letters: s"),
            InvalidFormatPatternTestCase("SSSSSSSSSS", "Minimum width must be from 0 to 9 inclusive but was 10")
        )
    //unterminated quote
}

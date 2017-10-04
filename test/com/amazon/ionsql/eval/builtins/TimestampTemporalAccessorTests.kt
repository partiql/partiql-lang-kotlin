package com.amazon.ionsql.eval.builtins


import com.amazon.ion.*
import junitparams.*
import junitparams.naming.*
import org.junit.*
import org.junit.runner.*
import java.time.format.*
import java.util.*
import kotlin.test.*
import org.assertj.core.api.Assertions.*
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

    /** All all the format symbols supported by java's DateTimeFormatter are listed here:
     * https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
     * We only support the symbols below.
     */
    fun parametersForFormatRandomTimesWithAllDateFormatSymbolsTest() : List<String> =
        listOf(
            "y", //Year of era, e.g. "1978"; "78"
            "M", //Month of year (1-12)
            "L", //Month of year e.g. "Jan"; "January"
            "d", //day of month (1-31)
            "a", //am-pm of day
            "h", //Clock hour of am-pm (1-12)
            "H", //hour of day (0-23)
            "m", //Minute of hour (0-59)
            "s", //Second of minute (0-59)

            //Note:  S and n both use ChronoField.NANO_OF_SECOND so we cannot remove support for one without
            //removing support for the other AFAIK.
            "S", //fraction of second, in milliseconds (0-999)
            "n", //Nano of second (0-999,999,999)

            //Note: Same with X, x O and Z for ChronoField.OFFSET_SECONDS
            "X", //Zone offset or Z for zero: e.g. "-08", "-0830", "-08:30", "-083000", "-08:30:00" Note: the seconds portion will always be "00" because Ion-Timestamp offset is specified in minutes
            "x", //Zone offset "+0000", "-08", "-0830", "-08:30", "-083000", "-08:30:00" Note: the seconds portion will always be "00" because Ion-Timestamp offset is specified in minutes
            "O", //Localized zone offset, e.g. "GMT+8", "GMT+08:00", "UTC-08:00";
            "Z"  //4 digit zone offset, e.g "+0000", "-0800", "-08:00"
         )

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


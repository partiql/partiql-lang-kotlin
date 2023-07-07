package org.partiql.lang.eval.builtins

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.datetime.TimestampTemporalAccessor
import org.partiql.value.datetime.DateTimeValue
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.format.DateTimeFormatter
import java.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(JUnitParamsRunner::class)
class TimestampTemporalAccessorTests {

    @Test
    fun timestampWithUnknownOffset() {
        // Note:  Ion spec allows representation of unknown offset with "0"
        val timestamp = DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(36L), null)
        assertNull(timestamp.timeZone)

        val temporalAccessor = TimestampTemporalAccessor(timestamp)
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
        val timestamp = DateTimeValue.timestamp(1969, 7, 20, 20, 18, BigDecimal.valueOf(36L), null)
        val temporalAccessor = TimestampTemporalAccessor(timestamp)
        val formatter = DateTimeFormatter.ofPattern(testCase.formatSymbol)

        assertThatThrownBy { formatter.format(temporalAccessor) }
            .isInstanceOf(testCase.expectedExceptionType)
    }

    /** Most of these format symbols are unsupported either because Ion's Timestamp doesn't store the information required
     * (i.e. timezone) or as a risk mitigation strategy that reduces the complexity should we need to completely replace
     * Java's DateTimeFormatter with something else later.
     */
    fun parametersForHandleUnsupportedFormatSymbolsTest(): List<UnsupportedSymbolTestCase> = listOf(
        UnsupportedSymbolTestCase("G", UnsupportedTemporalTypeException::class.java), // Era, e.g. "AD"
        UnsupportedSymbolTestCase("u", UnsupportedTemporalTypeException::class.java), // Year of era, e.g. "1978"; "78", (this is always positive, even for BC values)
        UnsupportedSymbolTestCase("Q", UnsupportedTemporalTypeException::class.java), // Quarter of year (1-4)
        UnsupportedSymbolTestCase("q", UnsupportedTemporalTypeException::class.java), // Quarter of year e.g. "Q3" "3rd quarter",
        UnsupportedSymbolTestCase("E", UnsupportedTemporalTypeException::class.java), // Day of week
        UnsupportedSymbolTestCase("F", UnsupportedTemporalTypeException::class.java), // Week of month
        UnsupportedSymbolTestCase("K", UnsupportedTemporalTypeException::class.java), // hour of am-pm (0-11)
        UnsupportedSymbolTestCase("k", UnsupportedTemporalTypeException::class.java), // clock of am-pm (1-24)
        UnsupportedSymbolTestCase("A", UnsupportedTemporalTypeException::class.java), // Millisecond of day (0-85,499,999)
        UnsupportedSymbolTestCase("N", UnsupportedTemporalTypeException::class.java), // Nano of day (0-85,499,999,999,999)
        UnsupportedSymbolTestCase("Y", UnsupportedTemporalTypeException::class.java), // Week based year
        UnsupportedSymbolTestCase("w", UnsupportedTemporalTypeException::class.java), // Week of week based year
        UnsupportedSymbolTestCase("W", UnsupportedTemporalTypeException::class.java), // Week of month
        UnsupportedSymbolTestCase("e", UnsupportedTemporalTypeException::class.java), // Localized day of week (number)
        UnsupportedSymbolTestCase("c", UnsupportedTemporalTypeException::class.java), // Localized day of week (week name, e.g. "Tue" or "Tuesday")
        UnsupportedSymbolTestCase("VV", DateTimeException::class.java), // time zone id, e.g "America/Los_Angeles; Z; -08:30" - ion timestamp does not know timezone, only offset
        UnsupportedSymbolTestCase("z", DateTimeException::class.java) // time zone name, e.g. "Pacific Standard Time" - ion timestamp does not know timezone, only offset
    )
}

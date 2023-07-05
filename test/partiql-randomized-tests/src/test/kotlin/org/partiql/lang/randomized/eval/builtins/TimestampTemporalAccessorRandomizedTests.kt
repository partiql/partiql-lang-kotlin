package org.partiql.lang.randomized.eval.builtins

import com.amazon.ion.Timestamp
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.datetime.TimestampTemporalAccessor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Random
import kotlin.test.assertEquals

@RunWith(JUnitParamsRunner::class)
class TimestampTemporalAccessorRandomizedTests {

    private val ITERATION_COUNT = 1000000

    private val TIMESTAMP_FORMAT_SYMBOLS: Set<Char> =
        setOf(
            'y', // Year of era, e.g. "1978"; "78"
            'M', // Month of year (1-12)
            'L', // Month of year e.g. "Jan"; "January"
            'd', // day of month (1-31)
            'a', // am-pm of day
            'h', // Clock hour of am-pm (1-12)
            'H', // hour of day (0-23)
            'm', // Minute of hour (0-59)
            's', // Second of minute (0-59)

            // Note:  S and n both use ChronoField.NANO_OF_SECOND so we cannot remove support for one without
            // removing support for the other AFAIK.
            'S', // fraction of second, in milliseconds (0-999)
            'n', // Nano of second (0-999,999,999)

            // Note: Same with X, x O and Z for ChronoField.OFFSET_SECONDS
            'X', // Zone offset or Z for zero: e.g. "-08", "-0830", "-08:30", "-083000", "-08:30:00" Note: the seconds portion will always be "00" because Ion-Timestamp offset is specified in minutes
            'x', // Zone offset "+0000", "-08", "-0830", "-08:30", "-083000", "-08:30:00" Note: the seconds portion will always be "00" because Ion-Timestamp offset is specified in minutes
            'O', // Localized zone offset, e.g. "GMT+8", "GMT+08:00", "UTC-08:00";
            'Z' // 4 digit zone offset, e.g "+0000", "-0800", "-08:00"
        )

    fun createRng(): Random {
        val rng = Random()
        val seed = rng.nextLong()
        println("Randomly generated seed is $seed.  Use this to reproduce failures in dev environment.")
        rng.setSeed(seed)
        return rng
    }

    @Test
    @Parameters
    @TestCaseName("formatRandomTimesWithSymbol_{0}")
    fun formatRandomTimesWithAllDateFormatSymbolsTest(formatSymbol: String) {
        System.out.println(
            String.format(
                "Generating %,d random dates, formatting each of them with \"%s\" comparing the result...",
                ITERATION_COUNT, formatSymbol
            )
        )
        val rng = createRng()
        val formatter = DateTimeFormatter.ofPattern(formatSymbol)
        (0..ITERATION_COUNT).toList().parallelStream().forEach { _ ->
            val timestamp = rng.nextTimestamp()
            // Expected
            val offsetDatetime = timestamp.toOffsetDateTime()
            val formattedOffsetDateTime = formatter.format(offsetDatetime)
            // Actual
            val temporalAccessor = TimestampTemporalAccessor(org.partiql.value.datetime.Timestamp.forIonTimestamp(timestamp))
            val formattedTimestamp = formatter.format(temporalAccessor)
            assertEquals(formattedOffsetDateTime, formattedTimestamp)
        }
    }

    fun parametersForFormatRandomTimesWithAllDateFormatSymbolsTest(): Set<Char> = TIMESTAMP_FORMAT_SYMBOLS
}

internal fun Timestamp.toOffsetDateTime() = OffsetDateTime.of(
    this.year,
    this.month,
    this.day,
    this.hour,
    this.minute,
    this.second,
    this.decimalSecond.rem(BigDecimal.valueOf(1L)).multiply(BigDecimal.valueOf(1000000000)).toInt(),
    java.time.ZoneOffset.ofTotalSeconds(this.localOffset * 60)
)

internal fun Random.nextTimestamp(): Timestamp {
    val year = Math.abs(this.nextInt() % 9999) + 1
    val month = Math.abs(this.nextInt() % 12) + 1

    // Determine last day of month for randomly generated month & year (e.g. 28, 29, 30 or 31)
    val maxDayOfMonth = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth

    val day = Math.abs(this.nextInt() % maxDayOfMonth) + 1
    val hour = Math.abs(this.nextInt() % 24)
    val minute = Math.abs(this.nextInt() % 60)

    val secondFraction = BigDecimal.valueOf(Math.abs(this.nextLong()) % 1000000000).div(BigDecimal.valueOf(1000000000L))
    val seconds = BigDecimal.valueOf(Math.abs(this.nextInt() % 59L)).add(secondFraction).abs()
    // Note:  need to % 59L above because 59L + secondFraction can yield 60 seconds

    var offsetMinutes = this.nextInt() % (18 * 60)

    // If the offset pushes this timestamp before 1/1/0001 then we will get IllegalArgumentException from
    // Timestamp.forSecond
    // NOTE:  the offset is *substracted* from the specified time!
    if (year == 1 && month == 1 && day == 1 && hour <= 18) {
        offsetMinutes = -Math.abs(offsetMinutes)
    }
    // Same if the offset can push this time stamp after 12/31/9999
    else if (year == 9999 && month == 12 && day == 31 && hour >= 6) {
        offsetMinutes = Math.abs(offsetMinutes)
    }
    return Timestamp.forSecond(year, month, day, hour, minute, seconds, offsetMinutes)
}

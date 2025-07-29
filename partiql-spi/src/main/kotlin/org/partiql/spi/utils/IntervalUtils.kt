package org.partiql.spi.utils

import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal object IntervalUtils {
    private const val NANO_PRECISION = 9
    private const val MONTHS_PER_YEAR = 12L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val HOURS_PER_DAY = 24L
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
    private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
    private const val NANOS_PER_SECOND = 1_000_000_000L

    /**
     * Divides an interval by a numeric value.
     * For YEAR_MONTH Interval, We maintain the integer part (precision 0) and truncate the floating part
     * For DAY_SECOND Interval, We maintain the precision of 9 for nanoseconds and truncate the additional part
     *
     * @param interval The interval type to divide
     * @return A function that takes an interval Datum and BigDecimal divisor, returns the divided interval
     */
    fun intervalDivide(interval: PType): ((Datum, BigDecimal) -> Datum) {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { i, number ->
                // get total months from the interval as long as it can hold the interval with precision = 9.
                val totalMonths: Long = i.years * MONTHS_PER_YEAR + i.months
                // use big decimal to do the calculation as we can control the round mode.
                // RoundingMode.DOWN mode is used to truncate fraction part e.g. 2.9 months will be 2 months.
                val totalMonthsInDecimal = BigDecimal(totalMonths).divide(number, 0, RoundingMode.DOWN)
                val years = (totalMonthsInDecimal.toLong() / MONTHS_PER_YEAR).toInt()
                val months = (totalMonthsInDecimal.toLong() % MONTHS_PER_YEAR).toInt()
                Datum.intervalYearMonth(years, months, interval.precision)
            }

            IntervalCode.DAY,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE,
            IntervalCode.MINUTE_SECOND,
            IntervalCode.SECOND -> { i, number ->
                val totalSecondsInBigDecimal = toSeconds(i)
                val resultInBigDecimal = totalSecondsInBigDecimal.divide(number, NANO_PRECISION, RoundingMode.DOWN)
                fromSecond(resultInBigDecimal, interval.precision, interval.fractionalPrecision)
            }

            else -> { num, i -> throw IllegalArgumentException("Unable to calculate division for INTERVAL expression") }
        }
    }

    /**
     * Multiplies an interval by a numeric value.
     * For YEAR_MONTH Interval, We maintain the integer part (precision 0) and truncate the floating part
     * For DAY_SECOND Interval, We maintain the precision of 9 for nanoseconds and truncate the additional part
     *
     * @param interval The interval type to multiply
     * @return A function that takes an interval Datum and BigDecimal multiplier, returns the multiplied interval
     */
    fun intervalMultiply(interval: PType): ((Datum, BigDecimal) -> Datum) {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { i, number ->
                // get total months from the interval as long as it can hold the interval with precision = 9.
                val totalMonths: Long = i.years * MONTHS_PER_YEAR + i.months
                // for interval multiply, fraction part is truncated when converting to Long type which is consistent
                // with RoundingMode.Down
                val totalMonthsInDecimal = BigDecimal(totalMonths).multiply(number)
                val years = (totalMonthsInDecimal.toLong() / MONTHS_PER_YEAR).toInt()
                val months = (totalMonthsInDecimal.toLong() % MONTHS_PER_YEAR).toInt()
                Datum.intervalYearMonth(years, months, interval.precision)
            }

            IntervalCode.DAY,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE,
            IntervalCode.MINUTE_SECOND,
            IntervalCode.SECOND -> { i, number ->
                val totalSecondsInBigDecimal = toSeconds(i)
                val resultInBigDecimal = totalSecondsInBigDecimal.multiply(number)
                fromSecond(resultInBigDecimal, interval.precision, interval.fractionalPrecision)
            }

            else -> { num, i -> throw IllegalArgumentException("Unable to calculate multiply for INTERVAL expression") }
        }
    }

    private fun toSeconds(i: Datum): BigDecimal {
        if (i.type.code() == PType.INTERVAL_DT) {
            val daysInSeconds = i.days * SECONDS_PER_DAY
            val hoursInSeconds = i.hours * SECONDS_PER_HOUR
            val minutesInSeconds = i.minutes * SECONDS_PER_MINUTE
            val totalSeconds = daysInSeconds + hoursInSeconds + minutesInSeconds + i.seconds
            return BigDecimal.valueOf(totalSeconds).add(BigDecimal.valueOf(i.nanos.toLong(), NANO_PRECISION))
        } else {
            throw UnsupportedOperationException("Unable to convert non DayToSeconds type to seconds")
        }
    }

    private fun fromSecond(totalSeconds: BigDecimal, precision: Int, fractionalPrecision: Int): Datum {
        val resultTotalNanos: BigInteger = totalSeconds.movePointRight(NANO_PRECISION).toBigIntegerExact()
        val divRem = resultTotalNanos.divideAndRemainder(BigInteger.valueOf(NANOS_PER_SECOND))
        val resultTotalSeconds = divRem[0].toLong()

        val days = (resultTotalSeconds / SECONDS_PER_DAY).toInt()
        val remainingSeconds = resultTotalSeconds % SECONDS_PER_DAY

        val hours = (remainingSeconds / SECONDS_PER_HOUR).toInt()
        val remainingSecondsAfterHours = remainingSeconds % SECONDS_PER_HOUR

        val minutes = (remainingSecondsAfterHours / SECONDS_PER_MINUTE).toInt()
        val seconds = remainingSecondsAfterHours % SECONDS_PER_MINUTE

        return Datum.intervalDaySecond(
            days,
            hours,
            minutes,
            seconds.toInt(),
            divRem[1].toInt(),
            precision,
            fractionalPrecision
        )
    }
}

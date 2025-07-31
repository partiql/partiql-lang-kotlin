package org.partiql.spi.utils

import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal object IntervalUtils {
    // According to SQL1992, whether to truncate or round in the least significant
    // field of the result is implementation-defined. We implemented as truncation for Interval as most database systems do.
    private val INTERVAL_ROUNDING_MODE = RoundingMode.DOWN

    private const val INTERVAL_DEFAULT_FRACTIONAL_PRECISION = 6
    private const val NANO_MAX_PRECISION = 9
    private const val MONTHS_PER_YEAR = 12L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val HOURS_PER_DAY = 24L
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
    private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
    private const val NANOS_PER_SECOND = 1_000_000_000L

    /**
     * Divides an interval by a numeric value.
     *
     * @param interval The interval type to divide
     * @return A function that takes an interval Datum and BigDecimal divisor, returns the divided interval
     */
    fun intervalDivide(interval: PType): ((Datum, Number) -> Datum) {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { i, number ->
                // get total months from the interval as Long Type as it can hold the interval with max precision = 9.
                val totalMonths: Long = i.years * MONTHS_PER_YEAR + i.months
                val totalMonthsInDouble = totalMonths / number.toDouble()
                val years = (totalMonthsInDouble.toLong() / MONTHS_PER_YEAR).toInt()
                val months = (totalMonthsInDouble.toLong() % MONTHS_PER_YEAR).toInt()
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
                // When float/double is converted to BigDecimal, NumberUtils.bigDecimalOf will introduce inaccuracy for double due to the nature of approximate type
                // For example, 0.1 could be 0.100000000000000111 which leads to computing inaccuracy.
                val numberInBigDecimal = NumberUtils.bigDecimalOf(number)
                val totalSecondsInBigDecimal = toSeconds(i)
                val resultInBigDecimal = totalSecondsInBigDecimal.divide(numberInBigDecimal, NANO_MAX_PRECISION, INTERVAL_ROUNDING_MODE)
                fromSecond(resultInBigDecimal, interval.precision, getFractionPrecision(interval))
            }

            else -> throw IllegalArgumentException("Unable to calculate division for INTERVAL expression")
        }
    }

    /**
     * Multiplies an interval by a numeric value.
     *
     * @param interval The interval type to multiply
     * @return A function that takes an interval Datum and BigDecimal multiplier, returns the multiplied interval
     */
    fun intervalMultiply(interval: PType): ((Datum, Number) -> Datum) {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { i, number ->
                // get total months from the interval as Long Type as it can hold the interval with max precision = 9.
                val totalMonths: Long = i.years * MONTHS_PER_YEAR + i.months
                val totalMonthsInDouble = totalMonths * number.toDouble()
                val years = (totalMonthsInDouble / MONTHS_PER_YEAR).toInt()
                val months = (totalMonthsInDouble % MONTHS_PER_YEAR).toInt()
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
                // When float/double is converted to BigDecimal, NumberUtils.bigDecimalOf will introduce inaccuracy for double due to the nature of approximate type
                // For example, 0.1 could be 0.100000000000000111 which leads to computing inaccuracy.
                val numberInBigDecimal = BigDecimal(number.toString())
                val totalSecondsInBigDecimal = toSeconds(i)
                val resultInBigDecimal = totalSecondsInBigDecimal.multiply(numberInBigDecimal)
                fromSecond(resultInBigDecimal, interval.precision, getFractionPrecision(interval))
            }

            else -> throw IllegalArgumentException("Unable to calculate multiply for INTERVAL expression")
        }
    }

    private fun toSeconds(i: Datum): BigDecimal {
        if (i.type.code() == PType.INTERVAL_DT) {
            val daysInSeconds = i.days * SECONDS_PER_DAY
            val hoursInSeconds = i.hours * SECONDS_PER_HOUR
            val minutesInSeconds = i.minutes * SECONDS_PER_MINUTE
            val totalSeconds = daysInSeconds + hoursInSeconds + minutesInSeconds + i.seconds
            return BigDecimal.valueOf(totalSeconds).add(BigDecimal.valueOf(i.nanos.toLong(), NANO_MAX_PRECISION))
        } else {
            throw UnsupportedOperationException("Unable to convert non DayToSeconds type to seconds")
        }
    }

    private fun fromSecond(totalSeconds: BigDecimal, precision: Int, fractionalPrecision: Int): Datum {
        val resultTotalNanos: BigInteger = totalSeconds.movePointRight(NANO_MAX_PRECISION).toBigIntegerExact()
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

    private fun getFractionPrecision(interval: PType): Int{
        return when(interval.intervalCode){
            IntervalCode.SECOND,
            IntervalCode.MINUTE_SECOND,
            IntervalCode.HOUR_SECOND,
            IntervalCode.DAY_SECOND -> interval.fractionalPrecision
            IntervalCode.DAY,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.HOUR,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.MINUTE -> INTERVAL_DEFAULT_FRACTIONAL_PRECISION
            else -> throw IllegalArgumentException("Cannot get fraction precision for Non-INTERVAL_DT type")
        }
    }
}

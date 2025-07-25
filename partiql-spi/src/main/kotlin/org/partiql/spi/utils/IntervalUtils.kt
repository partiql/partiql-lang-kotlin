package org.partiql.spi.utils

import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal object IntervalUtils {
    private const val MONTHS_PER_YEAR = 12
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
    private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
    private const val NANOS_PER_SECOND = 1_000_000_000

    fun intervalDivide(interval: PType): ((Datum, BigDecimal) -> Datum)? {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { i, number ->
                val totalMonths = i.years * MONTHS_PER_YEAR + i.months
                // Double is good to avoid calculation inaccuracy as we truncate fraction part
                val totalMonthsInDecimal = BigDecimal(totalMonths).divide(number, RoundingMode.DOWN)
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
                val resultInBigDecimal = totalSecondsInBigDecimal.divide(number)
                fromSecond(resultInBigDecimal, interval.precision, interval.fractionalPrecision)
            }

            else -> { num, i -> Datum.missing() }
        }
    }

    fun intervalMultiply(interval: PType): ((Datum, BigDecimal) -> Datum)? {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { i, number ->
                val totalMonths = i.years * MONTHS_PER_YEAR + i.months
                // Double is good to avoid calculation inaccuracy as we truncate fraction part
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
                val totalSecondsInBigDecimal = toSeconds(i)
                val resultInBigDecimal = totalSecondsInBigDecimal * number
                fromSecond(resultInBigDecimal, interval.precision, interval.fractionalPrecision)
            }

            else -> { num, i -> Datum.missing() }
        }
    }

    private fun toSeconds(i: Datum): BigDecimal {
        if (i.type.code() == PType.INTERVAL_DT) {
            val daysInSeconds = i.days * SECONDS_PER_DAY.toLong()
            val hoursInSeconds = i.hours * SECONDS_PER_HOUR.toLong()
            val minutesInSeconds = i.minutes * SECONDS_PER_MINUTE.toLong()
            val totalSeconds = daysInSeconds + hoursInSeconds + minutesInSeconds + i.seconds
            return BigDecimal.valueOf(totalSeconds).add(BigDecimal.valueOf(i.nanos.toLong(), 9))
        } else {
            throw UnsupportedOperationException("Unable to convert non DayToSeconds type")
        }
    }

    private fun fromSecond(totalSeconds: BigDecimal, precision: Int, fractionalPrecision: Int): Datum {
        val resultTotalNanos: BigInteger = totalSeconds.movePointRight(9).toBigIntegerExact()
        val divRem = resultTotalNanos.divideAndRemainder(BigInteger.valueOf(NANOS_PER_SECOND.toLong()))
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

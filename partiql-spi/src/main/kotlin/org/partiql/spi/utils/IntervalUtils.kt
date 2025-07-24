package org.partiql.spi.utils

import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object  IntervalUtils {
    private const val MONTHS_PER_YEAR = 12
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
    private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
    private const val NANOS_PER_SECOND = 1000000000

    fun getIntervalWithFactor(interval: PType): ((Double, Datum) -> Datum)? {
        return when (interval.intervalCode) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> { number, i ->
                val totalMonths = i.years * MONTHS_PER_YEAR + i.months
                val totalMonthsInDouble = totalMonths * number
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
            IntervalCode.SECOND -> { number, i ->
                    val daysInSeconds = i.days * SECONDS_PER_DAY.toLong()
                    val hoursInSeconds = i.hours * SECONDS_PER_HOUR.toLong()
                    val minutesInSeconds = i.minutes * SECONDS_PER_MINUTE.toLong()
                    val totalSeconds: Long = daysInSeconds + hoursInSeconds + minutesInSeconds + i.seconds

                    val totalSecondsInDouble = totalSeconds * number
                    val days = (totalSecondsInDouble / SECONDS_PER_DAY).toInt()
                    val remainingSeconds = totalSecondsInDouble % SECONDS_PER_DAY

                    val hours = (remainingSeconds / SECONDS_PER_HOUR).toInt()
                    val remainingSecondsAfterHours = remainingSeconds % SECONDS_PER_HOUR

                    val minutes = (remainingSecondsAfterHours / SECONDS_PER_MINUTE).toInt()
                    val seconds = remainingSecondsAfterHours % SECONDS_PER_MINUTE

                    val nanos = (seconds % 1) * NANOS_PER_SECOND

                    Datum.intervalDaySecond(days, hours, minutes, seconds.toInt(), nanos.toInt(), interval.precision, interval.fractionalPrecision)
            }
            else -> { num, i -> Datum.missing() }
        }
    }
}
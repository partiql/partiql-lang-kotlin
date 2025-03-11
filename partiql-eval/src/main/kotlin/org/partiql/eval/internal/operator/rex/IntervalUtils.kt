package org.partiql.eval.internal.operator.rex

import org.partiql.spi.types.IntervalCode
import org.partiql.spi.value.Datum

internal object IntervalUtils {

    private const val MONTHS_PER_YEAR = 12
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
    private const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY

    fun getIntervalDateTime(
        days: Int,
        hours: Int,
        minutes: Int,
        seconds: Int,
        nanos: Int,
        precision: Int,
        scale: Int,
        intervalCode: Int
    ): Datum {
        // Get Total Seconds
        val daysInSeconds = days * SECONDS_PER_DAY.toLong()
        val hoursInSeconds = hours * SECONDS_PER_HOUR.toLong()
        val minutesInSeconds = minutes * SECONDS_PER_MINUTE.toLong()
        val totalSeconds: Long = daysInSeconds + hoursInSeconds + minutesInSeconds + seconds

        // Normalize to Specific Precision
        return when (intervalCode) {
            IntervalCode.DAY -> Datum.intervalDay((totalSeconds / SECONDS_PER_DAY).toInt(), precision)
            IntervalCode.HOUR -> Datum.intervalHour((totalSeconds / SECONDS_PER_HOUR).toInt(), precision)
            IntervalCode.MINUTE -> Datum.intervalMinute((totalSeconds / SECONDS_PER_MINUTE).toInt(), precision)
            IntervalCode.SECOND -> Datum.intervalSecond(totalSeconds.toInt(), nanos, precision, scale)
            IntervalCode.DAY_HOUR -> {
                Datum.intervalDayHour(
                    (totalSeconds / SECONDS_PER_DAY).toInt(),
                    (totalSeconds % SECONDS_PER_DAY / SECONDS_PER_HOUR).toInt(),
                    precision
                )
            }
            IntervalCode.DAY_MINUTE -> {
                val minutesTotal = totalSeconds / SECONDS_PER_MINUTE
                val minutesLeftOver = (minutesTotal % MINUTES_PER_HOUR).toInt()
                val hoursTotal = minutesTotal / MINUTES_PER_HOUR
                val hoursLeftOver = (minutesTotal % MINUTES_PER_HOUR).toInt()
                val daysTotal = (hoursTotal / HOURS_PER_DAY).toInt()
                Datum.intervalDayMinute(daysTotal, hoursLeftOver, minutesLeftOver, precision)
            }
            IntervalCode.DAY_SECOND -> {
                val secondsLeftOver = (totalSeconds % SECONDS_PER_MINUTE).toInt()
                val minutes = totalSeconds / SECONDS_PER_MINUTE
                val minutesLeftOver = (minutes % MINUTES_PER_HOUR).toInt()
                val hours = minutes / MINUTES_PER_HOUR
                val hoursLeftOver = (minutes % MINUTES_PER_HOUR).toInt()
                val days = (hours / HOURS_PER_DAY).toInt()
                Datum.intervalDaySecond(days, hoursLeftOver, minutesLeftOver, secondsLeftOver, nanos, precision, scale)
            }
            IntervalCode.HOUR_MINUTE -> {
                val minutesLeftOver = (totalSeconds % SECONDS_PER_MINUTE).toInt()
                val hours = totalSeconds / SECONDS_PER_HOUR
                Datum.intervalHourMinute(hours.toInt(), minutesLeftOver, precision)
            }
            IntervalCode.HOUR_SECOND -> {
                val secondsLeftOver = (totalSeconds % SECONDS_PER_MINUTE).toInt()
                val minutes = totalSeconds / SECONDS_PER_MINUTE
                val minutesLeftOver = (minutes % MINUTES_PER_HOUR).toInt()
                val hours = minutes / MINUTES_PER_HOUR
                Datum.intervalHourSecond(hours.toInt(), minutesLeftOver, secondsLeftOver, nanos, precision, scale)
            }
            IntervalCode.MINUTE_SECOND -> {
                val secondsLeftOver = (totalSeconds % SECONDS_PER_MINUTE).toInt()
                val minutes = totalSeconds / SECONDS_PER_MINUTE
                Datum.intervalMinuteSecond(minutes.toInt(), secondsLeftOver, nanos, precision, scale)
            }
            else -> throw IllegalArgumentException("Invalid interval code: $intervalCode")
        }
    }

    fun getIntervalYearMonth(
        years: Int,
        months: Int,
        precision: Int,
        intervalCode: Int
    ): Datum {
        // Get Total Months
        val totalMonths: Long = (years * MONTHS_PER_YEAR.toLong()) + months
        return when (intervalCode) {
            IntervalCode.YEAR -> Datum.intervalYear((totalMonths / MONTHS_PER_YEAR).toInt(), precision)
            IntervalCode.MONTH -> Datum.intervalMonth(totalMonths.toInt(), precision)
            IntervalCode.YEAR_MONTH -> {
                val yearsTotal = (totalMonths / MONTHS_PER_YEAR).toInt()
                val monthsTotal = (totalMonths % MONTHS_PER_YEAR).toInt()
                Datum.intervalYearMonth(yearsTotal, monthsTotal, precision)
            }
            else -> throw IllegalArgumentException("Invalid interval code: $intervalCode")
        }
    }
}

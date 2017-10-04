package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import java.math.*
import java.time.temporal.*

private val NANOS_PER_SECOND = 1_000_000_000L
private val MILLIS_PER_SECOND = 1_000L
private val MILLIS_PER_SECOND_BD = BigDecimal.valueOf(MILLIS_PER_SECOND)
private val NANOS_PER_SECOND_BD = BigDecimal.valueOf(NANOS_PER_SECOND)

private val Timestamp.nanoOfSecond: Long get() = this.decimalSecond.multiply(NANOS_PER_SECOND_BD).toLong() % NANOS_PER_SECOND

private val Timestamp.milliOfSecond: Long get() = this.decimalSecond.multiply(MILLIS_PER_SECOND_BD).toLong() % MILLIS_PER_SECOND

internal class TimestampTemporalAccessor(val ts: Timestamp) : TemporalAccessor {

    /**
     * This method should return true to indicate whether a given TemporalField is supported.
     * Note that the date-time formatting functionality in JDK8 assumes that all ChronoFields are supported and
     * doesn't invoke this method to check if a ChronoField is supported.
     */
    override fun isSupported(field: TemporalField?): Boolean =
            when (field) {
                IsoFields.QUARTER_OF_YEAR -> true
                else -> false
            }

    override fun getLong(field: TemporalField?): Long {
        if(field == null) {
            throw IllegalArgumentException("argument 'field' may not be null")
        }
        return when (field) {
            ChronoField.YEAR_OF_ERA -> ts.year.toLong()
            ChronoField.MONTH_OF_YEAR -> ts.month.toLong()
            ChronoField.DAY_OF_MONTH -> ts.day.toLong()
            ChronoField.HOUR_OF_DAY -> ts.hour.toLong()
            ChronoField.SECOND_OF_MINUTE -> ts.second.toLong()
            ChronoField.MINUTE_OF_HOUR -> ts.minute.toLong()
            ChronoField.MILLI_OF_SECOND -> ts.milliOfSecond
            ChronoField.NANO_OF_SECOND -> ts.nanoOfSecond

            ChronoField.AMPM_OF_DAY -> ts.hour / 12L
            ChronoField.CLOCK_HOUR_OF_AMPM -> {
                val hourOfAmPm = ts.hour.toLong() % 12L
                if(hourOfAmPm == 0L) 12 else hourOfAmPm
            }
            ChronoField.OFFSET_SECONDS -> if(ts.localOffset == null) 0 else ts.localOffset * 60L
            else -> throw UnsupportedTemporalTypeException(
                    field.javaClass.name + "." + field.toString() + " not supported")
        }
    }
}
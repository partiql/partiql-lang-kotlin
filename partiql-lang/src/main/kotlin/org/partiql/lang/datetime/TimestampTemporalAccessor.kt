package org.partiql.lang.datetime

import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalField
import java.time.temporal.UnsupportedTemporalTypeException

private val NANOS_PER_SECOND = 1_000_000_000L
private val MILLIS_PER_SECOND = 1_000L
private val MILLIS_PER_SECOND_BD = BigDecimal.valueOf(MILLIS_PER_SECOND)
private val NANOS_PER_SECOND_BD = BigDecimal.valueOf(NANOS_PER_SECOND)

private val Timestamp.nanoOfSecond: Long get() = this.epochSecond.multiply(NANOS_PER_SECOND_BD).toLong() % NANOS_PER_SECOND

private val Timestamp.milliOfSecond: Long get() = this.epochSecond.multiply(MILLIS_PER_SECOND_BD).toLong() % MILLIS_PER_SECOND

/**
 * This is a workaround to identify the timestamp has no timezone field,
 * if this is thrown, it must be the formatter is trying to access the timezone field
 * but the timestamp value is of type timestamp without time zone.
 * In which case we need to convert this to timestamp with timezone using session timezone.
 */
internal class NoTimeZoneException : Exception()
internal class TimestampTemporalAccessor(private val ts: Timestamp) : TemporalAccessor {

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
        if (field == null) {
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
                if (hourOfAmPm == 0L) 12 else hourOfAmPm
            }
            ChronoField.OFFSET_SECONDS -> when (val timezone = this.ts.timeZone) {
                TimeZone.UnknownTimeZone -> 0
                is TimeZone.UtcOffset -> timezone.totalOffsetMinutes * 60L
                null -> throw NoTimeZoneException()
            }
            else -> throw UnsupportedTemporalTypeException(
                field.javaClass.name + "." + field.toString() + " not supported"
            )
        }
    }
}

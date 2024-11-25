package org.partiql.spi.datetime

import org.partiql.spi.datetime.util.DatetimeUtil.toBigDecimal
import org.partiql.spi.datetime.impl.OffsetTimestampLowPrecision
import org.partiql.spi.datetime.util.DatetimeComparisons
import org.partiql.spi.datetime.util.DatetimePrecisionUtil
import java.math.BigDecimal

/**
 * Superclass for all implementations representing time value.
 * See [TimestampWithTimeZone] and [TimestampWithoutTimeZone]
 */
public sealed interface Timestamp : Datetime, Comparable<Timestamp> {

    public override val year: Int
    public override val month: Int
    public override val day: Int
    public override val hour: Int
    public override val minute: Int
    public override val decimalSecond: BigDecimal
    public override val timeZone: Timezone?

    // Operation
    /**
     * Returns a [Timestamp] value with the specified number of years added.
     * [years] can be negative.
     */
    public fun plusYears(years: Long): Timestamp

    /**
     * Returns a [Timestamp] value with the specified number of years added.
     * Year fields may be changed to ensure the correctness of the result.
     * [months] can be negative.
     */
    public fun plusMonths(months: Long): Timestamp

    /**
     * Returns a [Timestamp] value with the specified number of days added.
     * Year, Month fields may be changed to ensure the correctness of the result.
     * [days] can be negative.
     */
    public fun plusDays(days: Long): Timestamp

    /**
     * Returns a [Timestamp] value with the specified number of hours added.
     * Year, Month, Day fields may be changed to ensure the correctness of the result.
     * [hours] can be negative.
     */
    public fun plusHours(hours: Long): Timestamp

    /**
     * Returns a [Timestamp] value with the specified number of minutes added.
     * Year, Month, Day, Hour fields may be changed to ensure the correctness of the result.
     * [minutes] can be negative.
     */
    public fun plusMinutes(minutes: Long): Timestamp

    /**
     * Returns a [Timestamp] value with the specified number of minutes added.
     * Year, Month, Day, Hour, Minute fields may be changed to ensure the correctness of the result.
     * [seconds] can be negative.
     */
    public fun plusSeconds(seconds: BigDecimal): Timestamp

    /**
     * Convenient wrapper for entering long value as [seconds]
     */
    public fun plusSeconds(seconds: Long): Timestamp

    /**
     * Returns a [Timestamp] value with [precision] number of digits in second fraction.
     */
    public fun toPrecision(precision: Int): Timestamp

    /**
     * Returns a [Date] value by extracting [year], [month], [day] field
     */
    public fun toDate(): Date = DateTimeValue.date(this.year, this.month, this.day)

    /**
     * Returns a [Time] value by extracting [hour], [minute], [decimalSecond], and [timeZone].
     */
    public fun toTime(): Time

    /**
     * Comparison method for timestamp value
     * If both value are [TimestampWithTimeZone]:
     *    The two value are considered equivalent if they refer to the same point in time.
     * If both value are timestamp without timezone:
     *      The two value are consider equivalent if all the fields (exclude precision) are equivalent.
     *      Another way to interpret this is, two timestamp without timezone are equivalent
     *          if and only if they are equivalent when converted to the same time zone.
     * If one value is timestamp with time zone and the other is timestamp without time zone:
     *      Error.
     *      One may not directly compare a timestamp value with timezone to a timestamp value without time zone.
     */
    public override fun compareTo(other: Timestamp): Int =
        DatetimeComparisons.compareTo(this, other)
}

/**
 * Superclass for all implementations representing timestamp with time zone value.
 * A [TimestampWithTimeZone] is a instant in the timeline.
 */
public abstract class TimestampWithTimeZone : Timestamp {
    public abstract override val timeZone: Timezone

    public abstract val ionRaw: com.amazon.ion.Timestamp?

    /**
     * Returns an Ion Equivalent Timestamp
     * [ionTimestampValue] takes care of the precision,
     * the ion representation will have exact precision of the backing partiQL value.
     * TODO: Move this to serde package.
     */
    @get:Throws(DateTimeException::class)
    public val ionTimestampValue: com.amazon.ion.Timestamp by lazy {
        when (val timeZone = this.timeZone) {
            Timezone.UnknownTimeZone -> com.amazon.ion.Timestamp.forSecond(
                this.year, this.month, this.day,
                this.hour, this.minute, this.decimalSecond,
                com.amazon.ion.Timestamp.UNKNOWN_OFFSET
            )

            is Timezone.UtcOffset -> com.amazon.ion.Timestamp.forSecond(
                this.year, this.month, this.day,
                this.hour, this.minute, this.decimalSecond,
                timeZone.totalOffsetMinutes
            )
        }
    }

    /**
     * Returns a BigDecimal representing the instant in time that is
     * the number of Seconds (*including* any fractional Seconds) from the epoch.
     * If the instant is before 1970-01-01T00:00:00Z, then the result will be negative.
     */
    public abstract val epochSecond: BigDecimal

    public val epochMillis: BigDecimal by lazy {
        epochSecond.movePointRight(3)
    }

    public abstract override fun plusYears(years: Long): TimestampWithTimeZone
    public abstract override fun plusMonths(months: Long): TimestampWithTimeZone
    public abstract override fun plusDays(days: Long): TimestampWithTimeZone
    public abstract override fun plusHours(hours: Long): TimestampWithTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimestampWithTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimestampWithTimeZone
    public final override fun plusSeconds(seconds: Long): TimestampWithTimeZone =
        plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimestampWithTimeZone =
        DatetimePrecisionUtil.toPrecision(precision, this) as TimestampWithTimeZone

    /**
     * Returns a [TimestampWithoutTimeZone] object, assuming the local time is in [timeZone].
     */
    public abstract fun toTimeWithoutTimeZone(timeZone: Timezone): TimestampWithoutTimeZone

    /**
     * Returns a [TimestampWithTimeZone] object, assuming the desiredZone is [timeZone]
     * The intention of this function is to get the same instant of time in the desired zone.
     */
    public abstract fun atTimeZone(timeZone: Timezone): TimestampWithTimeZone
    public final override fun toTime(): TimeWithTimeZone =
        DateTimeValue.time(hour, minute, decimalSecond, timeZone) as TimeWithTimeZone

    public final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as TimestampWithTimeZone
        if (this.year != other.year) return false
        if (this.month != other.month) return false
        if (this.day != other.day) return false
        if (this.hour != other.hour) return false
        if (this.minute != other.minute) return false
        if (this.decimalSecond != other.decimalSecond) return false
        if (this.timeZone != other.timeZone) return false
        return true
    }

    public final override fun hashCode(): Int =
        year.hashCode() + month.hashCode() + day.hashCode() + hour.hashCode() + minute.hashCode() + decimalSecond.hashCode() + timeZone.hashCode()

    public final override fun toString(): String =
        "${this.javaClass.simpleName}(year=$year, month=$month, day=$day, hour=$hour, minute=$minute, second=$decimalSecond, timeZone=$timeZone)"

    public companion object {

        @JvmStatic
        public fun nowZ(): TimestampWithTimeZone = OffsetTimestampLowPrecision.nowZ()
    }
}

/**
 * Superclass for all implementations representing timestamp without time zone value.
 * A [TimestampWithoutTimeZone] is not an instant in the timeline, the same value may refer to different instant in timeline at different time zone.
 */
public abstract class TimestampWithoutTimeZone : Timestamp {
    public override val timeZone: Timezone? = null
    public abstract override fun plusYears(years: Long): TimestampWithoutTimeZone
    public abstract override fun plusMonths(months: Long): TimestampWithoutTimeZone
    public abstract override fun plusDays(days: Long): TimestampWithoutTimeZone
    public abstract override fun plusHours(hours: Long): TimestampWithoutTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimestampWithoutTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimestampWithoutTimeZone
    public override fun plusSeconds(seconds: Long): TimestampWithoutTimeZone =
        plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimestampWithoutTimeZone =
        DatetimePrecisionUtil.toPrecision(precision, this) as TimestampWithoutTimeZone

    /**
     * Returns a [TimestampWithTimeZone] object, assuming the current [TimestampWithoutTimeZone] value is in [timeZone].
     * The intention of this function is to append time zone information.
     */
    public abstract fun withTimeZone(timeZone: Timezone): TimestampWithTimeZone
    public final override fun toTime(): TimeWithoutTimeZone =
        DateTimeValue.time(this.hour, this.minute, this.decimalSecond) as TimeWithoutTimeZone
    public final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as TimestampWithoutTimeZone
        if (this.year != other.year) return false
        if (this.month != other.month) return false
        if (this.day != other.day) return false
        if (this.hour != other.hour) return false
        if (this.minute != other.minute) return false
        if (this.decimalSecond != other.decimalSecond) return false
        return true
    }

    public final override fun hashCode(): Int =
        year.hashCode() + month.hashCode() + day.hashCode() + hour.hashCode() + minute.hashCode() + decimalSecond.hashCode()

    public final override fun toString(): String =
        "${this.javaClass.simpleName}(year=$year, month=$month, day=$day, hour=$hour, minute=$minute, second=$decimalSecond)"
}
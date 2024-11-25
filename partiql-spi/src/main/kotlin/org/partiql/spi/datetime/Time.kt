package org.partiql.spi.datetime

import org.partiql.spi.datetime.util.DatetimeComparisons
import org.partiql.spi.datetime.util.DatetimePrecisionUtil
import org.partiql.spi.datetime.util.DatetimeUtil.toBigDecimal
import java.math.BigDecimal

/**
 * Superclass for all implementations representing time value.
 * See [TimeWithTimeZone] and [TimeWithoutTimeZone]
 */
public sealed interface Time : Datetime, Comparable<Time> {
    /**
     * Year field for [Time] is always null
     */
    public override val year: Int?
        get() = null

    /**
     * Month field for [Time] is always null
     */
    public override val month: Int?
        get() = null

    /**
     * Day field for [Time] is always null
     */
    public override val day: Int?
        get() = null

    /**
     * Hour as it would appear on local clock
     */
    public override val hour: Int

    /**
     * Minute as it would appear on local clock
     */
    public override val minute: Int

    /**
     * Second as it would appear on local clock
     *
     * Fraction of second is included and support arbitrary digits up to system limit
     */
    public override val decimalSecond: BigDecimal

    /**
     * Null if the value is a [TimeWithoutTimeZone]
     */
    public override val timeZone: Timezone?

    /**
     * Duration of time in second between midnight (00:00:00) to the given time
     * It only depends on clock time (i.e., hour, minutes, time field).
     */
    public val elapsedSecond: BigDecimal

    // Operation
    /**
     * Returns a [Time] value with the specified number of hours added.
     * The calculation wraps around midnight.
     * [hours] can be negative.
     * This operation only depends on clock time.
     */
    public fun plusHours(hours: Long): Time

    /**
     * Returns a [Time] value with the specified number of hours added.
     * The calculation wraps around midnight.
     * Hour Field may change as necessary to ensure a valid result.
     * [minutes] can be negative.
     * This operation only depends on clock time.
     */
    public fun plusMinutes(minutes: Long): Time

    /**
     * Returns a [Time] value with the specified number of hours added.
     * The calculation wraps around midnight.
     * Hour and Minute Field may change as necessary to ensure a valid result.
     * [seconds] can be negative.
     * This operation only depends on clock time.
     */
    public fun plusSeconds(seconds: BigDecimal): Time

    /**
     * Convenient wrapper that allows to enter a long value as second quantity
     */
    public fun plusSeconds(seconds: Long): Time

    /**
     * Returns a [Time] object with [precision] number of digits in second fraction.
     * This operation only depends on clock time.
     */
    public fun toPrecision(precision: Int): Time

    /**
     * Returns a [Timestamp] object.
     * The year, month, day fields comes from the [date] parameter,
     * The hour, minute, second, and timezone fields comes from the time object.
     */
    public fun atDate(date: Date): Timestamp

    /**
     * Compares two [Time] value.
     * 1. If both are [TimeWithTimeZone], then it is to convert them to their respective UTC equivalent (normalizing to +00:00),
     *   then comparing each of the field.
     * 2. If both are [TimeWithoutTimeZone], then we compare each of the field in order.
     */
    public override fun compareTo(other: Time): Int =
        DatetimeComparisons.compareTo(this, other)
}

/**
 * Superclass for all implementations representing a time with time zone value.
 * Informally, [TimeWithTimeZone] represents an orientation of a clock attached with timezone offset information.
 */
public abstract class TimeWithTimeZone : Time {
    public abstract override val timeZone: Timezone
    public abstract override fun plusHours(hours: Long): TimeWithTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimeWithTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimeWithTimeZone
    public final override fun plusSeconds(seconds: Long): Time = plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimeWithTimeZone =
        DatetimePrecisionUtil.toPrecision(precision, this) as TimeWithTimeZone
    public abstract override fun atDate(date: Date): TimestampWithTimeZone

    /**
     * Returns a [TimeWithoutTimeZone] object, assuming the local time is in [timeZone].
     * The intention of the function is to get a clock time in the [timeZone].
     */
    public abstract fun toTimeWithoutTimeZone(timeZone: Timezone): TimeWithoutTimeZone

    /**
     * Returns a [TimeWithTimeZone] object, assuming the desiredZone is [timeZone]
     * The intention of this function is to get the same instant of time in the desired zone.
     */
    public abstract fun atTimeZone(timeZone: Timezone): TimeWithTimeZone
    public final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as TimeWithTimeZone
        if (this.hour != other.hour) return false
        if (this.minute != other.minute) return false
        if (this.decimalSecond != other.decimalSecond) return false
        if (this.timeZone != other.timeZone) return false
        return true
    }
    public final override fun hashCode(): Int =
        hour.hashCode() + minute.hashCode() + decimalSecond.hashCode() + timeZone.hashCode()
    public final override fun toString(): String =
        "${this.javaClass.simpleName}(hour=$hour, minute=$minute, second=$decimalSecond, timeZone=$timeZone)"
}

/**
 * Superclass for all implementations representing time without time zone value
 * Informally, [TimeWithoutTimeZone] represents a particular orientation of a clock
 * which will represent different instances of "time" based on the timezone.
 */
public abstract class TimeWithoutTimeZone : Time {
    public final override val timeZone: Timezone? = null
    public abstract override fun plusHours(hours: Long): TimeWithoutTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimeWithoutTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimeWithoutTimeZone
    public abstract override fun atDate(date: Date): TimestampWithoutTimeZone

    /**
     * Returns a [TimeWithTimeZone] object, assuming the current [TimeWithoutTimeZone] object is in [timeZone].
     * The intention of this function is to append time zone information.
     */
    public abstract fun withTimeZone(timeZone: Timezone): TimeWithTimeZone

    public final override fun plusSeconds(seconds: Long): Time = plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimeWithoutTimeZone =
        DatetimePrecisionUtil.toPrecision(precision, this) as TimeWithoutTimeZone

    public final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as TimeWithoutTimeZone
        if (this.hour != other.hour) return false
        if (this.minute != other.minute) return false
        if (this.decimalSecond != other.decimalSecond) return false
        return true
    }

    public final override fun hashCode(): Int =
        hour.hashCode() + minute.hashCode() + decimalSecond.hashCode()

    public final override fun toString(): String =
        "${this.javaClass.simpleName}(hour=$hour, minute=$minute, second=$decimalSecond)"
}
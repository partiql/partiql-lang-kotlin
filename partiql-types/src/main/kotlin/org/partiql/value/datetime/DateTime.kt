package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import kotlin.jvm.Throws

/**
 * Superclass for all classes representing datetime values.
 */
public sealed interface DateTime {
    /**
     * Year field of date time object
     */
    public val year: Int?

    /**
     * Month field of date time object
     */
    public val month: Int?

    /**
     * Day field of date time object
     */
    public val day: Int?

    /**
     * Hour field of date time object
     */
    public val hour: Int?

    /**
     * Minute field of date time object
     */
    public val minute: Int?

    /**
     * Second field of date time object.
     * This field includes second fraction.
     */
    public val decimalSecond: BigDecimal?

    /**
     * Time zone field of date time object. See [TimeZone]
     */
    public val timeZone: TimeZone?

    /**
     * Equals method.
     * Two [DateTime] values are considered equals if and only if all the fields are equals.
     */
    public override fun equals(other: Any?): Boolean

    public override fun hashCode(): Int

    public override fun toString(): String
}

/**
 * Superclass for all implementations representing date value.
 * Date represents a calendar system, (i.e., 2023-06-01).
 * It does not include information on time or timezone, instead, it is meant to represent a specific date on calendar.
 * For example, 2022-11-25 (black friday in 2022).
 * The valid range are from 0001-01-01 to 9999-12-31
 * The [day] must be valid for the year and month, otherwise an exception will be thrown.
 */
public sealed interface Date : DateTime, Comparable<Date> {

    public override val year: Int
    public override val month: Int
    public override val day: Int

    /**
     * Hour field for [Date] value is always null.
     */
    public override val hour: Int?
        get() = null

    /**
     * Minute field for [Date] value is always null.
     */
    public override val minute: Int?
        get() = null

    /**
     * Second field for [Date] value is always null.
     */
    public override val decimalSecond: BigDecimal?
        get() = null

    /**
     * Timezone field for [Date] value is always null.
     */
    public override val timeZone: TimeZone?
        get() = null

    // Operation
    /**
     * Construct a [Timestamp] value by appending [time] to this [Date] value.
     */
    public fun atTime(time: Time): Timestamp

    /**
     * Returns a [Date] value with the specified number of days added.
     * The month and year fields may be changed as necessary to ensure the result remains valid.
     * [days] can be negative.
     */
    public fun plusDays(days: Long): Date

    /**
     * Returns a [Date] value with the specified number of months added.
     * The month and year fields may be changed as necessary to ensure the result remains valid.
     * [months] can be negative.
     */
    public fun plusMonths(months: Long): Date

    /**
     * Returns a [Date] value with the specified number of months added.
     * [years] can be negative.
     */
    public fun plusYears(years: Long): Date

    /**
     * Comparison method for [Date] value.
     *
     * Since [Date] value has no concept of time zone, they are compared as calendar date.
     */
    public override fun compareTo(other: Date): Int =
        DateTimeComparator.compareTo(this, other)
}

/**
 * Superclass for all implementations representing time value.
 * See [TimeWithTimeZone] and [TimeWithoutTimeZone]
 */
public sealed interface Time : DateTime, Comparable<Time> {
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
    public override val timeZone: TimeZone?

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
        DateTimeComparator.compareTo(this, other)
}

/**
 * Superclass for all implementations representing time value.
 * See [TimestampWithTimeZone] and [TimestampWithoutTimeZone]
 */
public sealed interface Timestamp : DateTime, Comparable<Timestamp> {

    public override val year: Int
    public override val month: Int
    public override val day: Int
    public override val hour: Int
    public override val minute: Int
    public override val decimalSecond: BigDecimal
    public override val timeZone: TimeZone?

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
        DateTimeComparator.compareTo(this, other)
}

/**
 * Superclass for all implementation representing date value
 */
public abstract class DateImpl : Date, Comparable<Date> {
    public final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as DateImpl
        if (this.year != other.year) return false
        if (this.month != other.month) return false
        if (this.day != other.day) return false
        return true
    }

    public final override fun hashCode(): Int =
        year.hashCode() + month.hashCode() + day.hashCode()

    public final override fun toString(): String =
        "${this.javaClass.simpleName}(year=$year, month=$month, day=$day)"
}

/**
 * Superclass for all implementations representing a time with time zone value.
 * Informally, [TimeWithTimeZone] represents an orientation of a clock attached with timezone offset information.
 */
public abstract class TimeWithTimeZone : Time {
    public abstract override val timeZone: TimeZone
    public abstract override fun plusHours(hours: Long): TimeWithTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimeWithTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimeWithTimeZone
    public final override fun plusSeconds(seconds: Long): Time = plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimeWithTimeZone =
        DateTimePrecisionChanger.toPrecision(precision, this) as TimeWithTimeZone
    public abstract override fun atDate(date: Date): TimestampWithTimeZone

    /**
     * Returns a [TimeWithoutTimeZone] object, assuming the local time is in [timeZone].
     * The intention of the function is to get a clock time in the [timeZone].
     */
    public abstract fun toTimeWithoutTimeZone(timeZone: TimeZone): TimeWithoutTimeZone

    /**
     * Returns a [TimeWithTimeZone] object, assuming the desiredZone is [timeZone]
     * The intention of this function is to get the same instant of time in the desired zone.
     */
    public abstract fun atTimeZone(timeZone: TimeZone): TimeWithTimeZone
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
    public final override val timeZone: TimeZone? = null
    public abstract override fun plusHours(hours: Long): TimeWithoutTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimeWithoutTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimeWithoutTimeZone
    public abstract override fun atDate(date: Date): TimestampWithoutTimeZone

    /**
     * Returns a [TimeWithTimeZone] object, assuming the current [TimeWithoutTimeZone] object is in [timeZone].
     * The intention of this function is to append time zone information.
     */
    public abstract fun withTimeZone(timeZone: TimeZone): TimeWithTimeZone

    public final override fun plusSeconds(seconds: Long): Time = plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimeWithoutTimeZone =
        DateTimePrecisionChanger.toPrecision(precision, this) as TimeWithoutTimeZone

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

/**
 * Superclass for all implementations representing timestamp with time zone value.
 * A [TimestampWithTimeZone] is a instant in the timeline.
 */
public abstract class TimestampWithTimeZone : Timestamp {
    public abstract override val timeZone: TimeZone

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
            TimeZone.UnknownTimeZone -> com.amazon.ion.Timestamp.forSecond(
                this.year, this.month, this.day,
                this.hour, this.minute, this.decimalSecond,
                com.amazon.ion.Timestamp.UNKNOWN_OFFSET
            )

            is TimeZone.UtcOffset -> com.amazon.ion.Timestamp.forSecond(
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
        DateTimePrecisionChanger.toPrecision(precision, this) as TimestampWithTimeZone

    /**
     * Returns a [TimestampWithoutTimeZone] object, assuming the local time is in [timeZone].
     */
    public abstract fun toTimeWithoutTimeZone(timeZone: TimeZone): TimestampWithoutTimeZone

    /**
     * Returns a [TimestampWithTimeZone] object, assuming the desiredZone is [timeZone]
     * The intention of this function is to get the same instant of time in the desired zone.
     */
    public abstract fun atTimeZone(timeZone: TimeZone): TimestampWithTimeZone
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
}

/**
 * Superclass for all implementations representing timestamp without time zone value.
 * A [TimestampWithoutTimeZone] is not an instant in the timeline, the same value may refer to different instant in timeline at different time zone.
 */
public abstract class TimestampWithoutTimeZone : Timestamp {
    public override val timeZone: TimeZone? = null
    public abstract override fun plusYears(years: Long): TimestampWithoutTimeZone
    public abstract override fun plusMonths(months: Long): TimestampWithoutTimeZone
    public abstract override fun plusDays(days: Long): TimestampWithoutTimeZone
    public abstract override fun plusHours(hours: Long): TimestampWithoutTimeZone
    public abstract override fun plusMinutes(minutes: Long): TimestampWithoutTimeZone
    public abstract override fun plusSeconds(seconds: BigDecimal): TimestampWithoutTimeZone
    public override fun plusSeconds(seconds: Long): TimestampWithoutTimeZone =
        plusSeconds(seconds.toBigDecimal())
    public final override fun toPrecision(precision: Int): TimestampWithoutTimeZone =
        DateTimePrecisionChanger.toPrecision(precision, this) as TimestampWithoutTimeZone

    /**
     * Returns a [TimestampWithTimeZone] object, assuming the current [TimestampWithoutTimeZone] value is in [timeZone].
     * The intention of this function is to append time zone information.
     */
    public abstract fun withTimeZone(timeZone: TimeZone): TimestampWithTimeZone
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

package org.partiql.value.datetime

import java.math.BigDecimal

/**
 * Superclass for all classes representing datetime values.
 */
public sealed interface DateTime

/**
 * Superclass for all implementations representing date value
 */
public sealed interface Date: DateTime{
    public val year: Int
    public val month: Int
    public val day: Int

    public fun atTime(time: Time): Timestamp

    public fun plusDays(days: Long): Date
    public fun plusMonths(months: Long): Date
    public fun plusYear(years: Long): Date
}

/**
 * Superclass for all implementations representing time value
 */
public sealed interface Time : DateTime{
    public val hour: Int
    public val minute: Int
    public val decimalSecond: BigDecimal
    public val timeZone: TimeZone?

    // Operation
    public fun plusHours(hours: Long): Time

    public fun plusMinutes(minutes: Long): Time

    public fun plusSeconds(seconds: Number) : Time

    public fun toPrecision(precision: Int) : Time

    public fun atDate(date: Date) : Timestamp
}


/**
 * Superclass for all implementations representing time value
 */
public sealed interface Timestamp: DateTime{
    public val year: Int
    public val month: Int
    public val day: Int
    public val hour: Int
    public val minute: Int
    public val decimalSecond: BigDecimal
    public val timeZone: TimeZone?

    // Operation
    public fun plusYear(years: Long): Timestamp
    public fun plusMonths(months: Long): Timestamp
    public fun plusDays(days: Long): Timestamp
    public fun plusHours(hours: Long): Timestamp
    public fun plusMinutes(minutes: Long): Timestamp
    public fun plusSeconds(seconds: Number) : Timestamp
    public fun toPrecision(precision: Int) : Timestamp
    public fun toDate() : Date
    public fun toTime() : Time
}

/**
 * Superclass for all implementations representing a time with time zone value
 */
public sealed interface TimeWithTimeZone: Time {
    public abstract override val timeZone : TimeZone

    public fun atTimeZone(timeZone: TimeZone): TimeWithTimeZone
}

/**
 * Superclass for all implementations representing time without time zone value
 */
public sealed interface TimeWithoutTimeZone: Time {
    public override val timeZone: TimeZone?
        get() = null
}

/**
 * Superclass for all implementations representing timestamp with time zone value
 */
public sealed interface TimestampWithTimeZone: Timestamp {
    public abstract override val timeZone : TimeZone
    public fun atTimeZone(timeZone: TimeZone) : Timestamp
}

/**
 * Superclass for all implementations representing timestamp without time zone value
 */
public sealed interface TimestampWithoutTimeZone: Timestamp {
    public override val timeZone : TimeZone?
        get() = null
}
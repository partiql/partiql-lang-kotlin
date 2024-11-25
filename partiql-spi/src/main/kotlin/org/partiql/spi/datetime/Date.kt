package org.partiql.spi.datetime

import org.partiql.spi.datetime.util.DatetimeComparisons
import java.math.BigDecimal

/**
 * Superclass for all implementations representing date value.
 * Date represents a calendar system, (i.e., 2023-06-01).
 * It does not include information on time or timezone, instead, it is meant to represent a specific date on calendar.
 * For example, 2022-11-25 (black friday in 2022).
 * The valid range are from 0001-01-01 to 9999-12-31
 * The [day] must be valid for the year and month, otherwise an exception will be thrown.
 */
public sealed interface Date : Datetime, Comparable<Date> {

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
    public override val timeZone: Timezone?
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
        DatetimeComparisons.compareTo(this, other)
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
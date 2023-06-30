package org.partiql.value.datetime

import java.time.LocalDate

/**
 * A date is modeled by [year], [month], and [day].
 * The valid range are from 0001-01-01 to 9999-12-31
 * The [day] must be valid for the year and month, otherwise an exception will be thrown.
 */
public data class Date private constructor(
    val year: Int,
    val month: Int,
    val day: Int
) {
    public companion object {
        public fun of(year: Int, month: Int, day: Int): Date {
            if (year < 1 || year > 9999)
                throw DateTimeFormatException("Expect Year Field to be between 1 to 9999, but received $year")
            try {
                LocalDate.of(year, month, day)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeFormatException(e.localizedMessage, e)
            }
            return Date(year, month, day)
        }
    }
    public val localDate: LocalDate by lazy {
        LocalDate.of(year, month, day)
    }
    public val epochDays: Long by lazy {
        this.localDate.toEpochDay()
    }
    // Operation
    public fun plusDays(days: Long): Date =
        this.localDate.plusDays(days)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
    public fun plusMonths(months: Long): Date =
        this.localDate.plusMonths(months)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
    public fun plusYear(years: Long): Date =
        this.localDate.plusYears(years)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
}

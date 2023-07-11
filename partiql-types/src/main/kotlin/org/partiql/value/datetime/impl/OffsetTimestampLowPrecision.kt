package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeUtil.JAVA_MAX_OFFSET
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import org.partiql.value.datetime.TimestampWithTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * This implementation utilize [java.time.OffsetDateTime] to handle timestamp with time zone if :
 * 1. The desired precision is below nanosecond (9 digits after decimal point).
 * 2. The desired timezone is within +18:00 to -18:00.
 *
 * The constructor functions assumes the above conditions to be true.
 */
internal class OffsetTimestampLowPrecision(
    val offsetDateTime: OffsetDateTime,
    val isUnknownTimeZone: Boolean,
    val date: Date,
    val time: OffsetTimeLowPrecision,
    _inputIonTimestamp: com.amazon.ion.Timestamp? = null,
    _epochSecond: BigDecimal? = null
) : TimestampWithTimeZone() {

    companion object {
        /**
         * Construct a Timestamp by concatenate a Date and a Time component.
         *
         * The time component should have a TimeZone.
         *
         * This should be called by other constructors for validation date / time.
         */
        fun forDateTime(date: Date, time: OffsetTimeLowPrecision): OffsetTimestampLowPrecision {
            val offsetDateTime =
                OffsetDateTime.of(
                    LocalDate.of(date.year, date.month, date.day),
                    time.offsetTime.toLocalTime(),
                    time.offsetTime.offset
                )
            return OffsetTimestampLowPrecision(offsetDateTime, time.isUnknownTimeZone, date, time)
        }

        /**
         * Construct a Timestamp with nanosecond precision.
         */
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            timeZone: TimeZone,
        ): OffsetTimestampLowPrecision {
            val date = SqlDate.of(year, month, day)
            val time = OffsetTimeLowPrecision.of(hour, minute, second, nanoOfSecond, timeZone)
            return forDateTime(date, time)
        }

        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal,
            timeZone: TimeZone
        ): OffsetTimestampLowPrecision {
            val date = SqlDate.of(year, month, day)
            val time = OffsetTimeLowPrecision.of(hour, minute, decimalSecond, timeZone)
            return forDateTime(date, time)
        }

        fun forIonTimestamp(ionTs: com.amazon.ion.Timestamp): TimestampWithTimeZone {
            val timestamp = when {
                ionTs.localOffset == null ->
                    of(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UnknownTimeZone
                    ).copy(_inputIonTimestamp = ionTs)

                // Should never be reached.
                ionTs.localOffset.absoluteValue > JAVA_MAX_OFFSET ->
                    OffsetTimestampHighPrecision.of(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UtcOffset.of(ionTs.localOffset)
                    ).copy(_inputIonTimestamp = ionTs)

                else ->
                    of(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UtcOffset.of(ionTs.localOffset)
                    ).copy(_inputIonTimestamp = ionTs)
            }
            return timestamp
        }

        fun forEpochSeconds(epochSeconds: BigDecimal, timeZone: TimeZone): OffsetTimestampLowPrecision {
            val wholeSeconds = epochSeconds.setScale(0, RoundingMode.DOWN)
            val nano = (epochSeconds - wholeSeconds).let { it.movePointRight(9) }
            val offsetDateTime = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(wholeSeconds.longValueExact(), nano.longValueExact()),
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> ZoneOffset.ofTotalSeconds(0)
                    is TimeZone.UtcOffset -> ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60)
                }
            )
            val date = SqlDate.of(offsetDateTime.year, offsetDateTime.monthValue, offsetDateTime.dayOfMonth)
            // we need to assign a precision based on the input epochSecond
            val time =
                OffsetTimeLowPrecision
                    .of(offsetDateTime.hour, offsetDateTime.minute, offsetDateTime.second, offsetDateTime.nano, timeZone)
                    .let { it.copy(_decimalSecond = it.decimalSecond.setScale(epochSeconds.scale(), RoundingMode.UNNECESSARY)) }
            return forDateTime(date, time).copy(_epochSecond = epochSeconds)
        }

        @JvmStatic
        fun nowZ(): OffsetTimestampLowPrecision {
            val javaNowZ = OffsetDateTime.now(ZoneOffset.UTC)
            val date = SqlDate.of(javaNowZ.year, javaNowZ.monthValue, javaNowZ.dayOfMonth)
            val time = OffsetTimeLowPrecision.of(javaNowZ.hour, javaNowZ.minute, javaNowZ.second, javaNowZ.nano, TimeZone.UtcOffset.of(0))
            return forDateTime(date, time)
        }
    }

    override val year: Int = date.year
    override val month: Int = date.month
    override val day: Int = date.day
    override val hour: Int = time.hour
    override val minute: Int = time.minute
    override val decimalSecond: BigDecimal = time.decimalSecond
    override val timeZone: TimeZone = time.timeZone
    override val ionRaw: com.amazon.ion.Timestamp? = _inputIonTimestamp
    val second: Int = offsetDateTime.second
    val nano: Int = offsetDateTime.nano
    override val epochSecond: BigDecimal by lazy {
        _epochSecond
            ?: ((offsetDateTime.toEpochSecond() - second).toBigDecimal() + decimalSecond)
    }

    override fun plusYear(years: Long): OffsetTimestampLowPrecision =
        offsetDateTime.plusYears(years).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusMonths(months: Long): OffsetTimestampLowPrecision =
        offsetDateTime.plusMonths(months).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusDays(days: Long): OffsetTimestampLowPrecision =
        offsetDateTime.plusDays(days).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusHours(hours: Long): OffsetTimestampLowPrecision =
        offsetDateTime.plusHours(hours).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusMinutes(minutes: Long): OffsetTimestampLowPrecision =
        offsetDateTime.plusMinutes(minutes).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusSeconds(seconds: BigDecimal): TimestampWithTimeZone =
        if (seconds.scale() > 9) {
            OffsetTimestampHighPrecision.of(year, month, day, hour, minute, decimalSecond, timeZone)
                .plusSeconds(seconds)
        } else {
            val wholeSecond = seconds.setScale(0, RoundingMode.DOWN)
            val nano = seconds.minus(wholeSecond).movePointRight(9)
            val newTime = offsetDateTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
            // the real precision of this operation, should be max(original_value.decimalSecond.precision, seconds.precision)
            val newDecimalSecond = newTime.second.toBigDecimal() + newTime.nano.toBigDecimal().movePointLeft(9)
            val roundedDecimalSecond =
                newDecimalSecond.stripTrailingZeros().setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(
                newTime.year,
                newTime.monthValue,
                newTime.dayOfMonth,
                newTime.hour,
                newTime.minute,
                roundedDecimalSecond,
                timeZone
            )
        }

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): LocalTimestampLowPrecision {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetDateTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        // Second field should be intact from this operation
        return LocalTimestampLowPrecision.of(
            local.year,
            local.monthValue,
            local.dayOfMonth,
            local.hour,
            local.minute,
            decimalSecond
        )
    }

    override fun atTimeZone(timeZone: TimeZone): OffsetTimestampLowPrecision {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetDateTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        return of(
            local.year,
            local.monthValue,
            local.dayOfMonth,
            local.hour,
            local.minute,
            this.decimalSecond,
            timeZone
        )
    }

    internal fun copy(
        _inputIonTimestamp: com.amazon.ion.Timestamp? = null,
        _epochSecond: BigDecimal? = null
    ) =
        OffsetTimestampLowPrecision(
            this.offsetDateTime, this.isUnknownTimeZone, this.date, this.time,
            _inputIonTimestamp ?: this.ionRaw, _epochSecond ?: this.epochSecond
        )
}

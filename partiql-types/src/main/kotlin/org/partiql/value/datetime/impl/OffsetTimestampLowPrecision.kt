package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil.JAVA_MAX_OFFSET
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue
import kotlin.math.max

internal class OffsetTimestampLowPrecision(
    val offsetDateTime: OffsetDateTime,
    val isUnknownTimeZone: Boolean,
    _year: Int? = null,
    _month: Int? = null,
    _day: Int? = null,
    _hour: Int? = null,
    _minute: Int? = null,
    _decimalSecond: BigDecimal? = null,
    _timeZone: TimeZone? = null,
    _inputIonTimestamp: com.amazon.ion.Timestamp? = null
) : TimestampWithTimeZone() {

    companion object {
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
            try {
                return when (timeZone) {
                    TimeZone.UnknownTimeZone ->
                        OffsetTimestampLowPrecision(
                            OffsetDateTime.of(year, month, day, hour, minute, second, nanoOfSecond, ZoneOffset.UTC),
                            true,
                            year, month, day, hour, minute, null, timeZone, null
                        )

                    is TimeZone.UtcOffset -> OffsetTimestampLowPrecision(
                        OffsetDateTime.of(
                            year, month, day,
                            hour, minute, second, nanoOfSecond,
                            ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60)
                        ),
                        false,
                        year, month, day, hour, minute, null, timeZone, null
                    )
                }
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage)
            }
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
            val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
            val nano = decimalSecond.minus(wholeSecond).movePointRight(9)
            return of(year, month, day, hour, minute, wholeSecond.intValueExact(), nano.intValueExact(), timeZone).let {
                OffsetTimestampLowPrecision(
                    it.offsetDateTime, it.isUnknownTimeZone,
                    year, month, day,
                    hour, minute, decimalSecond,
                    timeZone, null
                )
            }
        }

        fun forDateTime(date: Date, time: TimeWithTimeZone): OffsetTimestampLowPrecision {
            return of(date.year, date.month, date.day, time.hour, time.minute, time.decimalSecond, time.timeZone)
        }

        fun forIonTimestamp(ionTs: com.amazon.ion.Timestamp): TimestampWithTimeZone {
            val timestamp = when {
                ionTs.localOffset == null ->
                    of(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UnknownTimeZone
                    ).copy(_inputIonTimestamp = ionTs)
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

        @JvmStatic
        fun nowZ() =
            OffsetTimestampLowPrecision(OffsetDateTime.now(ZoneOffset.UTC), false)
    }

    override val year: Int = _year ?: offsetDateTime.year
    override val month: Int = _month ?: offsetDateTime.monthValue
    override val day: Int = _day ?: offsetDateTime.dayOfMonth
    override val hour: Int = _hour ?: offsetDateTime.hour
    override val minute: Int = _minute ?: offsetDateTime.minute
    override val decimalSecond: BigDecimal =
        _decimalSecond
            ?: BigDecimal.valueOf(offsetDateTime.second.toLong())
                .plus(BigDecimal.valueOf(offsetDateTime.nano.toLong(), 9))
    override val timeZone: TimeZone =
        _timeZone
            ?: if (isUnknownTimeZone) TimeZone.UnknownTimeZone else TimeZone.UtcOffset.of(offsetDateTime.offset.totalSeconds / 60)
    override val ionRaw: com.amazon.ion.Timestamp? = _inputIonTimestamp
    val second: Int = offsetDateTime.second
    val nano: Int = offsetDateTime.nano
    override val epochSecond: BigDecimal by lazy {
        (offsetDateTime.toEpochSecond() - second).toBigDecimal() + decimalSecond
    }

    override fun plusYear(years: Long): TimestampWithTimeZone =
        offsetDateTime.plusYears(years).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusMonths(months: Long): TimestampWithTimeZone =
        offsetDateTime.plusMonths(months).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusDays(days: Long): TimestampWithTimeZone =
        offsetDateTime.plusDays(days).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusHours(hours: Long): TimestampWithTimeZone =
        offsetDateTime.plusHours(hours).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusMinutes(minutes: Long): TimestampWithTimeZone =
        offsetDateTime.plusMinutes(minutes).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusSeconds(seconds: BigDecimal): TimestampWithTimeZone =
        if (seconds.scale() > 9) {
            OffsetTimestampHighPrecision.of(year, month, day, hour, minute, decimalSecond, timeZone).plusSeconds(seconds)
        } else {
            val wholeSecond = seconds.setScale(0, RoundingMode.DOWN)
            val nano = seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            val newTime = offsetDateTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
            // the real precision of this operation, should be max(original_value.decimalSecond.precision, seconds.precision)
            val newDecimalSecond = newTime.second.toBigDecimal() + newTime.nano.toBigDecimal().movePointLeft(9)
            val roundedDecimalSecond =
                newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.year, newTime.monthValue, newTime.dayOfMonth, newTime.hour, newTime.minute, roundedDecimalSecond, timeZone)
        }

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): TimestampWithoutTimeZone {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetDateTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        // Second field should be intact from this operation
        return LocalTimestampLowPrecision.of(local.year, local.monthValue, local.dayOfMonth, local.hour, local.minute, decimalSecond)
    }

    override fun atTimeZone(timeZone: TimeZone): TimestampWithTimeZone {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetDateTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        return of(local.year, local.monthValue, local.dayOfMonth, local.hour, local.minute, this.decimalSecond, timeZone)
    }

    internal fun copy(_year: Int? = null, _month: Int? = null, _day: Int? = null,
                                                  _hour: Int? = null, _minute: Int? = null, _decimalSecond: BigDecimal? = null,
                                                  _timeZone: TimeZone? = null, _inputIonTimestamp: com.amazon.ion.Timestamp? = null) =
        OffsetTimestampLowPrecision(
            this.offsetDateTime, this.isUnknownTimeZone,
            _year?: this.year, _month ?: this.month, _day?: this.day,
            _hour?:this.hour, _minute?:this.minute, _decimalSecond?:this.decimalSecond,
            _timeZone?:this.timeZone, _inputIonTimestamp ?:this.ionRaw
        )
}

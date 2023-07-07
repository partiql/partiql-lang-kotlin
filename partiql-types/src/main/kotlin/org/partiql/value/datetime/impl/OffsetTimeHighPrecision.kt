package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimePrecisionChanger
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField

internal class OffsetTimeHighPrecision private constructor(
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal,
    override val timeZone: TimeZone,
    _localTime: LocalTimeHighPrecision,
    _elapsedSecond: BigDecimal? = null
) : TimeWithTimeZone() {

    companion object {
        fun of(
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal,
            timeZone: TimeZone
        ): OffsetTimeHighPrecision {
            try {
                val localTime = LocalTimeHighPrecision.of(hour, minute, decimalSecond)
                return OffsetTimeHighPrecision(localTime.hour, localTime.minute, localTime.decimalSecond, timeZone, localTime)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }

        fun forSeconds(elapsedSeconds: BigDecimal, timeZone: TimeZone): OffsetTimeHighPrecision {
            val localTime = LocalTimeHighPrecision.forSeconds(elapsedSeconds)
            return OffsetTimeHighPrecision(
                localTime.hour, localTime.minute, localTime.decimalSecond,
                timeZone, localTime, elapsedSeconds
            )
        }
    }

    override val elapsedSecond: BigDecimal by lazy {
        _elapsedSecond ?: _localTime.elapsedSecond
    }

    override fun plusHours(hours: Long): TimeWithTimeZone =
        forSeconds(this.elapsedSecond.plus((hours * SECONDS_IN_HOUR).toBigDecimal()), timeZone)

    override fun plusMinutes(minutes: Long): TimeWithTimeZone =
        forSeconds(this.elapsedSecond.plus((minutes * DateTimeUtil.SECONDS_IN_MINUTE).toBigDecimal()), timeZone)

    override fun plusSeconds(seconds: BigDecimal): TimeWithTimeZone =
        forSeconds(this.elapsedSecond.plus(seconds.toBigDecimal()), timeZone)

    override fun atDate(date: Date): TimestampWithTimeZone =
        OffsetTimestampHighPrecision.forDateTime(date, this)

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): TimeWithoutTimeZone =
        this.atTimeZone(timeZone).let {
            LocalTimeHighPrecision.of(it.hour, it.minute, it.decimalSecond)
        }

    override fun atTimeZone(timeZone: TimeZone): TimeWithTimeZone =
        when (val valueTimeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this
                    is TimeZone.UtcOffset -> of(
                        hour, minute, decimalSecond,
                        TimeZone.UtcOffset.of(0)
                    ).atTimeZone(timeZone)
                }
            }

            is TimeZone.UtcOffset -> {
                val utc = this.plusMinutes(-valueTimeZone.totalOffsetMinutes.toLong())
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> of(utc.hour, utc.minute, utc.decimalSecond, timeZone)
                    is TimeZone.UtcOffset -> utc.plusMinutes(timeZone.totalOffsetMinutes.toLong())
                        .let { of(it.hour, it.minute, it.decimalSecond, timeZone) }
                }
            }
        }
}
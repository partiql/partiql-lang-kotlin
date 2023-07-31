package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.math.max

internal class OffsetTimeLowPrecision private constructor(
    private val offsetTime: OffsetTime,
    private val isUnknownTimeZone: Boolean,
    _hour: Int? = null,
    _minute: Int? = null,
    _decimalSecond: BigDecimal? = null,
    _timeZone: TimeZone? = null
) : TimeWithTimeZone() {
    companion object {
        fun of(
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            timeZone: TimeZone
        ): OffsetTimeLowPrecision {
            try {
                return when (timeZone) {
                    TimeZone.UnknownTimeZone ->
                        OffsetTimeLowPrecision(
                            OffsetTime.of(hour, minute, second, nanoOfSecond, ZoneOffset.UTC),
                            true
                        )

                    is TimeZone.UtcOffset -> OffsetTimeLowPrecision(
                        OffsetTime.of(
                            hour, minute, second, nanoOfSecond,
                            ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60)
                        ),
                        false
                    )
                }
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage)
            }
        }

        fun of(hour: Int, minute: Int, decimalSecond: BigDecimal, timeZone: TimeZone): OffsetTimeLowPrecision {
            val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
            val nano = decimalSecond.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            return of(hour, minute, wholeSecond.intValueExact(), nano.intValueExact(), timeZone).let {
                OffsetTimeLowPrecision(it.offsetTime, it.isUnknownTimeZone, it.hour, it.minute, decimalSecond, timeZone)
            }
        }
    }

    override val hour: Int = _hour ?: offsetTime.hour
    override val minute: Int = _minute ?: offsetTime.minute
    override val decimalSecond: BigDecimal =
        _decimalSecond
            ?: BigDecimal.valueOf(offsetTime.second.toLong()).plus(BigDecimal.valueOf(offsetTime.nano.toLong(), 9))
    override val timeZone: TimeZone =
        _timeZone
            ?: if (isUnknownTimeZone) TimeZone.UnknownTimeZone else TimeZone.UtcOffset.of(offsetTime.offset.totalSeconds / 60)
    override val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE) + this.decimalSecond
    }

    override fun plusHours(hours: Long): TimeWithTimeZone =
        offsetTime.plusHours(hours).let {
            of(it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusMinutes(minutes: Long): TimeWithTimeZone =
        offsetTime.plusMinutes(minutes).let {
            of(it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusSeconds(seconds: BigDecimal): TimeWithTimeZone =
        if (seconds.scale() > 9) {
            OffsetTimeHighPrecision.of(hour, minute, decimalSecond, timeZone).plusSeconds(seconds)
        } else {
            val wholeSecond = seconds.setScale(0, RoundingMode.DOWN)
            val nano = seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            val newTime = offsetTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
            val newDecimalSecond = newTime.second.toBigDecimal() + newTime.nano.toBigDecimal().movePointLeft(9)
            val roundedDecimalSecond = newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.hour, newTime.minute, roundedDecimalSecond, timeZone)
        }

    override fun atDate(date: Date): TimestampWithTimeZone =
        OffsetTimestampLowPrecision.forDateTime(date, this)

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): TimeWithoutTimeZone {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        // Second field should be intact from this operation
        return LocalTimeLowPrecision.of(local.hour, local.minute, this.decimalSecond)
    }

    override fun atTimeZone(timeZone: TimeZone): TimeWithTimeZone {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        return of(local.hour, local.minute, this.decimalSecond, timeZone)
    }
}

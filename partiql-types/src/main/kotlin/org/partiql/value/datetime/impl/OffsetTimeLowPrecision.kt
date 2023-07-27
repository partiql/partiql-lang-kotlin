package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.math.max

/**
 * This implementation utilize [java.time.OffsetTime] to handle time without time zone if :
 * 1. The desired precision is below nanosecond (9 digits after decimal point).
 * 2. The desired timezone is within +18:00 to -18:00.
 *
 * The constructor functions assumes the above conditions to be true.
 */
internal class OffsetTimeLowPrecision private constructor(
    val offsetTime: OffsetTime,
    val isUnknownTimeZone: Boolean,
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
            val (wholeSecond, nano) = Utils.getSecondAndNanoFromDecimalSecond(decimalSecond)
            return of(hour, minute, wholeSecond.toInt(), nano.toInt(), timeZone)
                .copy(_hour = hour, _minute = minute, _decimalSecond = decimalSecond, _timeZone = timeZone)
        }
    }

    override val hour: Int = _hour ?: offsetTime.hour
    override val minute: Int = _minute ?: offsetTime.minute
    override val decimalSecond: BigDecimal =
        _decimalSecond
            ?: Utils.getDecimalSecondFromSecondAndNano(this.offsetTime.second.toLong(), this.offsetTime.nano.toLong())
    override val timeZone: TimeZone =
        _timeZone
            ?: if (isUnknownTimeZone) TimeZone.UnknownTimeZone else TimeZone.UtcOffset.of(offsetTime.offset.totalSeconds / 60)
    override val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE) + this.decimalSecond
    }

    override fun plusHours(hours: Long): OffsetTimeLowPrecision =
        offsetTime.plusHours(hours).let {
            of(it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusMinutes(minutes: Long): OffsetTimeLowPrecision =
        offsetTime.plusMinutes(minutes).let {
            of(it.hour, it.minute, decimalSecond, timeZone)
        }

    override fun plusSeconds(seconds: BigDecimal): TimeWithTimeZone =
        if (seconds.scale() > 9) {
            OffsetTimeHighPrecision.of(hour, minute, decimalSecond, timeZone).plusSeconds(seconds)
        } else {
            val (wholeSecond, nano) = Utils.getSecondAndNanoFromDecimalSecond(seconds)
            val newTime = offsetTime.plusSeconds(wholeSecond).plusNanos(nano)
            val newDecimalSecond = Utils.getDecimalSecondFromSecondAndNano(newTime.second.toLong(), newTime.nano.toLong())
            val roundedDecimalSecond = newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.hour, newTime.minute, roundedDecimalSecond, timeZone)
        }

    override fun atDate(date: Date): OffsetTimestampLowPrecision =
        OffsetTimestampLowPrecision.forDateTime(date, this)

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): LocalTimeLowPrecision {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        // Second field should be intact from this operation
        return LocalTimeLowPrecision.of(local.hour, local.minute, this.decimalSecond)
    }

    override fun atTimeZone(timeZone: TimeZone): OffsetTimeLowPrecision {
        val local = when (timeZone) {
            TimeZone.UnknownTimeZone -> offsetTime.withOffsetSameInstant(ZoneOffset.UTC)
            is TimeZone.UtcOffset -> offsetTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60))
        }
        return of(local.hour, local.minute, this.decimalSecond, timeZone)
    }

    fun copy(_hour: Int? = null, _minute: Int? = null, _decimalSecond: BigDecimal? = null, _timeZone: TimeZone? = null) =
        OffsetTimeLowPrecision(this.offsetTime, this.isUnknownTimeZone, _hour, _minute, _decimalSecond, _timeZone)
}

package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import kotlin.math.max

internal class LocalTimeLowPrecision private constructor(
    private val localTime: LocalTime,
    _hour: Int? = null,
    _minute: Int? = null,
    _decimalSecond: BigDecimal? = null,
) : TimeWithoutTimeZone() {
    companion object {
        /**
         * Creates a [TimeWithTimeZone] value.
         * If using this API, the precision will be nanosecond (i.e., exactly 9 digits in second fraction)
         */
        fun forNano(hour: Int, minute: Int, second: Int, nanoOfSecond: Int): LocalTimeLowPrecision {
            try {
                return LocalTimeLowPrecision(LocalTime.of(hour, minute, second, nanoOfSecond))
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage)
            }
        }

        fun of(hour: Int, minute: Int, decimalSecond: BigDecimal): LocalTimeLowPrecision {
            val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
            val nano = decimalSecond.minus(wholeSecond).movePointRight(9)
            return forNano(hour, minute, wholeSecond.intValueExact(), nano.intValueExact()).let {
                LocalTimeLowPrecision(it.localTime, hour, minute, decimalSecond)
            }
        }
    }

    override val hour: Int = _hour ?: localTime.hour
    override val minute: Int = _minute ?: localTime.minute

    /**
     * Whole second.
     */
    internal val second: Int = localTime.second

    /**
     * Nano of second.
     */
    internal val nano: Int = localTime.nano
    override val decimalSecond: BigDecimal =
        _decimalSecond ?: BigDecimal.valueOf(second.toLong()).plus(BigDecimal.valueOf(nano.toLong(), 9))
    override val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE) + this.decimalSecond
    }

    override fun plusHours(hours: Long): TimeWithoutTimeZone =
        localTime.plusHours(hours).let {
            of(it.hour, it.minute, decimalSecond)
        }

    override fun plusMinutes(minutes: Long): TimeWithoutTimeZone =
        localTime.plusMinutes(minutes).let {
            of(it.hour, it.minute, decimalSecond)
        }

    override fun plusSeconds(seconds: BigDecimal): TimeWithoutTimeZone =
        if (seconds.scale() > 9) {
            LocalTimeHighPrecision.of(hour, minute, decimalSecond).plusSeconds(seconds)
        } else {
            val wholeSecond = seconds.setScale(0, RoundingMode.DOWN)
            val nano = seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            val newTime = localTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
            val newDecimalSecond = newTime.second.toBigDecimal() + newTime.nano.toBigDecimal().movePointLeft(9)
            val roundedDecimalSecond = newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.hour, newTime.minute, roundedDecimalSecond)
        }

    override fun atDate(date: Date): TimestampWithoutTimeZone =
        LocalTimestampLowPrecision.forDateTime(date, this)

    override fun withTimeZone(timeZone: TimeZone): TimeWithTimeZone =
        OffsetTimeLowPrecision.of(hour, minute, decimalSecond, timeZone)
}

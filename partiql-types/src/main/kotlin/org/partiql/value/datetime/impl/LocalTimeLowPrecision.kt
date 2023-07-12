package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import kotlin.math.max

/**
 * This implementation utilize [java.time.LocalTime] to handle time without time zone if :
 * 1. The desired precision is below nanosecond (9 digits after decimal point).
 *
 * The constructor functions assumes the above condition to be true.
 */
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
            return forNano(hour, minute, wholeSecond.intValueExact(), nano.intValueExact()).copy(
                _hour = hour,
                _minute = minute,
                _decimalSecond = decimalSecond
            )
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
        _decimalSecond ?: Utils.getDecimalSecondFromSecondAndNano(this.second.toLong(), this.nano.toLong())
    override val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE) + this.decimalSecond
    }

    override fun plusHours(hours: Long): LocalTimeLowPrecision =
        localTime.plusHours(hours).let {
            of(it.hour, it.minute, decimalSecond)
        }

    override fun plusMinutes(minutes: Long): LocalTimeLowPrecision =
        localTime.plusMinutes(minutes).let {
            of(it.hour, it.minute, decimalSecond)
        }

    override fun plusSeconds(seconds: BigDecimal): TimeWithoutTimeZone =
        if (seconds.scale() > 9) {
            LocalTimeHighPrecision.of(hour, minute, decimalSecond).plusSeconds(seconds)
        } else {
            val (wholeSecond, nano) = Utils.getSecondAndNanoFromDecimalSecond(seconds)
            val newTime = localTime.plusSeconds(wholeSecond).plusNanos(nano)
            val newDecimalSecond = Utils.getDecimalSecondFromSecondAndNano(newTime.second.toLong(), newTime.nano.toLong())
            val roundedDecimalSecond = newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.hour, newTime.minute, roundedDecimalSecond)
        }

    override fun atDate(date: Date): LocalTimestampLowPrecision =
        LocalTimestampLowPrecision.forDateTime(date as SqlDate, this)

    override fun withTimeZone(timeZone: TimeZone): OffsetTimeLowPrecision =
        OffsetTimeLowPrecision.of(hour, minute, decimalSecond, timeZone)

    fun copy(_hour: Int? = null, _minute: Int? = null, _decimalSecond: BigDecimal?) =
        LocalTimeLowPrecision(this.localTime, _hour, _minute, _decimalSecond)
}

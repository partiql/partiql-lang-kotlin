package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField

/**
 * This implementation handles edge cases that can not be supported by [LocalTimeLowPrecision], that is:
 * 1. The desired precision exceeds nanosecond.
 * 2. The desired timestamp exceeds the range of +18:00 to -18:00
 */
internal class LocalTimeHighPrecision private constructor(
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal,
    _elapsedSecond: BigDecimal? = null
) : TimeWithoutTimeZone() {
    companion object {
        fun of(
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal
        ): LocalTimeHighPrecision {
            try {
                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
                // round down the decimalSecond to check
                ChronoField.SECOND_OF_MINUTE.checkValidValue(decimalSecond.setScale(0, RoundingMode.DOWN).toLong())
                return LocalTimeHighPrecision(hour, minute, decimalSecond)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }

        fun forSeconds(elapsedSeconds: BigDecimal): LocalTimeHighPrecision {
            val wholeSecond = elapsedSeconds.setScale(0, RoundingMode.DOWN).longValueExact()
            val fraction = elapsedSeconds.minus(BigDecimal.valueOf(wholeSecond))
            var total = wholeSecond
            val hour = total / DateTimeUtil.SECONDS_IN_HOUR
            total -= hour * DateTimeUtil.SECONDS_IN_HOUR
            val minute = total / DateTimeUtil.SECONDS_IN_MINUTE
            total -= minute * DateTimeUtil.SECONDS_IN_MINUTE
            return of(hour.toInt(), minute.toInt(), fraction.plus(BigDecimal.valueOf(total))).copy(elapsedSeconds)
        }
    }

    override val elapsedSecond: BigDecimal by lazy {
        _elapsedSecond
            ?: (BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE) + this.decimalSecond)
    }

    override fun plusHours(hours: Long): LocalTimeHighPrecision =
        forSeconds(this.elapsedSecond.plus((hours * DateTimeUtil.SECONDS_IN_HOUR).toBigDecimal()))

    override fun plusMinutes(minutes: Long): LocalTimeHighPrecision =
        forSeconds(this.elapsedSecond.plus((minutes * DateTimeUtil.SECONDS_IN_MINUTE).toBigDecimal()))

    override fun plusSeconds(seconds: BigDecimal): LocalTimeHighPrecision =
        forSeconds(this.elapsedSecond.plus(seconds))

    override fun atDate(date: Date): LocalTimestampHighPrecision =
        LocalTimestampHighPrecision.forDateTime(date as SqlDate, this)

    override fun withTimeZone(timeZone: TimeZone): OffsetTimeHighPrecision =
        OffsetTimeHighPrecision.of(hour, minute, decimalSecond, timeZone)

    fun copy(_elapsedSecond: BigDecimal? = null) = LocalTimeHighPrecision(this.hour, this.minute, this.decimalSecond, _elapsedSecond)
}

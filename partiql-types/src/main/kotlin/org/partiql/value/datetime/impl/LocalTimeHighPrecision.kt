package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField

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
            decimalSecond: BigDecimal,
            elapsedSeconds: BigDecimal? = null
        ): LocalTimeHighPrecision {
            try {
                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
                // round down the decimalSecond to check
                ChronoField.SECOND_OF_MINUTE.checkValidValue(decimalSecond.setScale(0, RoundingMode.DOWN).toLong())
                return LocalTimeHighPrecision(hour, minute, decimalSecond, elapsedSeconds)
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
            return of(hour.toInt(), minute.toInt(), fraction.plus(BigDecimal.valueOf(total)), elapsedSeconds)
        }
    }

    override val elapsedSecond: BigDecimal by lazy {
        _elapsedSecond
            ?: (BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE) + this.decimalSecond)
    }

    override fun plusHours(hours: Long): TimeWithoutTimeZone =
        forSeconds(this.elapsedSecond.plus((hours * DateTimeUtil.SECONDS_IN_HOUR).toBigDecimal()))

    override fun plusMinutes(minutes: Long): TimeWithoutTimeZone =
        forSeconds(this.elapsedSecond.plus((minutes * DateTimeUtil.SECONDS_IN_MINUTE).toBigDecimal()))

    override fun plusSeconds(seconds: BigDecimal): TimeWithoutTimeZone =
        forSeconds(this.elapsedSecond.plus(seconds))

    override fun atDate(date: Date): TimestampWithoutTimeZone =
        LocalTimestampHighPrecision.forDateTime(date, this)

    override fun withTimeZone(timeZone: TimeZone): TimeWithTimeZone =
        OffsetTimeHighPrecision.of(hour, minute, decimalSecond, timeZone)
}

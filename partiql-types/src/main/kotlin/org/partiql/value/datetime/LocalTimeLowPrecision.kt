package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import kotlin.jvm.Throws

public data class LocalTimeLowPrecision private constructor(
    val localTime: LocalTime
) : TimeWithoutTimeZone {
    public companion object {
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(hour: Int, minute: Int, second: Int, nanoOfSecond: Int) : LocalTimeLowPrecision{
            try{
                return LocalTimeLowPrecision(LocalTime.of(hour, minute, second, nanoOfSecond))
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage)
            }
        }

        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(hour: Int, minute: Int, decimalSecond: BigDecimal):  LocalTimeLowPrecision{
            if (decimalSecond.scale() > 9) {
                throw DateTimeException("Second precision exceed nano second")
            }
            val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
            val nano = decimalSecond.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            return of(hour, minute, wholeSecond.intValueExact(), nano.intValueExact())
        }
    }

    override val hour: Int = localTime.hour
    override val minute: Int = localTime.minute
    override val decimalSecond: BigDecimal =
        BigDecimal.valueOf(localTime.second.toLong())
            .plus(BigDecimal.valueOf(localTime.nano.toLong(), 9))

    val second: Int = localTime.second
    val nanoOfSecond: Int = localTime.nano

    override fun plusHours(hours: Long): LocalTimeLowPrecision = LocalTimeLowPrecision(localTime.plusHours(hours))


    override fun plusMinutes(minutes: Long): LocalTimeLowPrecision = LocalTimeLowPrecision(localTime.plusMinutes(minutes))

    override fun plusSeconds(seconds: Number): LocalTimeLowPrecision {
        val _seconds = seconds.toBigDecimal()
        if (_seconds.scale() > 9) {
            throw IllegalArgumentException("Second precision exceed nano second")
        }
        val wholeSecond = _seconds.setScale(0, RoundingMode.DOWN)
        val nano = _seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
        val newTime = localTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
        return LocalTimeLowPrecision(newTime)
    }

    override fun toPrecision(precision: Int): LocalTimeLowPrecision =
        when {
            decimalSecond.scale() == precision -> this
            decimalSecond.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    private fun roundToPrecision(precision: Int): LocalTimeLowPrecision {
        // if second fraction is 0.99999, precision 4
        // rounding this using half up will be 1.0000
        // diff is 0.0001
        // which means we need to add 0.0001 * 10^9 (100000)
        val decimalNano = decimalSecond.minus(this.second.toBigDecimal())
        val rounded = decimalNano.setScale(precision, RoundingMode.HALF_UP)
        val diff = rounded.minus(decimalNano).movePointRight(9).longValueExact()
        return LocalTimeLowPrecision(localTime.plusNanos(diff))
    }

    private fun paddingToPrecision(precision: Int) =
        of(hour, minute, decimalSecond.setScale(precision))

    override fun atDate(date: Date): Timestamp =
        LocalTimestampLowPrecision.forDateTime(date, this)

}
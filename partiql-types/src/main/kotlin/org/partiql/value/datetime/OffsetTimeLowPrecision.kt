package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.jvm.Throws

public data class OffsetTimeLowPrecision private constructor(
    val offsetTime: OffsetTime,
    val isUnknownTimeZone: Boolean
) : TimeWithTimeZone {
    public companion object {
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(
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

        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(hour: Int, minute: Int, decimalSecond: BigDecimal, timeZone: TimeZone): OffsetTimeLowPrecision {
            if (decimalSecond.scale() > 9) {
                throw DateTimeException("Second precision exceed nano second")
            }
            val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
            val nano = decimalSecond.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            return of(hour, minute, wholeSecond.intValueExact(), nano.intValueExact(), timeZone)
        }
    }

    override val hour: Int = offsetTime.hour

    override val minute: Int = offsetTime.minute

    override val decimalSecond: BigDecimal =
        BigDecimal.valueOf(offsetTime.second.toLong())
            .plus(BigDecimal.valueOf(offsetTime.nano.toLong(), 9))

    val second: Int = offsetTime.second
    val nanoOfSecond: Int = offsetTime.nano

    override val timeZone: TimeZone =
        if (isUnknownTimeZone) TimeZone.UnknownTimeZone else TimeZone.UtcOffset.of(offsetTime.offset.totalSeconds / 60)

    override fun plusHours(hours: Long): Time = OffsetTimeLowPrecision(offsetTime.plusHours(hours), isUnknownTimeZone)

    override fun plusMinutes(minutes: Long): Time =
        OffsetTimeLowPrecision(offsetTime.plusMinutes(minutes), isUnknownTimeZone)

    override fun plusSeconds(seconds: Number): Time {
        val _seconds = seconds.toBigDecimal()
        if (_seconds.scale() > 9) {
            throw IllegalArgumentException("Second precision exceed nano second")
        }
        val wholeSecond = _seconds.setScale(0, RoundingMode.DOWN)
        val nano = _seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
        val newTime = offsetTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
        return OffsetTimeLowPrecision(newTime, isUnknownTimeZone)
    }

    override fun toPrecision(precision: Int): Time =
        when {
            decimalSecond.scale() == precision -> this
            decimalSecond.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    private fun roundToPrecision(precision: Int): OffsetTimeLowPrecision {
        // if second fraction is 0.99999, precision 4
        // rounding this using half up will be 1.0000
        // diff is 0.0001
        // which means we need to add 0.0001 * 10^9 (100000)
        val decimalNano = decimalSecond.minus(this.second.toBigDecimal())
        val rounded = decimalNano.setScale(precision, RoundingMode.HALF_UP)
        val diff = rounded.minus(decimalNano).movePointRight(9).longValueExact()
        return OffsetTimeLowPrecision(offsetTime.plusNanos(diff), this.isUnknownTimeZone)
    }

    private fun paddingToPrecision(precision: Int) =
        of(hour, minute, decimalSecond.setScale(precision), timeZone)


    override fun atTimeZone(timeZone: TimeZone): TimeWithTimeZone =
        when (val valueTimeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this
                    is TimeZone.UtcOffset -> this.atTimeZone(TimeZone.UtcOffset.of(0)).atTimeZone(timeZone)
                }
            }

            is TimeZone.UtcOffset -> {
                val offsetTime = OffsetTime.of(
                    hour, minute, second, nanoOfSecond,
                    ZoneOffset.ofTotalSeconds(valueTimeZone.totalOffsetMinutes * 60)
                )
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> OffsetTimeLowPrecision(
                        offsetTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(0)),
                        true
                    )

                    is TimeZone.UtcOffset -> OffsetTimeLowPrecision(
                        offsetTime.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60)),
                        true
                    )
                }
            }
        }

    override fun atDate(date: Date): Timestamp {
        TODO("Not yet implemented")
    }

}
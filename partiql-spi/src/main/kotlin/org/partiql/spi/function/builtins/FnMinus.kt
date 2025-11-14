// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.utils.IntervalUtils.INTERVAL_MAX_PRECISION
import org.partiql.spi.utils.IntervalUtils.NANO_MAX_PRECISION
import org.partiql.spi.utils.NumberUtils.byteOverflows
import org.partiql.spi.utils.NumberUtils.shortOverflows
import org.partiql.spi.value.Datum
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

internal object FnMinus : DiadicArithmeticOperator("minus") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Fn {
        return basic(PType.tinyint()) { args ->
            val arg0 = args[0].byte
            val arg1 = args[1].byte
            val result = arg0 - arg1
            if (result.byteOverflows()) {
                throw PErrors.numericValueOutOfRangeException("$arg0 - $arg1", PType.tinyint())
            } else {
                Datum.tinyint(result.toByte())
            }
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Fn {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            val result = arg0 - arg1
            if (result.shortOverflows()) {
                throw PErrors.numericValueOutOfRangeException("$arg0 - $arg1", PType.smallint())
            } else {
                Datum.smallint(result.toShort())
            }
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Fn {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            try {
                val result = Math.subtractExact(arg0, arg1)
                Datum.integer(result)
            } catch (e: ArithmeticException) {
                throw PErrors.numericValueOutOfRangeException("$arg0 - $arg1", PType.integer())
            }
        }
    }

    override fun getIntervalInstance(lhs: PType, rhs: PType): Fn? {
        return when {
            lhs.code() == PType.INTERVAL_DT && rhs.code() == PType.INTERVAL_DT -> {
                val p: Int = INTERVAL_MAX_PRECISION // Based on SQL2023 6.44 SR 2)c), precision is implementation-defined. Set to max
                val s: Int = max(lhs.fractionalPrecision, rhs.fractionalPrecision) // Based on SQL2023 6.44 SR 2)c), set to max of lhs and rhs
                basic(PType.intervalDaySecond(p, s)) { args ->
                    val i1 = args[0]
                    val i2 = args[1]
                    Datum.intervalDaySecond(
                        i1.days - i2.days,
                        i1.hours - i2.hours,
                        i1.minutes - i2.minutes,
                        i1.seconds - i2.seconds,
                        i1.nanos - i2.nanos,
                        p,
                        s
                    )
                }
            }
            lhs.code() == PType.INTERVAL_YM && rhs.code() == PType.INTERVAL_YM -> {
                val p = INTERVAL_MAX_PRECISION // Based on SQL2023 6.44 SR 2)c), precision is implementation-defined. Set to max
                basic(PType.intervalYearMonth(p)) { args ->
                    val i1 = args[0]
                    val i2 = args[1]
                    Datum.intervalYearMonth(i1.years - i2.years, i1.months - i2.months, p)
                }
            }
            else -> null
        }
    }

    private fun getRemainder(value: Int, divisor: Int): Pair<Int, Int> {
        val remainder = value % divisor
        val quotient = value / divisor
        return remainder to quotient
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Fn {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            try {
                val result = Math.subtractExact(arg0, arg1)
                Datum.bigint(result)
            } catch (e: ArithmeticException) {
                throw PErrors.numericValueOutOfRangeException("$arg0 - $arg1", PType.bigint())
            }
        }
    }

    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Fn {
        val (p, s) = minusPrecisionScale(numericLhs, numericRhs)
        return Function.instance(
            name = signature.name,
            returns = PType.numeric(p, s),
            parameters = arrayOf(
                Parameter("lhs", numericLhs),
                Parameter("rhs", numericRhs),
            )
        ) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.numeric(arg0 - arg1, p, s)
        }
    }

    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Fn {
        val (p, s) = minusPrecisionScale(decimalLhs, decimalRhs)
        return Function.instance(
            name = signature.name,
            returns = PType.decimal(p, s),
            parameters = arrayOf(
                Parameter("lhs", decimalLhs),
                Parameter("rhs", decimalRhs),
            )
        ) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.decimal(arg0 - arg1, p, s)
        }
    }

    /**
     * P = max(s1, s2) + max(p1 - s1, p2 - s2) + 1
     * S = max(s1, s2)
     */
    private fun minusPrecisionScale(lhs: PType, rhs: PType): Pair<Int, Int> {
        val (p1, s1) = lhs.precision to lhs.scale
        val (p2, s2) = rhs.precision to rhs.scale
        val p = s1.coerceAtLeast(s2) + (p1 - s1).coerceAtLeast(p2 - s2) + 1
        val s = s1.coerceAtLeast(s2)
        val returnedP = p.coerceAtMost(38)
        val returnedS = s.coerceAtMost(p)
        return returnedP to returnedS
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Fn {
        return basic(PType.real()) { args ->
            val arg0 = args[0].float
            val arg1 = args[1].float
            Datum.real((arg0 - arg1))
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Fn {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            Datum.doublePrecision((arg0 - arg1))
        }
    }

    override fun getDateInstance(dateLhs: PType, dateRhs: PType): Fn {
        return basic(PType.intervalDay(INTERVAL_MAX_PRECISION), dateLhs, dateRhs) { args ->
            val arg0 = args[0].localDate
            val arg1 = args[1].localDate
            val dayDiff = arg0.toEpochDay() - arg1.toEpochDay()
            Datum.intervalDay(dayDiff.toInt(), INTERVAL_MAX_PRECISION)
        }
    }

    override fun getTimeInstance(timeLhs: PType, timeRhs: PType): Fn {
        return basic(PType.intervalSecond(INTERVAL_MAX_PRECISION, NANO_MAX_PRECISION), timeLhs, timeRhs) { args ->
            val arg0 = args[0].localTime
            val arg1 = args[1].localTime
            val result = arg0
                .minus(arg1.hour.toLong(), ChronoUnit.HOURS)
                .minus(arg1.minute.toLong(), ChronoUnit.MINUTES)
                .minus(arg1.second.toLong(), ChronoUnit.SECONDS)
                .minus(arg1.nano.toLong(), ChronoUnit.NANOS)
            val resultSeconds = result.toSecondOfDay()
            Datum.intervalSecond(resultSeconds, result.nano, INTERVAL_MAX_PRECISION, NANO_MAX_PRECISION)
        }
    }

    override fun getTimezInstance(timezLhs: PType, timezRhs: PType): Fn {
        return basic(PType.intervalSecond(INTERVAL_MAX_PRECISION, NANO_MAX_PRECISION), timezLhs, timezRhs) { args ->
            val arg0 = args[0].localTime
            val arg1 = args[1].localTime
            val result = arg0
                .minus(arg1.hour.toLong(), ChronoUnit.HOURS)
                .minus(arg1.minute.toLong(), ChronoUnit.MINUTES)
                .minus(arg1.second.toLong(), ChronoUnit.SECONDS)
                .minus(arg1.nano.toLong(), ChronoUnit.NANOS)
            val resultSeconds = result.toSecondOfDay()
            Datum.intervalSecond(resultSeconds, result.nano, INTERVAL_MAX_PRECISION, NANO_MAX_PRECISION)
        }
    }

    override fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Fn {
        return basic(PType.intervalDaySecond(INTERVAL_MAX_PRECISION, NANO_MAX_PRECISION), timestampLhs, timestampRhs) { args ->
            val arg0 = args[0].localDateTime
            val arg1 = args[1].localDateTime
            val days = ChronoUnit.DAYS.between(arg1, arg0)
            val arg0Time = arg0.toLocalTime()
            val arg1Time = arg1.toLocalTime()
            val resultTime = arg0Time
                .minus(arg1Time.hour.toLong(), ChronoUnit.HOURS)
                .minus(arg1Time.minute.toLong(), ChronoUnit.MINUTES)
                .minus(arg1Time.second.toLong(), ChronoUnit.SECONDS)
                .minus(arg1Time.nano.toLong(), ChronoUnit.NANOS)
            Datum.intervalDaySecond(
                days.toInt(),
                resultTime.hour,
                resultTime.minute,
                resultTime.second,
                resultTime.nano,
                INTERVAL_MAX_PRECISION,
                NANO_MAX_PRECISION
            )
        }
    }

    override fun getTimestampzInstance(timestampzLhs: PType, timestampzRhs: PType): Fn {
        return basic(PType.intervalDaySecond(INTERVAL_MAX_PRECISION, NANO_MAX_PRECISION), timestampzLhs, timestampzRhs) { args ->
            val arg0 = args[0].offsetDateTime
            val arg1 = args[1].offsetDateTime
            val days = ChronoUnit.DAYS.between(arg1, arg0)
            val arg0Time = arg0.toLocalTime()
            val arg1Time = arg1.toLocalTime()
            val resultTime = arg0Time
                .minus(arg1Time.hour.toLong(), ChronoUnit.HOURS)
                .minus(arg1Time.minute.toLong(), ChronoUnit.MINUTES)
                .minus(arg1Time.second.toLong(), ChronoUnit.SECONDS)
                .minus(arg1Time.nano.toLong(), ChronoUnit.NANOS)
            Datum.intervalDaySecond(
                days.toInt(),
                resultTime.hour,
                resultTime.minute,
                resultTime.second,
                resultTime.nano,
                INTERVAL_MAX_PRECISION,
                NANO_MAX_PRECISION
            )
        }
    }

    override fun getDateIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op: (LocalDate, Datum) -> LocalDate = when (rhs.intervalCode) {
            IntervalCode.YEAR, IntervalCode.MONTH, IntervalCode.YEAR_MONTH -> { date, i -> date.minusMonths(i.totalMonths) }
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> { date, i -> date.minusDays(i.days.toLong()) }
            else -> return null
        }
        return basic(PType.date(), lhs, rhs) { args ->
            val lhs = args[0].localDate
            val rhs = args[1]
            val result: LocalDate = op(lhs, rhs)
            Datum.date(result)
        }
    }

    override fun getTimeIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op: (LocalTime, Datum) -> LocalTime = when (rhs.intervalCode) {
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> { time, i ->
                time - Duration.ofSeconds(i.totalSeconds, i.nanos.toLong())
            }
            else -> return null
        }
        val lhsPrecision = lhs.precision
        return basic(lhs, lhs, rhs) { args ->
            val time = args[0].localTime
            val interval = args[1]
            val result: LocalTime = op(time, interval)

            if (lhs.code() == PType.TIMEZ) {
                val originalOffset = args[0].offsetTime.offset
                Datum.timez(result.atOffset(originalOffset), lhs.precision)
            } else {
                Datum.time(result, lhs.precision)
            }
        }
    }

    override fun getTimestampIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op: (LocalDateTime, Datum) -> LocalDateTime = when (rhs.intervalCode) {
            IntervalCode.YEAR, IntervalCode.MONTH, IntervalCode.YEAR_MONTH -> { time, i -> time.minusMonths(i.totalMonths) }
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> { time, i -> time - Duration.ofSeconds(i.totalSeconds, i.nanos.toLong()) }
            else -> return null
        }
        val lhsPrecision = lhs.precision
        return basic(lhs, lhs, rhs) { args ->
            val timestamp = args[0].localDateTime
            val interval = args[1]
            val result: LocalDateTime = op(timestamp, interval)
            if (lhs.code() == PType.TIMESTAMPZ) {
                val originalOffset = args[0].offsetTime.offset
                Datum.timestampz(result.atOffset(originalOffset), lhs.precision)
            } else {
                Datum.timestamp(result, lhs.precision)
            }
        }
    }
}

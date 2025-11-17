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
import org.partiql.spi.utils.NumberUtils.byteOverflows
import org.partiql.spi.utils.NumberUtils.shortOverflows
import org.partiql.spi.value.Datum
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.max

internal object FnPlus : DiadicArithmeticOperator("plus") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Fn {
        return basic(PType.tinyint()) { args ->
            val arg0 = args[0].byte
            val arg1 = args[1].byte
            val result = arg0 + arg1
            if (result.byteOverflows()) {
                throw PErrors.numericValueOutOfRangeException("$arg0 + $arg1", PType.tinyint())
            } else {
                Datum.tinyint(result.toByte())
            }
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Fn {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            val result = arg0 + arg1
            if (result.shortOverflows()) {
                throw PErrors.numericValueOutOfRangeException("$arg0 + $arg1", PType.smallint())
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
                val result = Math.addExact(arg0, arg1)
                Datum.integer(result)
            } catch (e: ArithmeticException) {
                throw PErrors.numericValueOutOfRangeException("$arg0 + $arg1", PType.integer())
            }
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Fn {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            try {
                val result = Math.addExact(arg0, arg1)
                Datum.bigint(result)
            } catch (e: ArithmeticException) {
                throw PErrors.numericValueOutOfRangeException("$arg0 + $arg1", PType.bigint())
            }
        }
    }

    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Fn {
        val (p, s) = plusPrecisionScale(numericLhs, numericRhs)
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
            Datum.numeric(arg0 + arg1, p, s)
        }
    }

    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Fn {
        val (p, s) = plusPrecisionScale(decimalLhs, decimalRhs)
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
            Datum.decimal(arg0 + arg1, p, s)
        }
    }

    /**
     * Precision and scale calculation:
     * P = max(s1, s2) + max(p1 - s1, p2 - s2) + 1
     * S = max(s1, s2)
     */
    fun plusPrecisionScale(lhs: PType, rhs: PType): Pair<Int, Int> {
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
            Datum.real(arg0 + arg1)
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Fn {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            Datum.doublePrecision(arg0 + arg1)
        }
    }

    override fun getDateIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op = localDatePlusIntervalInstance(rhs) ?: return null
        return basic(PType.date(), lhs, rhs) { args ->
            val date = args[0].localDate
            val interval = args[1]
            val result: LocalDate = op(date, interval)
            Datum.date(result)
        }
    }

    override fun getIntervalDateInstance(lhs: PType, rhs: PType): Fn? {
        val op = localDatePlusIntervalInstance(lhs) ?: return null
        return basic(PType.date(), lhs, rhs) { args ->
            val interval = args[0]
            val date = args[1].localDate
            val result: LocalDate = op(date, interval)
            Datum.date(result)
        }
    }

    override fun getTimeIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op = localTimePlusIntervalInstance(rhs) ?: return null
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

    override fun getIntervalTimeInstance(lhs: PType, rhs: PType): Fn? {
        val op = localTimePlusIntervalInstance(lhs) ?: return null
        return basic(rhs, lhs, rhs) { args ->
            val interval = args[0]
            val time = args[1].localTime
            val result: LocalTime = op(time, interval)

            if (rhs.code() == PType.TIMEZ) {
                val originalOffset = args[1].offsetTime.offset
                Datum.timez(result.atOffset(originalOffset), rhs.precision)
            } else {
                Datum.time(result, rhs.precision)
            }
        }
    }

    override fun getTimestampIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op = localDateTimePlusIntervalInstance(rhs) ?: return null
        return basic(lhs, lhs, rhs) { args ->
            val timestamp = args[0].localDateTime
            val interval = args[1]
            val result: LocalDateTime = op(timestamp, interval)

            if (lhs.code() == PType.TIMESTAMPZ) {
                val originalOffset = args[0].offsetDateTime.offset
                Datum.timestampz(result.atOffset(originalOffset), lhs.precision)
            } else {
                Datum.timestamp(result, lhs.precision)
            }
        }
    }

    override fun getIntervalTimestampInstance(lhs: PType, rhs: PType): Fn? {
        val op = localDateTimePlusIntervalInstance(lhs) ?: return null
        return basic(rhs, lhs, rhs) { args ->
            val interval = args[0]
            val timestamp = args[1].localDateTime
            val result: LocalDateTime = op(timestamp, interval)
            if (rhs.code() == PType.TIMESTAMPZ) {
                val originalOffset = args[1].offsetDateTime.offset
                Datum.timestampz(result.atOffset(originalOffset), rhs.precision)
            } else {
                Datum.timestamp(result, rhs.precision)
            }
        }
    }

    private fun localDateTimePlusIntervalInstance(interval: PType): ((LocalDateTime, Datum) -> LocalDateTime)? {
        return when (interval.intervalCode) {
            IntervalCode.YEAR, IntervalCode.MONTH, IntervalCode.YEAR_MONTH -> { time, i -> time.plusMonths(i.totalMonths) }
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> { time, i -> time + Duration.ofSeconds(i.totalSeconds, i.nanos.toLong()) }
            else -> return null
        }
    }

    private fun localTimePlusIntervalInstance(interval: PType): ((LocalTime, Datum) -> LocalTime)? {
        return when (interval.intervalCode) {
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> { time, i -> time + Duration.ofSeconds(i.totalSeconds, i.nanos.toLong()) }
            else -> return null
        }
    }

    private fun localDatePlusIntervalInstance(interval: PType): ((LocalDate, Datum) -> LocalDate)? {
        return when (interval.intervalCode) {
            IntervalCode.YEAR, IntervalCode.MONTH, IntervalCode.YEAR_MONTH -> { date, i -> date.plusMonths(i.totalMonths) }
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> { date, i -> date.plusDays(i.days.toLong()) }
            else -> return null
        }
    }

    override fun getIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val lhsCode = lhs.intervalCode
        val rhsCode = rhs.intervalCode
        return when {
            isDayTimeInterval(lhsCode) && isDayTimeInterval(rhsCode) -> {
                val p: Int = INTERVAL_MAX_PRECISION // Based on SQL2023 6.44 SR 2)c), precision is implementation-defined. Set to max
                val s: Int = max(lhs.fractionalPrecision, rhs.fractionalPrecision) // Based on SQL2023 6.44 SR 2)c), set to max of lhs and rhs
                basic(PType.intervalDaySecond(p, s)) { args ->
                    val i1 = args[0]
                    val i2 = args[1]
                    Datum.intervalDaySecond(
                        i1.days + i2.days,
                        i1.hours + i2.hours,
                        i1.minutes + i2.minutes,
                        i1.seconds + i2.seconds,
                        i1.nanos + i2.nanos,
                        p,
                        s
                    )
                }
            }
            isYearMonthInterval(lhsCode) && isYearMonthInterval(rhsCode) -> {
                val p = INTERVAL_MAX_PRECISION // Based on SQL2023 6.44 SR 2)c), precision is implementation-defined. Set to max
                basic(PType.intervalYearMonth(p)) { args ->
                    val i1 = args[0]
                    val i2 = args[1]
                    Datum.intervalYearMonth(
                        i1.years + i2.years,
                        i1.months + i2.months,
                        p
                    )
                }
            }
            else -> null
        }
    }

    private fun isYearMonthInterval(code: Int): Boolean {
        return when (code) {
            IntervalCode.YEAR,
            IntervalCode.MONTH,
            IntervalCode.YEAR_MONTH -> true
            else -> false
        }
    }

    private fun isDayTimeInterval(code: Int): Boolean {
        return when (code) {
            IntervalCode.DAY,
            IntervalCode.HOUR,
            IntervalCode.MINUTE,
            IntervalCode.SECOND,
            IntervalCode.DAY_HOUR,
            IntervalCode.DAY_MINUTE,
            IntervalCode.DAY_SECOND,
            IntervalCode.HOUR_MINUTE,
            IntervalCode.HOUR_SECOND,
            IntervalCode.MINUTE_SECOND -> true
            else -> false
        }
    }

    private fun getRemainder(value: Int, divisor: Int): Pair<Int, Int> {
        val remainder = value % divisor
        val quotient = value / divisor
        return remainder to quotient
    }
}

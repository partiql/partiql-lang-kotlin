// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.utils.NumberUtils.byteOverflows
import org.partiql.spi.utils.NumberUtils.shortOverflows
import org.partiql.spi.value.Datum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

internal object FnMinus : DiadicArithmeticOperator("minus") {

    init {
        fillTable()
    }

    private const val INTERVAL_MAX_PRECISION = 9
    private const val INTERVAL_MAX_SCALE = 9

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
                val p: Int = lhs.precision // TODO: Do we need to calculate a new precision?
                val s: Int = 6 // TODO: Do we need to calculate a new fractional precision?
                basic(PType.intervalDaySecond(p, s)) { args ->
                    val interval0 = args[0]
                    val interval1 = args[1]
                    subtractIntervalDayTimes(interval0, interval1, p, s)
                }
            }
            lhs.code() == PType.INTERVAL_YM && rhs.code() == PType.INTERVAL_YM -> {
                val p: Int = lhs.precision // TODO: Do we need to calculate a new precision?
                basic(PType.intervalYearMonth(p)) { args ->
                    val interval0 = args[0]
                    val interval1 = args[1]
                    subtractIntervalYearMonths(interval0, interval1, p)
                }
            }
            else -> null
        }
    }

    private fun subtractIntervalYearMonths(lhs: Datum, rhs: Datum, precision: Int): Datum {
        val (months, yearsRemainder) = getRemainder(lhs.months - rhs.months, 12)
        val years = lhs.years - rhs.years - yearsRemainder
        return Datum.intervalYearMonth(years, months, precision)
    }

    private fun subtractIntervalDayTimes(lhs: Datum, rhs: Datum, precision: Int, scale: Int): Datum {
        val (nanos, secondsRemainder) = getRemainder(lhs.nanos - rhs.nanos, 1_000_000_000)
        val (seconds, minutesRemainder) = getRemainder(lhs.seconds - rhs.seconds - secondsRemainder, 60)
        val (minutes, hoursRemainder) = getRemainder(lhs.minutes - rhs.minutes - minutesRemainder, 60)
        val (hours, daysRemainder) = getRemainder(lhs.hours - rhs.hours - hoursRemainder, 12)
        val days = lhs.days - rhs.days - daysRemainder
        return Datum.intervalDaySecond(days, hours, minutes, seconds, nanos, precision, scale)
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
        return basic(PType.intervalSecond(INTERVAL_MAX_PRECISION, INTERVAL_MAX_SCALE), timeLhs, timeRhs) { args ->
            val arg0 = args[0].localTime
            val arg1 = args[1].localTime
            val result = arg0
                .minus(arg1.hour.toLong(), ChronoUnit.HOURS)
                .minus(arg1.minute.toLong(), ChronoUnit.MINUTES)
                .minus(arg1.second.toLong(), ChronoUnit.SECONDS)
                .minus(arg1.nano.toLong(), ChronoUnit.NANOS)
            val resultSeconds = result.toSecondOfDay()
            Datum.intervalSecond(resultSeconds, result.nano, INTERVAL_MAX_PRECISION, INTERVAL_MAX_SCALE)
        }
    }

    override fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Fn {
        return basic(PType.intervalDaySecond(INTERVAL_MAX_PRECISION, INTERVAL_MAX_SCALE), timestampLhs, timestampRhs) { args ->
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
                INTERVAL_MAX_SCALE
            )
        }
    }

    override fun getDateIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op: (LocalDate, Datum) -> LocalDate = when (rhs.intervalCode) {
            IntervalCode.YEAR -> { date, i -> date.minusYears(i.years.toLong()) }
            IntervalCode.MONTH -> { date, i -> date.minusMonths(i.months.toLong()) }
            IntervalCode.DAY -> { date, i -> date.minusDays(i.days.toLong()) }
            IntervalCode.HOUR -> { date, i -> date.minusDays(i.hours.toLong() / 24L) }
            IntervalCode.MINUTE -> { date, i -> date.minusDays(i.minutes.toLong() / (24L * 60L)) }
            IntervalCode.SECOND -> { date, i -> date.minusDays(i.seconds.toLong() / (24L * 60L * 60L)) }
            IntervalCode.YEAR_MONTH -> { date, i -> date.minusYears(i.years.toLong()).minusMonths(i.months.toLong()) }
            IntervalCode.DAY_HOUR -> { date, i -> date.minusDays(i.days.toLong()).minusDays(i.hours.toLong() / 24L) }
            IntervalCode.DAY_MINUTE -> { date, i -> date.minusDays(i.days.toLong()).minusDays(i.hours.toLong() / 24L).minusDays(i.minutes.toLong() / (24L * 60L)) }
            IntervalCode.DAY_SECOND -> { date, i -> date.minusDays(i.days.toLong()).minusDays(i.hours.toLong() / 24L).minusDays(i.minutes.toLong() / (24L * 60L)).minusDays(i.seconds.toLong() / (24L * 60L * 60L)) }
            IntervalCode.HOUR_MINUTE -> { date, i -> date.minusDays(i.hours.toLong() / 24L).minusDays(i.minutes.toLong() / (24L * 60L)) }
            IntervalCode.HOUR_SECOND -> { date, i -> date.minusDays(i.hours.toLong() / 24L).minusDays(i.minutes.toLong() / (24L * 60L)).minusDays(i.seconds.toLong() / (24L * 60L * 60L)) }
            IntervalCode.MINUTE_SECOND -> { date, i -> date.minusDays(i.minutes.toLong() / (24L * 60L)).minusDays(i.seconds.toLong() / (24L * 60L * 60L)) }
            else -> return null
        }
        return basic(PType.date(), lhs, rhs) { args ->
            val arg0 = args[0].localDate
            val arg1 = args[1]
            val result: LocalDate = op(arg0, arg1)
            Datum.date(result)
        }
    }

    override fun getTimeIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op: (LocalTime, Datum) -> LocalTime = when (rhs.intervalCode) {
            IntervalCode.DAY -> { time, _ -> time }
            IntervalCode.HOUR -> { time, i -> time.minusHours(i.hours.toLong()) }
            IntervalCode.MINUTE -> { time, i -> time.minusMinutes(i.minutes.toLong()) }
            IntervalCode.SECOND -> { time, i -> time.minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.DAY_HOUR -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()) }
            IntervalCode.DAY_MINUTE -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.DAY_SECOND -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.HOUR_MINUTE -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()) }
            IntervalCode.HOUR_SECOND -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.MINUTE_SECOND -> { time, i -> time.minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            else -> return null
        }
        val lhsPrecision = lhs.precision
        return basic(lhs, lhs, rhs) { args ->
            val arg0 = args[0].localTime
            val arg1 = args[1]
            val result: LocalTime = op(arg0, arg1)
            Datum.time(result, lhsPrecision)
        }
    }

    override fun getTimestampIntervalInstance(lhs: PType, rhs: PType): Fn? {
        val op: (LocalDateTime, Datum) -> LocalDateTime = when (rhs.intervalCode) {
            IntervalCode.YEAR -> { time, i -> time.minusYears(i.years.toLong()) }
            IntervalCode.MONTH -> { time, i -> time.minusMonths(i.months.toLong()) }
            IntervalCode.DAY -> { time, i -> time.minusDays(i.days.toLong()) }
            IntervalCode.HOUR -> { time, i -> time.minusHours(i.hours.toLong()) }
            IntervalCode.MINUTE -> { time, i -> time.minusMinutes(i.minutes.toLong()) }
            IntervalCode.SECOND -> { time, i -> time.minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.YEAR_MONTH -> { time, i -> time.minusYears(i.years.toLong()).minusMonths(i.months.toLong()) }
            IntervalCode.DAY_HOUR -> { time, i -> time.minusDays(i.days.toLong()).minusHours(i.hours.toLong()) }
            IntervalCode.DAY_MINUTE -> { time, i -> time.minusDays(i.days.toLong()).minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()) }
            IntervalCode.DAY_SECOND -> { time, i -> time.minusDays(i.days.toLong()).minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.HOUR_MINUTE -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()) }
            IntervalCode.HOUR_SECOND -> { time, i -> time.minusHours(i.hours.toLong()).minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            IntervalCode.MINUTE_SECOND -> { time, i -> time.minusMinutes(i.minutes.toLong()).minusSeconds(i.seconds.toLong()).minusNanos(i.nanos.toLong()) }
            else -> return null
        }
        val lhsPrecision = lhs.precision
        return basic(lhs, lhs, rhs) { args ->
            val arg0 = args[0].localDateTime
            val arg1 = args[1]
            val result: LocalDateTime = op(arg0, arg1)
            Datum.timestamp(result, lhsPrecision)
        }
    }
}

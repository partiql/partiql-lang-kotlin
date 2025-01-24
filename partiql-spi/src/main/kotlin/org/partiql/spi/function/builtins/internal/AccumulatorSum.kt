package org.partiql.spi.function.builtins.internal

import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils.checkIsNumberType
import org.partiql.spi.utils.NumberUtils.AccumulatorType
import org.partiql.spi.utils.NumberUtils.MATH_CONTEXT
import org.partiql.spi.utils.NumberUtils.add
import org.partiql.spi.utils.NumberUtils.doubleValue
import org.partiql.spi.utils.NumberUtils.longValue
import org.partiql.spi.utils.NumberUtils.toTargetType
import org.partiql.spi.value.Datum
import java.math.BigDecimal

internal class AccumulatorSumBigInt : Accumulator() {
    var sum: Long = 0L
    var init = false
    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (!init) {
            init = true
        }
        val longValue = value.longValue()
        sum = try {
            Math.addExact(sum, longValue)
        } catch (e: ArithmeticException) {
            // In case of overflow, give a data exception
            throw PErrors.numericValueOutOfRangeException("$sum + $longValue", PType.bigint())
        }
    }

    override fun value(): Datum {
        return if (init) Datum.bigint(sum) else Datum.nullValue(PType.bigint())
    }
}

internal class AccumulatorSumDecimal(
    private val targetType: PType = PType.dynamic()
) : Accumulator() {
    var sum: BigDecimal = BigDecimal.ZERO
    var init = false

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (!init) {
            init = true
        }
        val arg1 = value.bigDecimal
        sum = sum.add(arg1, MATH_CONTEXT)
    }

    override fun value(): Datum {
        return if (init) sum.toTargetType(targetType) else Datum.nullValue(targetType)
    }
}

internal class AccumulatorSumDouble : Accumulator() {
    var sum: Double = 0.0
    var init = false
    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (!init) {
            init = true
        }
        val arg1 = value.doubleValue()
        sum += arg1
    }

    override fun value(): Datum {
        return if (init) Datum.doublePrecision(sum) else Datum.nullValue(PType.doublePrecision())
    }
}

internal class AccumulatorSumDynamic : Accumulator() {
    var sum: Number? = null
    private var accumulatorType: AccumulatorType? = null

    // TODO: need to take another look at variant lowering and casing here
    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "SUM", value = value)
        // Initialize `sum` and `accumulatorType`
        if (sum == null) {
            sum = when (value.type.code()) {
                PType.TINYINT, PType.SMALLINT, PType.INTEGER -> {
                    accumulatorType = AccumulatorType.INTEGRAL
                    0L
                }
                PType.REAL, PType.DOUBLE -> {
                    accumulatorType = AccumulatorType.APPROX
                    0.0
                }
                PType.BIGINT, PType.DECIMAL, PType.NUMERIC -> {
                    accumulatorType = AccumulatorType.DECIMAL
                    BigDecimal.ZERO
                }
                else -> error("Unexpected type: ${value.type}")
            }
        } else {
            // Update our `accumulatorType` if value is decimal or approximate
            when (value.type.code()) {
                PType.REAL, PType.DOUBLE -> {
                    accumulatorType = AccumulatorType.APPROX
                }
                PType.BIGINT, PType.DECIMAL, PType.NUMERIC -> {
                    if (accumulatorType!! != AccumulatorType.APPROX) {
                        accumulatorType = AccumulatorType.DECIMAL
                    }
                }
            }
        }
        sum = add(sum!!, value, accumulatorType!!)
    }

    override fun value(): Datum {
        return sum?.toTargetType(PType.dynamic()) ?: Datum.nullValue(PType.dynamic())
    }
}

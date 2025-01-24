package org.partiql.spi.function.builtins.internal

import org.partiql.spi.function.builtins.DefaultDecimal
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils.checkIsNumberType
import org.partiql.spi.utils.NumberUtils.AccumulatorType
import org.partiql.spi.utils.NumberUtils.MATH_CONTEXT
import org.partiql.spi.utils.NumberUtils.add
import org.partiql.spi.utils.NumberUtils.bigDecimalOf
import org.partiql.spi.utils.NumberUtils.numberValue
import org.partiql.spi.utils.NumberUtils.toTargetType
import org.partiql.spi.value.Datum
import java.math.BigDecimal

internal class AccumulatorAvgDecimal : Accumulator() {
    private var sum: BigDecimal = BigDecimal.ZERO
    private var count: Long = 0L
    private var init = false

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "AVG", value = value)
        if (!init) {
            init = true
        }
        val arg1 = bigDecimalOf(value.numberValue(), MATH_CONTEXT)
        sum = sum.add(arg1, MATH_CONTEXT)
        count += 1L
    }

    override fun value(): Datum = when (count) {
        0L -> Datum.nullValue(DefaultDecimal.DECIMAL)
        else -> Datum.decimal(bigDecimalOf(sum).divide(bigDecimalOf(count), MATH_CONTEXT))
    }
}

internal class AccumulatorAvgDouble : Accumulator() {
    private var sum: Double = 0.0
    private var count: Long = 0L
    private var init = false

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "AVG", value = value)
        val arg1 = value.double
        if (!init) {
            init = true
        }
        sum += arg1
        count += 1L
    }

    override fun value(): Datum = when (count) {
        0L -> Datum.nullValue(PType.doublePrecision())
        else -> Datum.doublePrecision(sum / count.toDouble())
    }
}

internal class AccumulatorAvgDynamic : Accumulator() {
    private var sum: Number? = null
    private var count: Long = 0L
    private var accumulatorType: AccumulatorType? = null

    // TODO: need to take another look at variant lowering and casing here
    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "AVG", value = value)
        if (sum == null) {
            sum = when (value.type.code()) {
                PType.REAL, PType.DOUBLE -> {
                    accumulatorType = AccumulatorType.APPROX
                    0.0
                }
                else -> {
                    accumulatorType = AccumulatorType.DECIMAL
                    BigDecimal.ZERO
                }
            }
        } else {
            if (value.type.code() == PType.REAL || value.type.code() == PType.DOUBLE) {
                accumulatorType = AccumulatorType.APPROX
            }
        }
        sum = add(sum!!, value, accumulatorType!!)
        count += 1L
    }

    override fun value(): Datum = when (count) {
        0L -> Datum.nullValue(PType.dynamic())
        else -> {
            when (sum) {
                is BigDecimal -> {
                    bigDecimalOf(sum!!).divide(bigDecimalOf(count), MATH_CONTEXT)
                }
                is Double -> {
                    (sum!!.toDouble()) / count.toDouble()
                }
                else -> error("Sum should be BigDecimal or Double")
            }.toTargetType(PType.dynamic())
        }
    }
}

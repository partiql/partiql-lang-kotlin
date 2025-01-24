package org.partiql.spi.function.builtins.internal

import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils.checkIsNumberType
import org.partiql.spi.utils.FunctionUtils.nullToTargetType
import org.partiql.spi.utils.NumberUtils.MATH_CONTEXT
import org.partiql.spi.utils.NumberUtils.add
import org.partiql.spi.utils.NumberUtils.bigDecimalOf
import org.partiql.spi.utils.NumberUtils.toTargetType
import org.partiql.spi.value.Datum
import java.math.BigDecimal

// TODO docs + further cleanup
internal class AccumulatorAvg(
    private val targetType: PType = PType.dynamic(),
) : Accumulator() {
    private var sum: Number? = null
    private var count: Long = 0L
    private var dynamicSumType: PType? = targetType

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "AVG", value = value)
        when (targetType.code()) {
            PType.DECIMAL -> {
                if (sum == null) {
                    sum = BigDecimal.ZERO
                }
            }
            PType.DOUBLE -> {
                if (sum == null) {
                    sum = 0.0
                }
            }
            PType.DYNAMIC -> if (sum == null) {
                dynamicSumType = when (value.type.code()) {
                    PType.REAL, PType.DOUBLE -> {
                        sum = BigDecimal.ZERO
                        PType.doublePrecision()
                    }
                    PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT, PType.DECIMAL, PType.NUMERIC -> {
                        sum = BigDecimal.ZERO
                        PType.decimal()
                    }
                    else -> error("Unexpected type: ${value.type}")
                }
            } else {
                when (value.type.code()) {
                    PType.REAL, PType.DOUBLE -> {
                        dynamicSumType = PType.doublePrecision()
                    }
                }
            }
        }
        sum = add(sum!!, value, dynamicSumType!!)
        count += 1L
    }

    override fun value(): Datum = when (count) {
        0L -> nullToTargetType(targetType)
        else -> {
            when (sum) {
                is BigDecimal -> {
                    bigDecimalOf(sum!!).divide(bigDecimalOf(count), MATH_CONTEXT)
                }
                is Double -> {
                    (sum!!.toDouble()) / count.toDouble()
                }
                else -> error("Sum should be BigDecimal or Double")
            }.toTargetType(targetType)
        }
    }
}

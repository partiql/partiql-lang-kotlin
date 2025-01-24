package org.partiql.spi.function.builtins.internal

import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils.checkIsNumberType
import org.partiql.spi.utils.FunctionUtils.nullToTargetType
import org.partiql.spi.utils.NumberUtils.add
import org.partiql.spi.utils.NumberUtils.toTargetType
import org.partiql.spi.value.Datum
import java.math.BigDecimal

// TODO docs + further cleanup
internal class AccumulatorSum(
    private var targetType: PType = PType.dynamic(),
) : Accumulator() {

    var sum: Number? = null
    private var dynamicSumType: PType? = targetType

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "SUM", value = value)
        when (targetType.code()) {
            PType.BIGINT -> {
                if (sum == null) {
                    sum = 0L
                }
            }
            PType.DECIMAL, PType.NUMERIC -> {
                if (sum == null) {
                    sum = BigDecimal.ZERO
                }
            }
            PType.DOUBLE -> {
                if (sum == null) {
                    sum = 0.0
                }
            }
            PType.DYNAMIC -> {
                if (sum == null) {
                    dynamicSumType = when (value.type.code()) {
                        PType.TINYINT, PType.SMALLINT, PType.INTEGER -> {
                            sum = 0L
                            PType.bigint()
                        }
                        PType.REAL, PType.DOUBLE -> {
                            sum = BigDecimal.ZERO
                            PType.doublePrecision()
                        }
                        PType.BIGINT, PType.DECIMAL, PType.NUMERIC -> {
                            sum = BigDecimal.ZERO
                            PType.decimal()
                        }
                        else -> error("Unexpected type: ${value.type.code()}")
                    }
                } else {
                    when (value.type.code()) {
                        PType.REAL, PType.DOUBLE -> {
                            dynamicSumType = PType.doublePrecision()
                        }
                        PType.BIGINT, PType.DECIMAL, PType.NUMERIC -> {
                            if (dynamicSumType!!.code() != PType.DOUBLE) {
                                dynamicSumType = PType.decimal()
                            }
                        }
                    }
                }
            }
        }
        sum = add(sum!!, value, dynamicSumType!!)
    }

    override fun value(): Datum {
        return sum?.toTargetType(targetType) ?: nullToTargetType(targetType)
    }
}

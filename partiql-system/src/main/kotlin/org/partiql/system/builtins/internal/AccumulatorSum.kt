package org.partiql.system.builtins.internal

import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class AccumulatorSum(
    private val targetType: PType = PType.dynamic(),
) : Accumulator() {

    var sum: Number? = null

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (sum == null) sum = 0L
        this.sum = value.numberValue() + this.sum!!
    }

    override fun value(): Datum {
        return sum?.toTargetType(targetType) ?: nullToTargetType(targetType)
    }
}

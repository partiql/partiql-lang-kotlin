package org.partiql.eval.internal.routines.internal

import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.PartiQLValueType


internal class AccumulatorSum(
    private val targetType: PartiQLValueType = PartiQLValueType.DYNAMIC
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

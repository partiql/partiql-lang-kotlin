package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.nullValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorSum(
    private val targetType: PartiQLValueType = PartiQLValueType.ANY
) : Accumulator() {

    var sum: Number? = null

    @OptIn(PartiQLValueExperimental::class)
    override fun nextValue(value: PartiQLValue) {
        checkIsNumberType(funcName = "SUM", value = value)
        if (sum == null) sum = 0L
        this.sum = value.numberValue() + this.sum!!
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun value(): PartiQLValue {
        return sum?.toTargetType(targetType) ?: nullValue()
    }
}

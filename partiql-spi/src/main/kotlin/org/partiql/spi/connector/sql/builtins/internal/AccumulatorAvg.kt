package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.nullValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorAvg(
    private val targetType: PartiQLValueType = PartiQLValueType.ANY
) : Accumulator() {

    var sum: Number = 0.0
    var count: Long = 0L

    override fun nextValue(value: PartiQLValue) {
        checkIsNumberType(funcName = "AVG", value = value)
        this.sum += value.numberValue()
        this.count += 1L
    }

    override fun value(): PartiQLValue = when (count) {
        0L -> nullValue()
        else -> (sum / bigDecimalOf(count)).toTargetType(targetType)
    }
}

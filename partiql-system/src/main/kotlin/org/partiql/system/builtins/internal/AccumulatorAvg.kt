package org.partiql.system.builtins.internal

import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class AccumulatorAvg : Accumulator() {

    private var targetType = PType.decimal(38, 19)
    private var sum: Number = 0.0
    private var count: Long = 0L

    override fun nextValue(value: Datum) {
        checkIsNumberType(funcName = "AVG", value = value)
        this.sum += value.numberValue()
        this.count += 1L
    }

    override fun value(): Datum = when (count) {
        0L -> nullToTargetType(targetType)
        else -> (sum / bigDecimalOf(count)).toTargetType(targetType)
    }
}

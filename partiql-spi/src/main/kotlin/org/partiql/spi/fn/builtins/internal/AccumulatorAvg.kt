package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.types.PType

internal class AccumulatorAvg(
    private val targetType: PType = PType.typeDynamic(),
) : Accumulator() {

    var sum: Number = 0.0
    var count: Long = 0L

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

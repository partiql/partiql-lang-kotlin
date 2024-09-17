package org.partiql.spi.function.builtins.internal

import org.partiql.eval.value.Datum

internal class AccumulatorCount : Accumulator() {

    var count: Long = 0L

    override fun nextValue(value: Datum) {
        this.count += 1L
    }

    override fun value(): Datum = Datum.bigint(count)
}

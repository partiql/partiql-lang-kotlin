package org.partiql.spi.function.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.spi.function.Aggregation

internal class AccumulatorCountStar : Aggregation.Accumulator {

    var count: Long = 0L

    override fun next(args: Array<Datum>) {
        this.count += 1L
    }

    override fun value(): Datum = Datum.bigint(count)
}

package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Agg

internal class AccumulatorCountStar : Agg.Accumulator {

    var count: Long = 0L

    override fun next(args: Array<Datum>) {
        this.count += 1L
    }

    override fun value(): Datum = Datum.bigint(count)
}

package org.partiql.eval.internal.routines.internal

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.int64Value


internal class AccumulatorCountStar : AggregationAccumulator {

    var count: Long = 0L

    override fun next(args: Array<Datum>) {
        this.count += 1L
    }

    override fun value(): Datum = int64Value(count)
}

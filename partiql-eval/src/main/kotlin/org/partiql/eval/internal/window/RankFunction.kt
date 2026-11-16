package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

/**
 * RANK(): one plus the number of rows preceding the current row's peer group, which is the (zero-based) index of the
 * first row of that peer group plus one.
 */
internal class RankFunction : WindowFunction {

    override fun reset(partition: WindowPartition) {}

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        return Datum.bigint(orderingGroupStart + 1)
    }
}

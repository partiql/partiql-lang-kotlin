package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

internal class RowNumberFunction : WindowFunction {

    private var _index: Long = 0

    override fun reset(partition: WindowPartition) {
        _index = 0
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        return Datum.bigint(++_index)
    }
}

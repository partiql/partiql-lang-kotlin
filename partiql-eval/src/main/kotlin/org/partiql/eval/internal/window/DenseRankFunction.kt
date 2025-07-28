package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

internal class DenseRankFunction : WindowFunction {

    private var _rank: Long = 0
    private var _currentOrderingGroupStart: Long = -1

    override fun reset(partition: WindowPartition) {
        _currentOrderingGroupStart = -1
        _rank = 0
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        val newOrderingGroup = orderingGroupStart != _currentOrderingGroupStart
        if (newOrderingGroup) {
            _currentOrderingGroupStart = orderingGroupStart
            _rank += 1
        }
        return Datum.bigint(_rank)
    }
}

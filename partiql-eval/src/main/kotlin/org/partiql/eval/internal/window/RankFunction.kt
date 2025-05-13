package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.spi.value.Datum

internal class RankFunction : WindowFunction {

    private var _rank: Long = 0
    private var _currentOrderingGroupStart: Long = -1
    private var _currentOrderingGroupCount: Long = 0

    override fun reset(partition: WindowPartition) {
        _currentOrderingGroupStart = -1
        _currentOrderingGroupCount = 1
        _rank = 0
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        val newOrderingGroup = orderingGroupStart != _currentOrderingGroupStart
        if (newOrderingGroup) {
            _currentOrderingGroupStart = orderingGroupStart
            _rank += _currentOrderingGroupCount
            _currentOrderingGroupCount = 1
        } else {
            _currentOrderingGroupCount++
        }
        return Datum.bigint(_rank)
    }
}

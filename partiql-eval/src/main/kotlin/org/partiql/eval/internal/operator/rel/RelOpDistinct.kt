package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Row
import org.partiql.eval.internal.operator.Operator
import java.util.TreeSet

internal class RelOpDistinct(input: Operator.Relation) : RelOpPeeking() {

    private val input = input
    private val seen = TreeSet(DatumArrayComparator)

    override fun openPeeking() {
        input.open()
    }

    override fun peek(): Row? {
        for (row in input) {
            val transformed = Array(row.values.size) { row.values[it] }
            if (seen.contains(transformed).not()) {
                seen.add(transformed)
                return row
            }
        }
        return null
    }

    override fun closePeeking() {
        seen.clear()
        input.close()
    }
}

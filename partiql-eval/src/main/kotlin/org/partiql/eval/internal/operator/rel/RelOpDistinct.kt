package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.operator.Record
import org.partiql.eval.operator.Relation
import java.util.TreeSet

internal class RelOpDistinct(
    val input: Relation
) : RelOpPeeking() {

    private val seen = TreeSet(DatumArrayComparator)

    override fun openPeeking(env: Environment) {
        input.open(env)
    }

    override fun peek(): Record? {
        for (next in input) {
            val transformed = Array(next.values.size) { next.values[it] }
            if (seen.contains(transformed).not()) {
                seen.add(transformed)
                return next
            }
        }
        return null
    }

    override fun closePeeking() {
        seen.clear()
        input.close()
    }
}

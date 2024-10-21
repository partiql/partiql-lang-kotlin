package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import java.util.TreeSet

internal class RelOpDistinct(private val input: ExprRelation) : RelOpPeeking() {

    private val seen = TreeSet(DatumArrayComparator)

    override fun openPeeking(env: Environment) {
        input.open(env)
    }

    override fun peek(): Row? {
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

package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelDistinct(
    val input: Operator.Relation
) : RelPeeking() {

    private val seen = mutableSetOf<Record>()

    override fun openPeeking(env: Environment) {
        input.open(env)
    }

    override fun peek(): Record? {
        for (next in input) {
            if (seen.contains(next).not()) {
                seen.add(next)
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

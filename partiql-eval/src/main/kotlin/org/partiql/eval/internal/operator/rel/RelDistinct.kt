package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelDistinct(
    val input: Operator.Relation
) : RelMaterialized() {

    private val seen = mutableSetOf<Record>()

    override fun open(env: Environment) {
        input.open(env)
    }

    override fun materializeNext(): Record? {
        while (input.hasNext()) {
            val next = input.next()
            if (seen.contains(next).not()) {
                seen.add(next)
                return next
            }
        }
        return null
    }

    override fun close() {
        seen.clear()
        input.close()
    }
}

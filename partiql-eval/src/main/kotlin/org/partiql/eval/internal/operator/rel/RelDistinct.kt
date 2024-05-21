package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class RelDistinct(
    val input: Operator.Relation
) : RelPeeking() {

    // TODO: Add hashcode/equals support for PQLValue. Then we can use Record directly.
    @OptIn(PartiQLValueExperimental::class)
    private val seen = mutableSetOf<List<PartiQLValue>>()

    override fun openPeeking(env: Environment) {
        input.open(env)
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun peek(): Record? {
        for (next in input) {
            val transformed = List(next.values.size) { next.values[it].toPartiQLValue() }
            if (seen.contains(transformed).not()) {
                seen.add(transformed)
                return next
            }
        }
        return null
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun closePeeking() {
        seen.clear()
        input.close()
    }
}

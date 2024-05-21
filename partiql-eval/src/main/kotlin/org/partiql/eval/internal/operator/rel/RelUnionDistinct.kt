package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorChain
import org.partiql.eval.internal.helpers.RecordUtility.toPartiQLValueList
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class RelUnionDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    // TODO: Add support for equals/hashcode in PQLValue
    @OptIn(PartiQLValueExperimental::class)
    private val seen: MutableSet<List<PartiQLValue>> = mutableSetOf()

    private lateinit var input: Iterator<Record>

    @OptIn(PartiQLValueExperimental::class)
    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        seen.clear()
        input = IteratorChain(arrayOf(lhs, rhs))
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun peek(): Record? {
        for (record in input) {
            val partiqlRow = record.toPartiQLValueList()
            if (!seen.contains(partiqlRow)) {
                seen.add(partiqlRow)
                return record
            }
        }
        return null
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun closePeeking() {
        lhs.close()
        rhs.close()
        seen.clear()
    }
}

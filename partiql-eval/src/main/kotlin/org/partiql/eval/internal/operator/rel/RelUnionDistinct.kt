package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorChain
import org.partiql.eval.internal.helpers.RecordUtility.toDatumArrayCoerceMissing
import org.partiql.eval.internal.operator.Operator
import java.util.TreeSet

internal class RelUnionDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelPeeking() {

    private val seen = TreeSet(DatumArrayComparator)

    private lateinit var input: Iterator<Record>

    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        seen.clear()
        input = IteratorChain(arrayOf(lhs, rhs))
    }

    override fun peek(): Record? {
        for (record in input) {
            val partiqlRow = record.toDatumArrayCoerceMissing()
            if (!seen.contains(partiqlRow)) {
                seen.add(partiqlRow)
                return Record.of(*partiqlRow)
            }
        }
        return null
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
        seen.clear()
    }
}

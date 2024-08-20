package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.helpers.IteratorChain
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import org.partiql.eval.internal.operator.Operator
import java.util.TreeSet

internal class RelOpUnionDistinct(
    private val lhs: Operator.Relation,
    private val rhs: Operator.Relation,
) : RelOpPeeking() {

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
            record.values.coerceMissing()
            if (!seen.contains(record.values)) {
                seen.add(record.values)
                return Record(record.values)
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

package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumArrayComparator
import org.partiql.eval.internal.helpers.IteratorChain
import org.partiql.eval.internal.helpers.RecordUtility.coerceMissing
import java.util.TreeSet

internal class RelOpUnionDistinct(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
) : RelOpPeeking() {

    private val seen = TreeSet(DatumArrayComparator)

    private lateinit var input: Iterator<Row>

    override fun openPeeking(env: Environment) {
        lhs.open(env)
        rhs.open(env)
        seen.clear()
        input = IteratorChain(arrayOf(lhs, rhs))
    }

    override fun peek(): Row? {
        for (record in input) {
            record.coerceMissing()
            if (!seen.contains(record)) {
                seen.add(record)
                return record
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

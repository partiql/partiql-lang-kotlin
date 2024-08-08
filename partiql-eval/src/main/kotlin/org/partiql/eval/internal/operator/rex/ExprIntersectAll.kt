package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprIntersectAll(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
    private val coerce: Boolean,
) : Operator.Expr {
    // TODO: Add support for equals/hashcode in Datum
    private val counts: MutableMap<PartiQLValue, Int> = mutableMapOf()

    override fun eval(env: Environment): Datum {
        val lIter = lhs.eval(env).asIterator(coerce)
        val rIter = rhs.eval(env).asIterator(coerce)
        // read in LHS first
        for (d in lIter) {
            val value = d.toPartiQLValue()
            val count = counts[value] ?: 0
            counts[value] = count + 1
        }
        val intersected = sequence {
            for (d in rIter) {
                val value = d.toPartiQLValue()
                val count = counts[value] ?: 0
                if (count > 0) {
                    counts[value] = count - 1
                    yield(d)
                }
            }
        }
        return Datum.bagValue(intersected.asIterable())
    }
}

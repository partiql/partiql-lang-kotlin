package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprUnionDistinct(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
) : Operator.Expr {
    // TODO: Add support for equals/hashcode in Datum
    private val seen: MutableSet<PartiQLValue> = mutableSetOf()

    override fun eval(env: Environment): Datum {
        val lIter = lhs.eval(env).asIterator()
        val rIter = rhs.eval(env).asIterator()
        val unioned = lIter.asSequence() + rIter.asSequence()
        val distinct = sequence {
            for (d in unioned) {
                val value = d.toPartiQLValue()
                if (!seen.contains(value)) {
                    seen.add(value)
                    yield(d)
                }
            }
        }
        return Datum.bagValue(distinct.toList())
    }
}

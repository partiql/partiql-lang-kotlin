package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.IteratorPeeking
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprExceptDistinct(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
    private val coerce: Boolean,
) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val lDatum = lhs.eval(env)
        val rDatum = rhs.eval(env)
        val iter = Iterable {
            // TODO: Add support for equals/hashcode in Datum
            val seen: MutableSet<PartiQLValue> = mutableSetOf()
            val lIter = lDatum.asIterator(coerce)
            val rIter = rDatum.asIterator(coerce)
            object : IteratorPeeking<Datum>() {
                override fun peek(): Datum? {
                    // read in RHS first
                    for (r in rIter) {
                        val value = r.toPartiQLValue()
                        seen.add(value)
                    }
                    for (l in lIter) {
                        val value = l.toPartiQLValue()
                        if (!seen.contains(value)) {
                            return l
                        }
                    }
                    return null
                }
            }
        }
        return Datum.bag(iter)
    }
}

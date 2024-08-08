package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
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
    // TODO: Add support for equals/hashcode in Datum
    private val seen: MutableSet<PartiQLValue> = mutableSetOf()

    override fun eval(env: Environment): Datum {
        val lIter = lhs.eval(env).asIterator(coerce)
        val rIter = rhs.eval(env).asIterator(coerce)
        // read in RHS first
        for (d in rIter) {
            val value = d.toPartiQLValue()
            seen.add(value)
        }
        val excepted = sequence {
            for (d in lIter) {
                val value = d.toPartiQLValue()
                if (!seen.contains(value)) {
                    yield(d)
                }
            }
        }
        return Datum.bagValue(excepted.asIterable())
    }
}

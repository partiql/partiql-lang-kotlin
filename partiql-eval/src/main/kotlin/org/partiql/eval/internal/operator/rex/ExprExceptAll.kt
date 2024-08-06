package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprExceptAll(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
) : Operator.Expr {

    // TODO: Add support for equals/hashcode in Datum
    private val counts: MutableMap<PartiQLValue, Int> = mutableMapOf()

    override fun eval(env: Environment): Datum {
        val lIter = lhs.eval(env).asIterator()
        val rIter = rhs.eval(env).asIterator()
        // read in RHS first
        for (d in rIter) {
            val value = d.toPartiQLValue()
            val count = counts[value] ?: 0
            counts[value] = count + 1
        }
        val excepted = sequence {
            for (d in lIter) {
                val value = d.toPartiQLValue()
                val count = counts[value] ?: 0
                if (count > 0) {
                    counts[value] = count - 1
                    continue
                }
                yield(d)
            }
        }
        return Datum.bagValue(excepted.toList())
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.IteratorChain
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

internal class ExprUnionAll(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
    private val coerce: Boolean,
) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val lDatum = lhs.eval(env)
        val rDatum = rhs.eval(env)
        val iter = Iterable {
            val lIter = lDatum.asIterator(coerce)
            val rIter = rDatum.asIterator(coerce)
            IteratorChain(arrayOf(lIter, rIter))
        }
        return Datum.bag(iter)
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

internal class ExprUnionAll(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
    private val coerce: Boolean,
) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val lIter = lhs.eval(env).asIterator(coerce)
        val rIter = rhs.eval(env).asIterator(coerce)
        val unioned = lIter.asSequence() + rIter.asSequence()
        return Datum.bagValue(unioned.asIterable())
    }
}

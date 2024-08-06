package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.asIterator
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

internal class ExprUnionAll(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val lIter = lhs.eval(env).asIterator()
        val rIter = rhs.eval(env).asIterator()
        val unioned = lIter.asSequence() + rIter.asSequence()
        return Datum.bagValue(unioned.toList())
    }
}

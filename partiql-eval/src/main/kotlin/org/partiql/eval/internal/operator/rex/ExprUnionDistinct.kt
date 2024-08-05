package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueExperimental

internal class ExprUnionDistinct(
    private val lhs: Operator.Expr,
    private val rhs: Operator.Expr,
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        TODO()
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue

internal class ExprNullIf(
    private val valueExpr: Operator.Expr,
    private val nullifierExpr: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    private val comparator = PartiQLValue.comparator()

    @PartiQLValueExperimental
    override fun eval(env: Environment): PartiQLValue {
        val value = valueExpr.eval(env)
        val nullifier = nullifierExpr.eval(env)
        return when (comparator.compare(value, nullifier)) {
            0 -> nullValue()
            else -> value
        }
    }
}

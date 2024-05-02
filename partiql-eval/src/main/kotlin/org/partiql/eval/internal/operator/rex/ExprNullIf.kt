package org.partiql.eval.internal.operator.rex

import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class ExprNullIf(
    private val valueExpr: Operator.Expr,
    private val nullifierExpr: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    private val comparator = PartiQLValue.comparator()

    @PartiQLValueExperimental
    override fun eval(env: Environment): PQLValue {
        val value = valueExpr.eval(env)
        val nullifier = nullifierExpr.eval(env)
        return when (comparator.compare(value.toPartiQLValue(), nullifier.toPartiQLValue())) {
            0 -> PQLValue.nullValue()
            else -> value
        }
    }
}

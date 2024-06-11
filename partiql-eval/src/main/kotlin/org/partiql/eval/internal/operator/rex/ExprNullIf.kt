package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class ExprNullIf(
    private val valueExpr: Operator.Expr,
    private val nullifierExpr: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    private val comparator = PartiQLValue.comparator()

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        val value = valueExpr.eval(env)
        val nullifier = nullifierExpr.eval(env)
        return when (comparator.compare(value.toPartiQLValue(), nullifier.toPartiQLValue())) {
            0 -> Datum.nullValue()
            else -> value
        }
    }
}

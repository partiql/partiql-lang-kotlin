package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

internal class ExprNullIf(
    private val valueExpr: Operator.Expr,
    private val nullifierExpr: Operator.Expr
) : Operator.Expr {

    private val comparator = Datum.comparator()

    override fun eval(env: Environment): Datum {
        val value = valueExpr.eval(env)
        val nullifier = nullifierExpr.eval(env)
        return when (comparator.compare(value, nullifier)) {
            0 -> Datum.nullValue()
            else -> value
        }
    }
}

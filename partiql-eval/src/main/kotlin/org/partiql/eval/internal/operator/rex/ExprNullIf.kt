package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum

internal class ExprNullIf(
    private val valueExpr: Expression,
    private val nullifierExpr: Expression
) : Expression {

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

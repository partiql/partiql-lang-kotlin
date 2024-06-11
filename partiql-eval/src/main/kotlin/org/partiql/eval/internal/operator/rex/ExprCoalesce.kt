package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueExperimental

internal class ExprCoalesce(
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        for (arg in args) {
            val result = arg.eval(env)
            if (!result.isNull && !result.isMissing) {
                return result
            }
        }
        return Datum.nullValue()
    }
}

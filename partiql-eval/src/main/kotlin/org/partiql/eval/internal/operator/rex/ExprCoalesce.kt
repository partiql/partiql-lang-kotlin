package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.PQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprCoalesce(
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): PQLValue {
        for (arg in args) {
            val result = arg.eval(env)
            if (!result.isNull && result.type != PartiQLValueType.MISSING) {
                return result
            }
        }
        return PQLValue.nullValue()
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental

/**
 * Returns the value in the given record index.
 */
internal class ExprVarLocal(
    private val ref: Int,
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(env: Environment): PQLValue {
        return env[ref]
    }
}

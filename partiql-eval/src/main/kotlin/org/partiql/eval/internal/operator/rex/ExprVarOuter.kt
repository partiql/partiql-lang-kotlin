package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Returns the appropriate value from the stack.
 */
internal class ExprVarOuter(
    private val scope: Int,
    private val reference: Int,
    private val env: Environment
) : Operator.Expr {

    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        return env[scope][reference]
    }
}

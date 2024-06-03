package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.PQLValue
import org.partiql.value.PartiQLValueExperimental

internal class ExprLiteral @OptIn(PartiQLValueExperimental::class) constructor(private val value: PQLValue) : Operator.Expr {
    @PartiQLValueExperimental
    override fun eval(env: Environment): PQLValue {
        return value
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueExperimental

internal class ExprLiteral @OptIn(PartiQLValueExperimental::class) constructor(private val value: Datum) : Operator.Expr {
    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        return value
    }
}

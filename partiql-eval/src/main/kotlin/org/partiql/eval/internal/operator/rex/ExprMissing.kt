package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.PQLValue

internal class ExprMissing(
    private val message: String,
) : Operator.Expr {

    override fun eval(env: Environment): PQLValue {
        throw TypeCheckException(message)
    }
}

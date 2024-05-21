package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator

internal class ExprMissing(
    private val message: String,
) : Operator.Expr {

    override fun eval(env: Environment): PQLValue {
        throw TypeCheckException(message)
    }
}

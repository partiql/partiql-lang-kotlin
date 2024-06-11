package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

internal class ExprMissing(
    private val message: String,
) : Operator.Expr {

    override fun eval(env: Environment): Datum {
        throw TypeCheckException(message)
    }
}

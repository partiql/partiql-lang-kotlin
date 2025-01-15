package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.value.Datum

internal class ExprError : ExprValue {
    override fun eval(env: Environment): Datum {
        throw TypeCheckException("A warning has been converted into an error.")
    }
}

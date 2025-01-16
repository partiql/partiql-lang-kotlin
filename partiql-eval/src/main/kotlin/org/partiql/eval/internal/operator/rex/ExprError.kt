package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.value.Datum

internal class ExprError : ExprValue {
    override fun eval(env: Environment): Datum {
        // TODO: Add error to ExprError
        throw PErrors.internalErrorException(IllegalStateException("A warning has been converted into an error."))
    }
}

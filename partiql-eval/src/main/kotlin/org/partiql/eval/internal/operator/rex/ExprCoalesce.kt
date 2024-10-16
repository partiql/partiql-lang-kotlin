package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

internal class ExprCoalesce(
    private val args: Array<Operator.Expr>,
) : Operator.Expr {

    override fun eval(): Datum {
        for (arg in args) {
            val result = arg.eval()
            if (!result.isNull && !result.isMissing) {
                return result
            }
        }
        return Datum.nullValue()
    }
}

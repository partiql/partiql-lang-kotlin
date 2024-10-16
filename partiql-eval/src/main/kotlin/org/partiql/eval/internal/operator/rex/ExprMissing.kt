package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprMissing(
    private val type: PType,
) : Operator.Expr {

    override fun eval(): Datum {
        return Datum.missing(type)
    }
}

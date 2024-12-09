package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprMissing(
    private val type: PType
) : ExprValue {

    override fun eval(env: Environment): Datum {
        return Datum.missing(type)
    }
}

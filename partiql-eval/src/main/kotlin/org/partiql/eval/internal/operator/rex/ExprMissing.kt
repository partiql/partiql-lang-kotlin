package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.PType

internal class ExprMissing(
    private val type: PType
) : Operator.Expr {

    override fun eval(env: Environment): Datum {
        return Datum.missingValue(type)
    }
}

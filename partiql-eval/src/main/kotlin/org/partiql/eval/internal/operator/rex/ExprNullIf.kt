package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum

internal class ExprNullIf(
    private val valueExpr: ExprValue,
    private val nullifierExpr: ExprValue
) : ExprValue {

    private val comparator = Datum.comparator()

    override fun eval(env: Environment): Datum {
        val value = valueExpr.eval(env)
        val nullifier = nullifierExpr.eval(env)
        if (value.isMissing || nullifier.isMissing) {
            return Datum.missing()
        }
        if (value.isNull || nullifier.isNull) {
            return value
        }
        return when (comparator.compare(value, nullifier)) {
            0 -> Datum.nullValue()
            else -> value
        }
    }
}

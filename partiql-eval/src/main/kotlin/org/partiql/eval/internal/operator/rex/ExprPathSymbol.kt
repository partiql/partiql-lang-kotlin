package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprPathSymbol(
    @JvmField val root: Operator.Expr,
    @JvmField val symbol: String,
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): Datum {
        val struct = root.eval(env).check(PartiQLValueType.STRUCT)
        if (struct.isNull) {
            return Datum.nullValue()
        }
        return struct.getInsensitive(symbol) ?: throw TypeCheckException("Couldn't find symbol '$symbol' in $struct.")
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprPathSymbol(
    @JvmField val root: Expression,
    @JvmField val symbol: String,
) : Expression {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): Datum {
        val struct = root.eval(env).check(PartiQLValueType.STRUCT)
        if (struct.isNull) {
            return Datum.nullValue()
        }
        return struct.getInsensitive(symbol) ?: throw TypeCheckException("Couldn't find symbol '$symbol' in $struct.")
    }
}

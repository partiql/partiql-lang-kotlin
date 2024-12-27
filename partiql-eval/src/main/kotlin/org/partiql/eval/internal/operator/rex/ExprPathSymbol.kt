package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprPathSymbol(
    @JvmField val root: ExprValue,
    @JvmField val symbol: String,
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val struct = root.eval(env).check(PType.struct())
        if (struct.isNull) {
            return Datum.nullValue()
        }
        return struct.getInsensitive(symbol) ?: throw TypeCheckException("Couldn't find symbol '$symbol' in $struct.")
    }
}

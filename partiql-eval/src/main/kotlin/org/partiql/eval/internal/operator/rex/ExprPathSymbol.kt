package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.checkStruct
import org.partiql.spi.value.Datum

internal class ExprPathSymbol(
    @JvmField val root: ExprValue,
    @JvmField val symbol: String,
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val struct = root.eval(env).checkStruct()
        if (struct.isNull) {
            return Datum.nullValue()
        }
        return struct.getInsensitive(symbol) ?: throw PErrors.pathSymbolFailureException()
    }
}

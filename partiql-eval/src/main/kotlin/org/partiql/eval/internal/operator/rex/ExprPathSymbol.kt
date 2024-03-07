package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.nullValue

internal class ExprPathSymbol(
    @JvmField val root: Operator.Expr,
    @JvmField val symbol: String,
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PartiQLValue {
        val struct = root.eval(env).check<StructValue<PartiQLValue>>()
        if (struct.isNull) {
            return nullValue()
        }
        for ((k, v) in struct.entries) {
            if (k.equals(symbol, ignoreCase = true)) {
                return v
            }
        }
        throw TypeCheckException("Couldn't find symbol '$symbol' in $struct.")
    }
}

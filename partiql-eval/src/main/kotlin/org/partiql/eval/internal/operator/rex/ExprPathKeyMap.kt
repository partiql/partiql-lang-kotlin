package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.value.Datum

internal class ExprPathKeyMap(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        return evalWithInput(root.eval(env), env)
    }

    fun evalWithInput(input: Datum, env: Environment): Datum {
        val k = key.eval(env)
        if (input.isNull || k.isNull) {
            return Datum.nullValue(input.type.valueType)
        }
        if (input.isMissing || k.isMissing) {
            return Datum.missing(input.type.valueType)
        }
        return input.get(k).orElseThrow { PErrors.pathKeyFailureException() }
    }
}

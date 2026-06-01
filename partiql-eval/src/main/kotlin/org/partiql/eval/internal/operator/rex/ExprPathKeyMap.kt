package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathKeyMap(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        return evalWithInput(root.eval(env), env)
    }

    fun evalWithInput(input: Datum, env: Environment): Datum {
        if (input.isNull || input.isMissing) {
            return Datum.nullValue()
        }
        val k = key.eval(env).check(PType.string())
        if (k.isNull || k.isMissing) {
            throw PErrors.pathKeyFailureException()
        }
        return input.get(k).orElseThrow { PErrors.pathKeyFailureException() }
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.helpers.ValueUtility.checkStruct
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathKeyStruct(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        return evalWithInput(root.eval(env), env)
    }

    fun evalWithInput(input: Datum, env: Environment): Datum {
        val checkedRoot = input.checkStruct()
        val k = key.eval(env).check(PType.string())
        if (checkedRoot.isNull || k.isNull) {
            return Datum.nullValue()
        }

        val keyString = k.string
        return checkedRoot.get(keyString) ?: throw PErrors.pathKeyFailureException()
    }
}

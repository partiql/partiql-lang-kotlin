package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.helpers.ValueUtility.checkStruct
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathKey(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val input = root.eval(env)
        val key = key.eval(env).check(PType.string())
        if (input.isNull || key.isNull) {
            return Datum.nullValue()
        }
        return when (input.type.code()) {
            PType.MAP -> evalMap(input, key)
            else -> evalStruct(input, key)
        }
    }

    private fun evalMap(input: Datum, k: Datum): Datum {
        if (k.isNull || k.isMissing) {
            return Datum.nullValue()
        }
        return input.get(k).orElseThrow { PErrors.pathKeyFailureException() }
    }

    private fun evalStruct(input: Datum, k: Datum): Datum {
        if (k.isNull || k.isMissing) {
            return Datum.nullValue()
        }
        val checkedRoot = input.checkStruct()
        return checkedRoot.get(k.string) ?: throw PErrors.pathKeyFailureException()
    }
}

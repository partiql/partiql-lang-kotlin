package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.getInt32Coerced
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathIndex(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue,
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val input = root.eval(env)
        val k = key.eval(env)
        return when (input.type.code()) {
            PType.MAP -> evalMap(input, k)
            else -> evalCollection(input, k)
        }
    }

    private fun evalMap(input: Datum, k: Datum): Datum {
        if (k.isNull || k.isMissing) {
            throw PErrors.pathIndexFailureException()
        }
        return input.get(k).orElseThrow { PErrors.pathIndexFailureException() }
    }

    private fun evalCollection(input: Datum, k: Datum): Datum {
        val iterator = when (input.type.code()) {
            PType.BAG,
            PType.ARRAY -> input.iterator()
            else -> throw PErrors.pathIndexFailureException()
        }
        val index = k.getInt32Coerced()
        var i = 0
        while (iterator.hasNext()) {
            val v = iterator.next()
            if (i == index) {
                return v
            }
            i++
        }
        throw PErrors.pathIndexFailureException()
    }
}

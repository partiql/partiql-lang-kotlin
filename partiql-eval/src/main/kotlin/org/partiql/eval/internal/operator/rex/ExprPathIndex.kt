package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getInt32Coerced
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal class ExprPathIndex(
    @JvmField val root: Operator.Expr,
    @JvmField val key: Operator.Expr,
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PQLValue {
        val input = root.eval(env)
        val iterator = when (input.type) {
            PartiQLValueType.BAG, PartiQLValueType.LIST, PartiQLValueType.SEXP -> input.iterator()
            else -> throw TypeCheckException()
        }

        // Calculate index
        val k = key.eval(env)
        val index = k.getInt32Coerced()

        // Get element
        var i = 0
        while (iterator.hasNext()) {
            val v = iterator.next()
            if (i == index) {
                return v
            }
            i++
        }
        throw TypeCheckException()
    }
}

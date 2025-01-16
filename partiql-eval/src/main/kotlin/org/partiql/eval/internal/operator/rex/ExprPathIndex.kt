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
        val iterator = when (input.type.code()) {
            PType.BAG,
            PType.ARRAY -> input.iterator()
            else -> throw PErrors.pathIndexFailureException()
        }

        // Calculate index
        // TODO: The PLANNER should be in charge of adding a necessary coercion for the index. AKA, getInt32Coerced()
        //  should never need to be called.
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
        throw PErrors.pathIndexFailureException()
    }
}

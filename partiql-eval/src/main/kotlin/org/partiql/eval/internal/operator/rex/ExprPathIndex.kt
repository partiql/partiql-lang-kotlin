package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.helpers.ValueUtility.getInt32Coerced
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprPathIndex(
    @JvmField val root: Operator.Expr,
    @JvmField val key: Operator.Expr,
) : Operator.Expr {

    override fun eval(): Datum {
        val input = root.eval()
        val iterator = when (input.type.kind) {
            PType.Kind.BAG,
            PType.Kind.ARRAY,
            PType.Kind.SEXP,
            -> input.iterator()
            else -> throw TypeCheckException("expected collection, found ${input.type.kind}")
        }

        // Calculate index
        // TODO: The PLANNER should be in charge of adding a necessary coercion for the index. AKA, getInt32Coerced()
        //  should never need to be called.
        val k = key.eval()
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

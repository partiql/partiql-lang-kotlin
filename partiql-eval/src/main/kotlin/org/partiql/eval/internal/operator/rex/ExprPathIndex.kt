package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.CollectionValue
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.check

internal class ExprPathIndex(
    @JvmField val root: Operator.Expr,
    @JvmField val key: Operator.Expr,
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(record: Record): PartiQLValue {
        val collection = root.eval(record).check<CollectionValue<PartiQLValue>>()

        // Calculate index
        val index = when (val k = key.eval(record)) {
            is Int16Value -> k.int
            is Int32Value -> k.int
            is Int64Value -> k.int
            is Int8Value -> k.int
            is IntValue -> k.int
            else -> throw TypeCheckException()
        } ?: throw TypeCheckException()

        // Get element
        val iterator = collection.iterator()
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

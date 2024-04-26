package org.partiql.eval.internal.operator.rex

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.NumericValue
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
            PartiQLValueType.BAG -> input.bagValues
            PartiQLValueType.LIST -> input.listValues
            PartiQLValueType.SEXP -> input.sexpValues
            else -> throw TypeCheckException()
        }

        // Calculate index
        val index = when (val k = key.eval(env)) {
            is Int16Value,
            is Int32Value,
            is Int64Value,
            is Int8Value,
            is IntValue -> try {
                (k as NumericValue<*>).toInt32().value
            } catch (e: DataException) {
                throw TypeCheckException()
            }
            else -> throw TypeCheckException()
        } ?: throw TypeCheckException()

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

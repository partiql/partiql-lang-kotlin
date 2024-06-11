package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

internal class ExprStruct(private val fields: List<Field>) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue()
            }
            val keyString = key.getText()
            val value = it.value.eval(env)
            when (value.isMissing) {
                true -> null
                false -> org.partiql.eval.value.Field.of(keyString, value)
            }
        }
        return Datum.structValue(fields)
    }

    internal class Field(val key: Operator.Expr, val value: Operator.Expr)
}

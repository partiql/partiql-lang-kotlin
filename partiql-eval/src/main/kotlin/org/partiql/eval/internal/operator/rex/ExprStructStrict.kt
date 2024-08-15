package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

internal class ExprStructStrict(private val fields: List<ExprStructField>) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env)
            if (key.isNull || key.isMissing) {
                throw TypeCheckException("Struct key was absent.")
            }
            val keyString = key.getText()
            val value = it.value.eval(env)
            when (value.isMissing) {
                true -> null
                false -> org.partiql.eval.value.Field.of(keyString, value)
            }
        }
        return Datum.struct(fields)
    }
}

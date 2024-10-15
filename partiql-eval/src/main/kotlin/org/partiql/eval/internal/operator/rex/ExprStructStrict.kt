package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal class ExprStructStrict(private val fields: List<ExprStructField>) :
    Expression {
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
                false -> Field.of(keyString, value)
            }
        }
        return Datum.struct(fields)
    }
}

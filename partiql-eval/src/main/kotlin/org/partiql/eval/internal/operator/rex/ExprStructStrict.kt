package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal class ExprStructStrict(private val fields: List<ExprStructField>) :
    ExprValue {
    override fun eval(env: Environment): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue(PType.struct())
            }
            if (key.isMissing) {
                return Datum.missing(PType.struct())
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

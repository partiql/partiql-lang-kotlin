package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.spi.types.PType

internal class ExprStructPermissive(private val fields: List<ExprStructField>) :
    ExprValue {
    override fun eval(env: Environment): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env).lowerSafe()
            val keyString = key.getTextOrNull() ?: return@mapNotNull null
            val value = it.value.eval(env)
            when (value.isMissing) {
                true -> null
                false -> Field.of(keyString, value)
            }
        }
        return Datum.struct(fields)
    }

    companion object {
        /**
         * @return the underlying string value of a textual value; null if the type is not a textual value or if the
         * value itself is absent.
         */
        @JvmStatic
        fun Datum.getTextOrNull(): String? {
            if (this.isNull || this.isMissing) {
                return null
            }
            return when (this.type.code()) {
                PType.STRING, PType.CHAR -> this.string
                else -> null
            }
        }
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType

internal class ExprStructPermissive(private val fields: List<ExprStructField>) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env)
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
            return when (this.type.kind) {
                PType.Kind.STRING, PType.Kind.SYMBOL, PType.Kind.CHAR -> this.string
                else -> null
            }
        }
    }
}

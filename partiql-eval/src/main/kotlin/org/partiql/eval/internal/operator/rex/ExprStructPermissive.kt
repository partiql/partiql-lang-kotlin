package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.PType

internal class ExprStructPermissive(private val fields: List<ExprStructField>) : Operator.Expr {
    override fun eval(env: Environment): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue()
            }
            val keyString = key.getTextOrNull() ?: return Datum.struct(emptyList())
            val value = it.value.eval(env)
            when (value.isMissing) {
                true -> null
                false -> org.partiql.eval.value.Field.of(keyString, value)
            }
        }
        return Datum.struct(fields)
    }

    companion object {
        /**
         * @throws NullPointerException if the value itself is null
         * @return the underlying string value of a textual value; null if the type is not a textual value.
         */
        fun Datum.getTextOrNull(): String? {
            return when (this.type.kind) {
                PType.Kind.STRING, PType.Kind.SYMBOL, PType.Kind.CHAR -> this.string
                else -> null
            }
        }
    }
}

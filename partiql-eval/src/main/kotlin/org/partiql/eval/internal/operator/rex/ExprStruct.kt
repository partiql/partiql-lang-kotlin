package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.helpers.ValueUtility.getTextOrNull
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal class ExprStructField(
    val key: Operator.Expr,
    val value: Operator.Expr,
)

internal class ExprStruct(private val fields: List<ExprStructField>) : Operator.Expr {

    override fun eval(): Datum {
        val fields = fields.mapNotNull {
            val key = it.key.eval()
            if (key.isNull || key.isMissing) {
                throw TypeCheckException("Struct key was absent.")
            }
            val k = key.getText()
            val v = it.value.eval()
            when (v.isMissing) {
                true -> null
                false -> Field.of(k, v)
            }
        }
        return Datum.struct(fields)
    }
}

internal class ExprStructPermissive(private val fields: List<ExprStructField>) : Operator.Expr {

    override fun eval(): Datum {
        val fields = fields.mapNotNull {
            val k = it.key.eval().getTextOrNull() ?: return@mapNotNull null
            val v = it.value.eval()
            when (v.isMissing) {
                true -> null
                false -> Field.of(k, v)
            }
        }
        return Datum.struct(fields)
    }
}

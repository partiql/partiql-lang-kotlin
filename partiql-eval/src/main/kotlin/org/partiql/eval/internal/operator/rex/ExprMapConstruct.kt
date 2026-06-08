package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry

internal class ExprMapConstruct(
    private val keyType: PType,
    private val valueType: PType,
    private val fields: List<ExprStructField>,
) : ExprValue {
    override fun eval(env: Environment): Datum {
        val entries = fields.mapNotNull { field ->
            val key = field.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue(PType.map(keyType, valueType))
            }
            if (key.isMissing) {
                return Datum.missing(PType.map(keyType, valueType))
            }
            val value = field.value.eval(env)
            when (value.isMissing) {
                true -> null
                false -> Entry.of(key, value)
            }
        }
        return Datum.map(keyType, valueType, entries)
    }
}

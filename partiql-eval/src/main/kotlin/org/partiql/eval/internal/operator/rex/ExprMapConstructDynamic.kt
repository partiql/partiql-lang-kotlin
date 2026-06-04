package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.DynamicTyper
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry

internal class ExprMapConstructDynamic(
    private val fields: List<ExprStructField>,
) : ExprValue {
    override fun eval(env: Environment): Datum {
        val keys = mutableListOf<Datum>()
        val values = mutableListOf<Datum>()
        for (field in fields) {
            val key = field.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue()
            }

            if (key.isMissing) {
                return Datum.missing()
            }
            keys.add(key)
            values.add(field.value.eval(env))
        }
        val keyType = if (keys.isEmpty()) PType.string() else DynamicTyper.commonSuperType(keys.map { it.type })
        if (keyType.code() == PType.DYNAMIC) {
            error("MAP key type must not be DYNAMIC")
        }
        val valueType = DynamicTyper.commonSuperType(values.map { it.type })
        val entries = keys.zip(values).map { (k, v) ->
            val castedKey = if (k.type.code() != keyType.code()) {
                CastTable.cast(k, keyType)
            } else {
                k
            }
            val castedValue = if (v.type.code() != valueType.code() && valueType.code() != PType.DYNAMIC) {
                CastTable.cast(v, valueType)
            } else {
                v
            }
            Entry.of(castedKey, castedValue)
        }
        return Datum.map(keyType, valueType, entries)
    }
}

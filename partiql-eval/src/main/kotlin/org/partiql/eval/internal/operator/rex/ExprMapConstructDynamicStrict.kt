package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.DynamicTyper
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry
import java.util.TreeSet

internal class ExprMapConstructDynamicStrict(
    private val fields: List<ExprStructField>,
) : ExprValue {

    companion object {
        private val COMPARATOR = Datum.comparator()
    }

    override fun eval(env: Environment): Datum {
        val keys = mutableListOf<Datum>()
        val values = mutableListOf<Datum>()
        for (field in fields) {
            val key = field.key.eval(env)
            if (key.isNull) return Datum.nullValue()
            if (key.isMissing) return Datum.missing()
            val value = field.value.eval(env)
            if (value.isMissing) continue
            keys.add(key)
            values.add(value)
        }
        val keyType = if (keys.isEmpty()) PType.string() else DynamicTyper.commonSuperType(keys.map { it.type })
        if (keyType.code() == PType.DYNAMIC) {
            error("MAP key type must not be DYNAMIC")
        }
        val valueType = DynamicTyper.commonSuperType(values.map { it.type })
        val seen = TreeSet<Datum>(COMPARATOR)
        val entries = keys.zip(values).mapNotNull { (k, v) ->
            val castedKey = if (k.type.code() != keyType.code()) {
                CastTable.cast(k, keyType)
            } else {
                k
            }
            if (!seen.add(castedKey)) {
                throw PErrors.mapDuplicateKeyException(castedKey)
            }
            val castedValue = if (v.type.code() != valueType.code() && valueType.code() != PType.DYNAMIC) {
                CastTable.cast(v, valueType)
            } else {
                v
            }
            if (castedValue.isMissing) return@mapNotNull null
            Entry.of(castedKey, castedValue)
        }
        return Datum.map(keyType, valueType, entries)
    }
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.DynamicTyper
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry

internal class ExprMapConstruct(
    private val keyType: PType?,
    private val valueType: PType?,
    private val fields: List<ExprStructField>,
) : ExprValue {
    override fun eval(env: Environment): Datum {
        val keys = mutableListOf<Datum>()
        val values = mutableListOf<Datum>()
        for (field in fields) {
            val key = field.key.eval(env)
            if (key.isNull || key.isMissing) {
                throw PErrors.pathKeyFailureException()
            }
            keys.add(key)
            values.add(field.value.eval(env))
        }
        val resolvedKeyType = keyType ?: DynamicTyper.commonSuperType(keys.map { it.type })
        assert(resolvedKeyType.code() != PType.DYNAMIC) { "MAP key type must not be DYNAMIC" }
        val resolvedValueType = valueType ?: DynamicTyper.commonSuperType(values.map { it.type })
        val entries = keys.zip(values).map { (k, v) ->
            val castedKey = if (k.type.code() != resolvedKeyType.code()) {
                CastTable.cast(k, resolvedKeyType)
            } else {
                k
            }
            val castedValue = if (v.type.code() != resolvedValueType.code() && resolvedValueType.code() != PType.DYNAMIC) {
                CastTable.cast(v, resolvedValueType)
            } else {
                v
            }
            Entry.of(castedKey, castedValue)
        }
        return Datum.map(resolvedKeyType, resolvedValueType, entries)
    }
}
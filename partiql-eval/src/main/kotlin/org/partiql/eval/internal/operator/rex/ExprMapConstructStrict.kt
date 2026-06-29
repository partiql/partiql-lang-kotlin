package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry
import java.util.TreeSet

internal class ExprMapConstructStrict(
    private val keyType: PType,
    private val valueType: PType,
    private val fields: List<ExprStructField>,
) : ExprValue {

    companion object {
        private val COMPARATOR = Datum.comparator()
    }

    override fun eval(env: Environment): Datum {
        val seen = TreeSet<Datum>(COMPARATOR)
        val entries = fields.mapNotNull { field ->
            val key = field.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue(PType.map(keyType, valueType))
            }
            if (key.isMissing) {
                return Datum.missing(PType.map(keyType, valueType))
            }
            if (!seen.add(key)) {
                throw PErrors.mapDuplicateKeyException(key)
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

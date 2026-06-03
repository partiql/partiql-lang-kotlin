package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry

internal class ExprMapConstruct(
    private val keyType: PType,
    private val valueType: PType,
    private val fields: List<ExprStructField>,
) : ExprValue {
    override fun eval(env: Environment): Datum {
        val entries = mutableListOf<Entry>()
        for (field in fields) {
            val key = field.key.eval(env)
            if (key.isNull) {
                return Datum.nullValue()
            }

            if (key.isMissing) {
                throw PErrors.pathKeyFailureException()
            }
            val value = field.value.eval(env)
            entries.add(Entry.of(key, value))
        }
        return Datum.map(keyType, valueType, entries)
    }
}

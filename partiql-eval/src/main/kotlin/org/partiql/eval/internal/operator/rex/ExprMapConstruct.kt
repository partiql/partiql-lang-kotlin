package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry

internal class ExprMapConstruct(private val fields: List<ExprStructField>) : ExprValue {
    override fun eval(env: Environment): Datum {
        val entries = mutableListOf<Entry>()
        for (field in fields) {
            val key = field.key.eval(env)
            if (key.isNull) {
                throw IllegalArgumentException("NULL is not allowed as a MAP key")
            }
            if (key.isMissing) {
                throw IllegalArgumentException("MISSING is not allowed as a MAP key")
            }
            val value = field.value.eval(env)
            entries.add(Entry.of(key, value))
        }
        return Datum.map(PType.map(), entries)
    }
}

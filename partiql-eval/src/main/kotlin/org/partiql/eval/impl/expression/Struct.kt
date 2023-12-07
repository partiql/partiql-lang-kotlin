package org.partiql.eval.impl.expression

import org.partiql.eval.impl.PhysicalNode
import org.partiql.eval.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.structValue

internal class Struct(val fields: List<Field>) : PhysicalNode.Expression {
    @OptIn(PartiQLValueExperimental::class)
    override fun evaluate(record: Record): PartiQLValue {
        val fields = fields.map {
            val key = it.key.evaluate(record) as? StringValue ?: error("Expected struct key to be a STRING.")
            val value = it.key.evaluate(record)
            key.value!! to value
        }
        return structValue(fields.asSequence())
    }

    internal class Field(val key: PhysicalNode.Expression, val value: PhysicalNode.Expression)
}

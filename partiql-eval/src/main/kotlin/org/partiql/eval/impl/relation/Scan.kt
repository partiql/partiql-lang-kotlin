package org.partiql.eval.impl.relation

import org.partiql.eval.impl.PhysicalNode
import org.partiql.eval.impl.Record
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValueExperimental

internal class Scan(
    private val expr: PhysicalNode.Expression
) : PhysicalNode.Relation {

    @OptIn(PartiQLValueExperimental::class)
    override fun evaluate(): Iterator<Record> {
        return when (val value = expr.evaluate(Record(emptyList()))) {
            is CollectionValue<*> -> value.elements!!.map { Record(listOf(it)) }.iterator()
            else -> iterator { yield(Record(listOf(value))) }
        }
    }
}

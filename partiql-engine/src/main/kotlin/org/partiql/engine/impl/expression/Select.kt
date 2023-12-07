package org.partiql.engine.impl.expression

import org.partiql.engine.impl.PhysicalNode
import org.partiql.engine.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue

internal class Select(
    val input: PhysicalNode.Relation,
    val constructor: PhysicalNode.Expression
) : PhysicalNode.Expression {
    @PartiQLValueExperimental
    override fun evaluate(record: Record): PartiQLValue {
        val elements = mutableListOf<PartiQLValue>()
        input.evaluate().forEach { record ->
            val element = constructor.evaluate(record)
            elements.add(element)
        }
        return bagValue(elements.asSequence())
    }
}

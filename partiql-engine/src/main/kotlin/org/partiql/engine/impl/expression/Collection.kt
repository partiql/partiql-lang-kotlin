package org.partiql.engine.impl.expression

import org.partiql.engine.impl.PhysicalNode
import org.partiql.engine.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue

internal class Collection(
    private val values: List<PhysicalNode.Expression>
) : PhysicalNode.Expression {
    @PartiQLValueExperimental
    override fun evaluate(record: Record): PartiQLValue {
        return bagValue(
            values.map { it.evaluate(record) }.asSequence()
        )
    }
}

package org.partiql.eval.impl.expression

import org.partiql.eval.impl.PhysicalNode
import org.partiql.eval.impl.Record
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

package org.partiql.engine.impl.expression

import org.partiql.engine.impl.PhysicalNode
import org.partiql.engine.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class Literal @OptIn(PartiQLValueExperimental::class) constructor(private val value: PartiQLValue) : PhysicalNode.Expression {
    @PartiQLValueExperimental
    override fun evaluate(record: Record): PartiQLValue {
        return value
    }
}

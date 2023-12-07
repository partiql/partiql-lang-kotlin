package org.partiql.eval.impl.expression

import org.partiql.eval.impl.PhysicalNode
import org.partiql.eval.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class Literal @OptIn(PartiQLValueExperimental::class) constructor(private val value: PartiQLValue) : PhysicalNode.Expression {
    @PartiQLValueExperimental
    override fun evaluate(record: Record): PartiQLValue {
        return value
    }
}

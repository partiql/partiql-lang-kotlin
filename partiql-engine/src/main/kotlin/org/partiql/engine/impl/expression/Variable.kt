package org.partiql.engine.impl.expression

import org.partiql.engine.impl.PhysicalNode
import org.partiql.engine.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class Variable(private val index: Int) : PhysicalNode.Expression {
    @PartiQLValueExperimental
    override fun evaluate(record: Record): PartiQLValue {
        return record.values[index]
    }
}

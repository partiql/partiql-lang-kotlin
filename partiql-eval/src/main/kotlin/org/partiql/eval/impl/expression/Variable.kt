package org.partiql.eval.impl.expression

import org.partiql.eval.impl.PhysicalNode
import org.partiql.eval.impl.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class Variable(private val index: Int) : PhysicalNode.Expression {
    @PartiQLValueExperimental
    override fun evaluate(record: Record): PartiQLValue {
        return record.values[index]
    }
}

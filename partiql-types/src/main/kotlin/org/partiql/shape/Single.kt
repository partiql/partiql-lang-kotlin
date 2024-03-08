package org.partiql.shape

import org.partiql.shape.constraints.Constraint
import org.partiql.shape.constraints.None
import org.partiql.value.PartiQLType

public data class Single(
    override val type: PartiQLType,
    override val constraint: Constraint = None,
    override val metas: Set<PShape.Meta> = emptySet()
) : PShape {
    override fun validate(): PShape.ValidationResult {
        return constraint.validate(type)
    }
}

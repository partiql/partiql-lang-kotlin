package org.partiql.shape

import org.partiql.shape.constraints.Constraint
import org.partiql.shape.constraints.None
import org.partiql.value.AnyType
import org.partiql.value.PartiQLType

public data class Union private constructor(
    val shapes: Set<PShape>,
    override val type: PartiQLType = AnyType,
    override val constraint: Constraint = None,
    override val metas: Set<PShape.Meta> = emptySet(),
) : PShape {

    override fun validate(): PShape.ValidationResult {
        shapes.forEach { shape ->
            when (val result = shape.validate()) {
                is PShape.ValidationResult.Success -> { /* Do nothing */ }
                is PShape.ValidationResult.Failure -> return result
            }
        }
        return constraint.validate(type)
    }

    public companion object {
        public fun of(
            shapes: Set<PShape>,
            type: PartiQLType = AnyType,
            constraint: Constraint = None,
            metas: Set<PShape.Meta> = emptySet(),
        ): Union {
            val flattened: Set<PShape> = shapes.flatMap { subShape ->
                when (subShape) {
                    is Union -> subShape.shapes
                    else -> setOf(subShape)
                }
            }.toSet()
            return Union(flattened, type, constraint, metas)
        }
    }
}

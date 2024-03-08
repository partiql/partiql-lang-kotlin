package org.partiql.shape.constraints

import org.partiql.shape.PShape
import org.partiql.shape.errors.InternalError
import org.partiql.value.AnyType
import org.partiql.value.PartiQLType

/**
 * Equivalent to Ion Schema's ANY OF constraint
 *
 * On creation of [Union], we unnest any [Union] constraints within our [subShapes].
 */
public class Union(
    subShapes: Set<PShape>
) : Constraint {

    public val subShapes: Set<PShape> = subShapes.flatMap { subShape ->
        when (val constraint = subShape.constraint) {
            is Union -> constraint.subShapes
            else -> setOf(subShape)
        }
    }.toSet()

    override fun validate(type: PartiQLType): PShape.ValidationResult {
        return when (type) {
            is AnyType -> PShape.ValidationResult.Success
            else -> PShape.ValidationResult.Failure(
                InternalError("UNION was not an ANY type. It contained a $type")
            )
        }
    }
}

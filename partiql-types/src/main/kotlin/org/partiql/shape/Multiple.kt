package org.partiql.shape

import org.partiql.shape.errors.InternalError
import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.value.AnyType
import org.partiql.value.PartiQLType

/**
 * Equivalent to Ion Schema's ANY OF constraint
 *
 * On creation of [Multiple], we unnest any [Multiple] constraints within our [subShapes].
 * TODO: Don't use data class. Handwrite equals/hashcode/toString
 */
public data class Multiple private constructor(
    public val constraints: Set<Constraint>
) : Constraint {

    public companion object {
        @JvmStatic
        public fun of(constraints: Set<Constraint>): Constraint {
            return constraints.flatMap { constraint ->
                when (constraint) {
                    is Multiple -> constraint.constraints
                    else -> setOf(constraint)
                }
            }.toSet().let { flattened ->
                when (flattened.size) {
                    0 -> None
                    1 -> flattened.first()
                    else -> Multiple(flattened)
                }
            }
        }
    }

    override fun validate(type: PartiQLType): PShape.ValidationResult {
        return when (type) {
            is AnyType -> PShape.ValidationResult.Success
            else -> PShape.ValidationResult.Failure(
                InternalError("UNION was not an ANY type. It contained a $type")
            )
        }
    }

    override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}

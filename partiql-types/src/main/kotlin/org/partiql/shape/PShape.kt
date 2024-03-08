package org.partiql.shape

import org.partiql.shape.constraints.Constraint
import org.partiql.shape.constraints.None
import org.partiql.shape.constraints.Union
import org.partiql.value.AnyType
import org.partiql.value.PartiQLType

/**
 * TODO: Do we support validation of values eventually?
 */
public sealed interface PShape {

    public val type: PartiQLType

    public val constraint: Constraint

    @Deprecated("This is an example of how we can gather metadata.")
    public val metas: Set<Meta>

    public fun validate(): ValidationResult

    /**
     * TODO: Do we support this?
     */
    @Deprecated(
        "This is an example of how we can gather other descriptors such as PRIMARY KEY, etc. " +
            "It is distinct from Constraints."
    )
    public sealed interface Meta

    public sealed interface ValidationResult {

        public object Success : ValidationResult

        public data class Failure(
            val errors: List<org.partiql.shape.errors.Error>
        ) : ValidationResult {

            public constructor(vararg errors: org.partiql.shape.errors.Error) : this(errors.toList())
        }
    }

    public companion object {

        @Deprecated("This will move to planner.")
        public fun getTypes(shape: PShape): Set<PartiQLType> {
            return when (val constraint = shape.constraint) {
                is Union -> constraint.subShapes.flatMap { getTypes(it) }.toSet()
                else -> setOf(shape.type)
            }
        }

        @JvmStatic
        @JvmOverloads
        @Deprecated("Should we allow this?")
        public fun of(
            type: PartiQLType,
            constraint: Constraint = None,
            metas: Set<Meta> = emptySet()
        ): PShape {
            return Default(type, constraint, metas)
        }

        private class Default(
            override val type: PartiQLType = AnyType,
            override val constraint: Constraint = None,
            override val metas: Set<Meta> = emptySet()
        ) : PShape {
            override fun validate(): ValidationResult {
                return constraint.validate(type)
            }
        }
    }
}

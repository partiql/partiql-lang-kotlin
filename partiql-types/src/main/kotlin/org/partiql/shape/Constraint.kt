package org.partiql.shape

import org.partiql.shape.errors.TypeMismatchError
import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.PartiQLType
import org.partiql.value.TupleType

public sealed interface Constraint : ShapeNode {

    /**
     * Some Constraints require a specific type.
     */
    public fun validate(type: PartiQLType): PShape.ValidationResult

    public data class AnyOf(
        val shapes: Set<PShape>
    ) : Constraint {

        public constructor(vararg shapes: PShape) : this(shapes.toSet())

        override fun validate(type: PartiQLType): PShape.ValidationResult {
            TODO("Not yet implemented")
        }

        override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
            return visitor.visitConstraintAnyOf(this, ctx)
        }
    }

    public data class Element(
        val shape: PShape
    ) : Constraint {
        override fun validate(type: PartiQLType): PShape.ValidationResult {
            return when (type is BagType || type is ArrayType) {
                true -> PShape.ValidationResult.Success
                false -> PShape.ValidationResult.Failure(
                    TypeMismatchError(setOf(BagType, ArrayType), type)
                )
            }
        }

        override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
            return visitor.visitConstraintElement(this, ctx)
        }
    }

    public data class Fields(
        public val fields: List<Field>,
        public val isClosed: Boolean = false,
    ) : Constraint {

        @Deprecated("This should probably be a Meta, not a Constraint.")
        public val isOrdered: Boolean = false

        override fun validate(type: PartiQLType): PShape.ValidationResult {
            if (type !is TupleType) return PShape.ValidationResult.Failure(
                TypeMismatchError(TupleType, type)
            )
            return PShape.ValidationResult.Success
        }

        override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
            return visitor.visitConstraintFields(this, ctx)
        }

        public data class Field(
            public val key: String,
            public val value: PShape
        ) {
            public constructor(key: String, value: PartiQLType) : this(key, PShape.of(value))
        }
    }

    /**
     * SQL:1999's NOT NULL
     */
    public object NotNull : Constraint {
        override fun validate(type: PartiQLType): PShape.ValidationResult {
            return PShape.ValidationResult.Success
        }

        override fun toString(): String = "NOT_NULL"

        override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
            return visitor.visitConstraintNotNull(this, ctx)
        }
    }
}

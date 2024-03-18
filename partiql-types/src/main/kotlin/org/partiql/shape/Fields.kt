package org.partiql.shape

import org.partiql.shape.errors.TypeMismatchError
import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.value.PartiQLType
import org.partiql.value.TupleType

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

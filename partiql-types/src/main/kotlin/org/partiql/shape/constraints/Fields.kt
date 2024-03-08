package org.partiql.shape.constraints

import org.partiql.shape.PShape
import org.partiql.shape.errors.TypeMismatchError
import org.partiql.value.AnyType
import org.partiql.value.PartiQLType
import org.partiql.value.TupleType

public data class Fields(
    public val fields: List<Field>,
    public val isClosed: Boolean = false,
    @Deprecated("This should probably be a Meta, not a Constraint.")
    public val isOrdered: Boolean = false
) : Constraint {

    override fun validate(type: PartiQLType): PShape.ValidationResult {
        if (type !is TupleType) return PShape.ValidationResult.Failure(
            TypeMismatchError(TupleType(AnyType), type)
        )
        return PShape.ValidationResult.Success
    }

    public data class Field(
        public val key: String,
        public val value: VariablyOccurringPShape
    ) {
        public constructor(key: String, value: PShape) : this(key, shapeToVariablyOccurringPShape(value))

        public constructor(key: String, value: PartiQLType) : this(key, shapeToVariablyOccurringPShape(PShape.of(value)))

        public companion object {
            private fun shapeToVariablyOccurringPShape(value: PShape): VariablyOccurringPShape {
                return VariablyOccurringPShape(
                    min = VariablyOccurringPShape.RangeIndex.Literal(1),
                    max = VariablyOccurringPShape.RangeIndex.Literal(1),
                    shape = value
                )
            }
        }
    }

    /**
     * The [min] and [max] are inclusive.
     */
    @Deprecated("Do we need this?")
    public data class VariablyOccurringPShape(
        public val min: RangeIndex,
        public val max: RangeIndex,
        public val shape: PShape
    ) {
        public sealed interface RangeIndex {
            public data class Literal(
                public val index: Int
            ) : RangeIndex

            /**
             * Zero
             */
            public object Minimum : RangeIndex

            /**
             * Positive infinity
             */
            public object Maximum : RangeIndex
        }
    }
}

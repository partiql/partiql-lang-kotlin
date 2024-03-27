package org.partiql.planner.internal.shape

import org.partiql.shape.Constraint
import org.partiql.shape.PShape
import org.partiql.shape.PShape.Companion.allShapes
import org.partiql.shape.PShape.Companion.copy
import org.partiql.shape.PShape.Companion.getFirstAndOnlyFields
import org.partiql.types.StaticType
import org.partiql.value.TupleType

internal object ShapeUtils {

    internal fun isOrderedTuple(shape: PShape): Boolean {
        if (shape.type != TupleType) {
            return false
        }
        return shape.allShapes().all { isOrderedTupleStrict(it) }
    }

    internal fun addOrdering(shape: PShape): PShape {
        return when (shape.type is TupleType) {
            true -> {
                val fields = shape.getFirstAndOnlyFields()
                val constraints = shape.constraints.filterNot { it is Constraint.Fields }.toSet() + setOfNotNull(fields?.copy(isOrdered = true))
                shape.copy(constraints = constraints)
            }
            false -> shape
        }
    }

    /**
     * Converts [StaticType] to [PShape]
     */
    @Deprecated("Should not be used")
    public fun fromStaticType(type: StaticType): PShape = PShape.fromStaticType(type)

    private fun isOrderedTupleStrict(shape: PShape): Boolean {
        if (shape.type != TupleType) {
            return false
        }
        return shape.getFirstAndOnlyFields()?.isOrdered ?: false
    }
}

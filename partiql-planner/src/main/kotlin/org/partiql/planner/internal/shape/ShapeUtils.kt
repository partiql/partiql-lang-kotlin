package org.partiql.planner.internal.shape

import org.partiql.shape.Element
import org.partiql.shape.Fields
import org.partiql.shape.NotNull
import org.partiql.shape.PShape
import org.partiql.shape.PShape.Companion.allShapes
import org.partiql.shape.PShape.Companion.asNullable
import org.partiql.types.AnyOfType
import org.partiql.types.CollectionType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.DynamicType
import org.partiql.value.PartiQLType
import org.partiql.value.TupleType

internal object ShapeUtils {

    internal fun isOrderedTuple(shape: PShape): Boolean {
        if (shape.type != TupleType) {
            return false
        }
        return shape.allShapes().all { isOrderedTupleStrict(it) }
    }

    /**
     * Converts [StaticType] to [PShape]
     */
    @Deprecated("Should not be used")
    public fun fromStaticType(type: StaticType): PShape {
        return when (type) {
            is SingleType -> when (type) {
                is StructType -> {
                    val pType = PartiQLType.fromSingleType(type)
                    val fields = type.fields.map { Fields.Field(it.key, fromStaticType(it.value)) }
                    PShape.of(
                        type = pType,
                        constraints = setOf(
                            Fields(
                                fields = fields,
                                isClosed = type.contentClosed,
                                // TODO: isOrdered = type.constraints.contains(TupleConstraint.Ordered)
                            ),
                            NotNull
                        ),
                        metas = when (type.constraints.contains(TupleConstraint.Ordered)) {
                            true -> setOf(IsOrdered)
                            false -> emptySet()
                        }
                    )
                }
                is CollectionType -> {
                    val element = type.elementType
                    val pType = when (type) {
                        is org.partiql.types.BagType -> BagType
                        is ListType -> ArrayType
                        is SexpType -> ArrayType
                    }
                    PShape.of(
                        type = pType,
                        constraints = setOf(
                            Element(fromStaticType(element)),
                            NotNull
                        )
                    )
                }
                is org.partiql.types.NullType -> PShape.of(PartiQLType.fromSingleType(type))
                else -> PShape.of(
                    PartiQLType.fromSingleType(type),
                    constraints = setOf(NotNull)
                )
            }
            is org.partiql.types.AnyType -> PShape.of(DynamicType)
            is AnyOfType -> {
                val flattened = type.flatten().allTypes
                val types = when (flattened.any { it is org.partiql.types.NullType }) {
                    true -> flattened.filterNot { it is org.partiql.types.NullType }.map { child ->
                        fromStaticType(child).asNullable()
                    }
                    false -> flattened.map { child ->
                        fromStaticType(child)
                    }
                }.toSet()
                PShape.anyOf(types)
            }
        }
    }

    private fun isOrderedTupleStrict(shape: PShape): Boolean {
        if (shape.type != TupleType) {
            return false
        }
        return shape.metas.contains(IsOrdered)
    }
}

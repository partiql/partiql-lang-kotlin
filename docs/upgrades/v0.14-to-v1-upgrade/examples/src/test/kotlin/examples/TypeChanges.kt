package examples

import org.partiql.types.AnyOfType
import org.partiql.types.BoolType
import org.partiql.types.DecimalType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StructType
import org.partiql.types.StructType.Field
import org.partiql.types.TupleConstraint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TypeChanges {
    @Test
    fun `checking a type`() {
        val someBool: StaticType = StaticType.BOOL
        val someNonBool: StaticType = StaticType.DATE
        // Determine if `t` is a boolean type
        fun isBoolType(t: StaticType): Boolean {
            return when (t) {
                is BoolType -> {
                    println("is bool type")
                    true
                }
                is AnyOfType -> {
                    isBoolType(t.flatten())
                } // need an additional check if `t` is enclosed in a union
                else -> {
                    println("is not a bool type")
                    false
                }
            }
        }
        assertEquals(true, isBoolType(someBool))
        assertEquals(false, isBoolType(someNonBool))
    }

    @Test
    fun `getting a type parameter`() {
        // Defining a `DECIMAL(5, 2)` type
        val decimalType: StaticType = DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(precision = 5, scale = 2))
        // Extracting the precision and scale assuming `decimalType` is a `StaticType`
        fun getPrecisionAndScale(t: StaticType): Pair<Int, Int>? {
            return when (t) {
                is DecimalType -> {
                    when (val constraint = t.precisionScaleConstraint) {
                        is DecimalType.PrecisionScaleConstraint.Constrained -> {
                            constraint.precision to constraint.scale
                        }
                        else -> null
                    }
                }
                is AnyOfType -> getPrecisionAndScale(t.flatten())
                else -> null
            }
        }
        val precAndScale = getPrecisionAndScale(decimalType)
        assertNotNull(precAndScale)
        assertEquals(5, precAndScale.first)
        assertEquals(2, precAndScale.second)
    }

    @Test
    fun `extracting a type from a union`() {
        // Assume we have a type (potentially nullable or missable), where we want to extract the non-null type
        val someType: StaticType = unionOf(StaticType.NULL, StaticType.MISSING, StaticType.INT4)
        val baseType = when (someType) {
            is AnyOfType -> {
                someType.types.first { it !is NullType && it !is MissingType }
            }
            else -> someType
        }
        assertEquals(baseType, StaticType.INT4)
    }

    @Test
    fun `defining a column type that is null or missing`() {
        // In PLK 0.14.9, NULL and MISSING are their own types
        val nullType: StaticType = StaticType.NULL
        val missingType: StaticType = StaticType.MISSING
    }

    @Test
    fun `defining an ordered, closed struct`() {
        // In PLK 0.14.9, the following properties and constraints need to be set to define an ordered, closed struct
        val orderedClosedStruct: StaticType = StructType(
            fields = listOf(Field("a", StaticType.INT4)),
            contentClosed = true,
            primaryKeyFields = listOf(),
            constraints = setOf(TupleConstraint.Ordered),
            metas = emptyMap()
        )
        assertTrue(orderedClosedStruct is StructType)
        assertEquals(1, orderedClosedStruct.fields.size)
        val fields = orderedClosedStruct.fields
        assertEquals("a", fields.first().key)
        assertEquals(StaticType.INT4, fields.first().value)
    }
}

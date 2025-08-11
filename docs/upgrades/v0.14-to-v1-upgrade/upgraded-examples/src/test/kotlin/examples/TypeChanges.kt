package examples

import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TypeChanges {
    @Test
    fun `checking a type`() {
        val someBool: PType = PType.bool()
        val someNonBool: PType = PType.date()
        // Determine if `t` is a boolean type
        fun isBoolType(t: PType): Boolean {
            return when (t.code()) {
                PType.BOOL -> {
                    println("is bool type")
                    true
                }
                // v1 does not have type union support
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
        val decimalType: PType = PType.decimal(5, 2)
        // Extracting the precision and scale assuming `decimalType` is a `PType`
        fun getPrecisionAndScale(t: PType): Pair<Int, Int>? {
            return when (t.code()) {
                PType.DECIMAL -> {
                    t.precision to t.scale
                }
                // v1 does not have type union support
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
        // In v1, all types are nullable and missable by default
        val someType: PType = PType.integer()
        val baseType = someType
        assertEquals(baseType, PType.integer())
    }

    @Test
    fun `defining a column type that is null or missing`() {
        // In PLK v1, we use `PType.UNKNOWN` to represent a column that is always null or missing
        val nullType: PType = PType.unknown()
        val missingType: PType = PType.unknown()
    }

    @Test
    fun `defining an ordered, closed struct`() {
        // In PLK v1, a `PType.ROW` defines an ordered, closed struct
        val orderedClosedStruct: PType = PType.row(
            PTypeField.of(
                "a",
                PType.integer()
            )
        )
        assertTrue(orderedClosedStruct.code() == PType.ROW)
        assertEquals(1, orderedClosedStruct.fields.size)
        val fields = orderedClosedStruct.fields
        assertEquals("a", fields.first().name)
        assertEquals(PType.integer(), fields.first().type)
    }
}

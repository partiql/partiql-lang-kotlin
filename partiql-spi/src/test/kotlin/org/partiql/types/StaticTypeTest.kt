package org.partiql.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class StaticTypeTest {
    @Test
    fun collectionWithConstraintInitTest() {
        val struct = StructType(
            fields = mapOf(
                "a" to StaticType.STRING,
            ),
            constraints =
            setOf(
                TupleConstraint.Open(false),
                TupleConstraint.UniqueAttrs(false),
            )
        )
        val constraint = setOf(CollectionConstraint.PrimaryKey(setOf("a")))
        val types = listOf(
            BagType(
                elementType = struct,
                metas = mapOf(),
                constraints = constraint
            ),
            ListType(
                elementType = struct,
                metas = mapOf(),
                constraints = constraint
            ),
            SexpType(
                elementType = struct,
                metas = mapOf(),
                constraints = constraint
            )
        )

        types.forEach {
            assertEquals(it.elementType.toString(), "struct(a: string, [Open(value=false), UniqueAttrs(value=false)])")
        }

        assertEquals(
            BagType(elementType = StaticType.INT, metas = mapOf(), constraints = setOf()).toString(),
            "bag(int)"
        )

        assertThrows<UnsupportedTypeConstraint> {
            BagType(
                StaticType.INT,
                metas = mapOf(),
                constraints = setOf(
                    CollectionConstraint.PrimaryKey(setOf("a"))
                )
            )
        }
    }
}

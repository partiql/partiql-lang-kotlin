package org.partiql.types

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SchemaTypeTest {
    // Just an innocent test to show the usage
    @Test
    fun toStringTest() {
        val schema: SchemaType = SchemaType(
            StructType(
                fields = mapOf(
                    "a" to StaticType.STRING,
                ),
                constraints =
                setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(false),
                )
            )
        )

        assertEquals(schema.toString(), "bag(struct(a: string, [Open(value=false), UniqueAttrs(value=false)]))")
    }
}

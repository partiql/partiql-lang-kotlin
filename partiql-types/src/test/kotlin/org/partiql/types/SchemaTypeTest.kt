package org.partiql.types

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SchemaTypeTest {
    // Just an innocent test to show the usage
    @Test
    fun tooStringTest() {
        val schema: SchemaType = SchemaType(
            StructType(
                fields = mapOf(
                    "a" to StaticType.STRING,
                ),
                constraints =
                setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.PrimaryKey(setOf("a")),
                )
            )
        )

        assertEquals(schema.toString(), "bag(struct(a: string, [ClosedSchema(value=true), PrimaryKey(attrs=[a])]))")
    }
}

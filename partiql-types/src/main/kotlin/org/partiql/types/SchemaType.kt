package org.partiql.types

typealias SchemaType = BagType

fun test() {
    val schema: SchemaType = SchemaType(
        StructType(
            mapOf(
                "a" to StaticType.STRING), constraints =
            setOf(
                    TupleSchemaConstraint.ClosedSchema(true),
                    TupleSchemaConstraint.PrimaryKey(setOf("a"))
                )
            )
    )
}
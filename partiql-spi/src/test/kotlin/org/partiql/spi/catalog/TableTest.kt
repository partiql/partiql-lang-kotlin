package org.partiql.spi.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField

class TableTest {

    private val rowSchema = PType.row(
        PTypeField.of("id", PType.integer()),
        PTypeField.of("name", PType.string()),
        PTypeField.of("score", PType.decimal(10, 2)),
    )

    // -----------------------------------------------
    // Unordered schema: BAG<ROW(...)>
    // -----------------------------------------------

    @Test
    fun `empty table with unordered schema`() {
        val schema = PType.bag(rowSchema)
        val table = Table.empty("users", schema)
        assertEquals("users", table.getName().getName())
        assertEquals(PType.BAG, table.getSchema().code())
        val inner = table.getSchema().typeParameter
        assertEquals(PType.ROW, inner.code())
        val fields = inner.fields.toList()
        assertEquals(3, fields.size)
        assertEquals("id", fields[0].name)
        assertEquals(PType.INTEGER, fields[0].type.code())
        assertEquals("name", fields[1].name)
        assertEquals(PType.STRING, fields[1].type.code())
        assertEquals("score", fields[2].name)
        assertEquals(PType.DECIMAL, fields[2].type.code())
    }

    @Test
    fun `standard table with unordered schema`() {
        val schema = PType.bag(rowSchema)
        val table = Table.standard(Name.of("users"), schema, org.partiql.spi.value.Datum.nullValue())
        assertEquals(PType.BAG, table.getSchema().code())
        assertEquals(PType.ROW, table.getSchema().typeParameter.code())
    }

    // -----------------------------------------------
    // Ordered schema: ARRAY<ROW(...)>
    // -----------------------------------------------

    @Test
    fun `empty table with ordered schema`() {
        val schema = PType.array(rowSchema)
        val table = Table.empty("events", schema)
        assertEquals("events", table.getName().getName())
        assertEquals(PType.ARRAY, table.getSchema().code())
        val inner = table.getSchema().typeParameter
        assertEquals(PType.ROW, inner.code())
        val fields = inner.fields.toList()
        assertEquals(3, fields.size)
        assertEquals("id", fields[0].name)
        assertEquals("name", fields[1].name)
        assertEquals("score", fields[2].name)
    }

    @Test
    fun `standard table with ordered schema`() {
        val schema = PType.array(rowSchema)
        val table = Table.standard(Name.of("events"), schema, org.partiql.spi.value.Datum.nullValue())
        assertEquals(PType.ARRAY, table.getSchema().code())
        assertEquals(PType.ROW, table.getSchema().typeParameter.code())
    }

    // -----------------------------------------------
    // Default schema
    // -----------------------------------------------

    @Test
    fun `empty table without schema defaults to DYNAMIC`() {
        val table = Table.empty("t")
        assertEquals(PType.DYNAMIC, table.getSchema().code())
    }

    // -----------------------------------------------
    // Builder
    // -----------------------------------------------

    @Test
    fun `builder with unordered schema`() {
        val schema = PType.bag(rowSchema)
        val table = Table.builder()
            .name("products")
            .schema(schema)
            .build()
        assertEquals("products", table.getName().getName())
        assertEquals(PType.BAG, table.getSchema().code())
        assertEquals(PType.ROW, table.getSchema().typeParameter.code())
        assertEquals(3, table.getSchema().typeParameter.fields.size)
    }

    @Test
    fun `builder with ordered schema`() {
        val schema = PType.array(rowSchema)
        val table = Table.builder()
            .name("logs")
            .schema(schema)
            .build()
        assertEquals(PType.ARRAY, table.getSchema().code())
        assertEquals(PType.ROW, table.getSchema().typeParameter.code())
    }

    @Test
    fun `builder without name throws`() {
        assertThrows(IllegalStateException::class.java) {
            Table.builder().schema(PType.bag(rowSchema)).build()
        }
    }

    // -----------------------------------------------
    // Schema with nested types
    // -----------------------------------------------

    @Test
    fun `table with nested collection in row schema`() {
        val nestedRow = PType.row(
            PTypeField.of("id", PType.integer()),
            PTypeField.of("tags", PType.array(PType.string())),
            PTypeField.of("metadata", PType.struct()),
        )
        val schema = PType.bag(nestedRow)
        val table = Table.empty("items", schema)
        val fields = table.getSchema().typeParameter.fields.toList()
        assertEquals(3, fields.size)
        assertEquals(PType.ARRAY, fields[1].type.code())
        assertEquals(PType.STRING, fields[1].type.typeParameter.code())
        assertEquals(PType.STRUCT, fields[2].type.code())
    }

    // -----------------------------------------------
    // Schema round-trip with DDL
    // -----------------------------------------------

    @Test
    fun `table schema round-trips through DDL`() {
        val schema = PType.bag(rowSchema)
        val table = Table.empty("users", schema)
        val ddl = table.getSchema().toDDL()
        assertEquals("BAG<ROW(id INTEGER, name STRING, score DECIMAL(10, 2))>", ddl)
        val restored = PType.fromDDL(ddl)
        assertEquals(PType.BAG, restored.code())
        assertEquals(PType.ROW, restored.typeParameter.code())
        assertEquals(3, restored.typeParameter.fields.size)
    }

    @Test
    fun `ordered table schema round-trips through DDL`() {
        val schema = PType.array(rowSchema)
        val table = Table.empty("events", schema)
        val ddl = table.getSchema().toDDL()
        assertEquals("ARRAY<ROW(id INTEGER, name STRING, score DECIMAL(10, 2))>", ddl)
        val restored = PType.fromDDL(ddl)
        assertEquals(PType.ARRAY, restored.code())
        assertEquals(PType.ROW, restored.typeParameter.code())
    }
}

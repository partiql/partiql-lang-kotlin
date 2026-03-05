package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import kotlin.test.assertEquals

class TrimTest {

    @Test
    fun `trim preserves CHAR length and returns VARCHAR type`() {
        val actualType = FnTestUtils.getQueryResultType("TRIM(CAST('  HELLO  ' AS CHAR(9)))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(9, actualType.length)
    }

    @Test
    fun `trim preserves VARCHAR length and type`() {
        val actualType = FnTestUtils.getQueryResultType("TRIM(CAST('  HELLO  ' AS VARCHAR(15)))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(15, actualType.length)
    }

    @Test
    fun `trim preserves CLOB length and type`() {
        val actualType = FnTestUtils.getQueryResultType("TRIM(CAST('  HELLO  ' AS CLOB(20)))")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(20, actualType.length)
    }

    @Test
    fun `trim preserves STRING type`() {
        val actualType = FnTestUtils.getQueryResultType("TRIM('  HELLO  ')")
        assertEquals(PType.STRING, actualType.code())
    }

    @Test
    fun `trim with unsupported type returns DYNAMIC`() {
        val actualType = FnTestUtils.getQueryResultType("TRIM(42)")
        assertEquals(PType.DYNAMIC, actualType.code())
    }
}

package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import kotlin.test.assertEquals

class LowerTest {

    @Test
    fun `lower preserves CHAR length and type`() {
        val actualType = FnTestUtils.getQueryResultType("LOWER(CAST('HELLO' AS CHAR(5)))")
        assertEquals(PType.CHAR, actualType.code())
        assertEquals(5, actualType.length)
    }

    @Test
    fun `lower preserves VARCHAR length and type`() {
        val actualType = FnTestUtils.getQueryResultType("LOWER(CAST('HELLO ' AS VARCHAR(10)))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(10, actualType.length)
    }

    @Test
    fun `lower preserves CLOB length and type`() {
        val actualType = FnTestUtils.getQueryResultType("LOWER(CAST(' HELLO' AS CLOB(20)))")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(20, actualType.length)
    }

    @Test
    fun `lower preserves STRING type`() {
        val actualType = FnTestUtils.getQueryResultType("LOWER('HELLO')")
        assertEquals(PType.STRING, actualType.code())
    }

    @Test
    fun `lower with unsupported type throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("LOWER(42)")
        }
    }
}

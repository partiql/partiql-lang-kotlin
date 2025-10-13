package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import kotlin.test.assertEquals

class UpperTest {

    @Test
    fun `upper preserves CHAR length and type`() {
        val actualType = FnTestUtils.getQueryResultType("UPPER(CAST('hello' AS CHAR(5)))")
        assertEquals(PType.CHAR, actualType.code())
        assertEquals(5, actualType.length)
    }

    @Test
    fun `upper preserves VARCHAR length and type`() {
        val actualType = FnTestUtils.getQueryResultType("UPPER(CAST('hello ' AS VARCHAR(10)))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(10, actualType.length)
    }

    @Test
    fun `upper preserves CLOB length and type`() {
        val actualType = FnTestUtils.getQueryResultType("UPPER(CAST(' hello' AS CLOB(20)))")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(20, actualType.length)
    }

    @Test
    fun `upper preserves STRING type`() {
        val actualType = FnTestUtils.getQueryResultType("UPPER(' hello ')")
        assertEquals(PType.STRING, actualType.code())
    }

    @Test
    fun `upper with unsupported type throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("UPPER(42)")
        }
    }
}

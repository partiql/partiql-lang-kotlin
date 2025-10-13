package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.errors.PRuntimeException

// Basic concat tests are covered in OpConcatTest.kt
class ConcatTest {
    private val maxInt = Int.MAX_VALUE

    @Test
    fun `concat with VARCHAR overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS VARCHAR($maxInt)) || CAST('a' AS VARCHAR(1))")
        }
    }

    @Test
    fun `concat with CHAR overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS CHAR($maxInt)) || CAST('a' AS CHAR(1))")
        }
    }

    @Test
    fun `concat with CLOB overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS CLOB($maxInt)) || CAST('a' AS CLOB(1))")
        }
    }

    @Test
    fun `concat with VARCHAR and CHAR overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS VARCHAR($maxInt)) || CAST('a' AS CHAR(1))")
        }
    }

    @Test
    fun `concat with CLOB and CHAR overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS CLOB($maxInt)) || CAST('a' AS CHAR(1))")
        }
    }

    @Test
    fun `concat with CLOB and VARCHAR overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS CLOB($maxInt)) || CAST('a' AS VARCHAR(1))")
        }
    }
}

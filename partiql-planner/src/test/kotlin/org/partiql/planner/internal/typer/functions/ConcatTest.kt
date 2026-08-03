package org.partiql.planner.internal.typer.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import kotlin.test.assertEquals

// Basic concat tests are covered in OpConcatTest.kt
/**
 * The result length of `||` is L1 + L2. When that sum exceeds the maximum length, the behavior splits
 * along the spec's fixed- vs variable-length distinction (SQL2023 section 6.32):
 * - CHAR is fixed-length, so the sum must be representable and an over-long one raises.
 * - VARCHAR/CLOB declare an upper bound, so the sum clamps to the maximum length.
 */
class ConcatTest {
    private val maxInt = Int.MAX_VALUE

    @Test
    fun `concat with CHAR overflow throws exception`() {
        assertThrows<PRuntimeException> {
            FnTestUtils.getQueryResultType("CAST('some string' AS CHAR($maxInt)) || CAST('a' AS CHAR(1))")
        }
    }

    @Test
    fun `concat with VARCHAR overflow clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("CAST('some string' AS VARCHAR($maxInt)) || CAST('a' AS VARCHAR(1))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    @Test
    fun `concat with CLOB overflow clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("CAST('some string' AS CLOB($maxInt)) || CAST('a' AS CLOB(1))")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    @Test
    fun `concat with VARCHAR and CHAR overflow clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("CAST('some string' AS VARCHAR($maxInt)) || CAST('a' AS CHAR(1))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    @Test
    fun `concat with CLOB and CHAR overflow clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("CAST('some string' AS CLOB($maxInt)) || CAST('a' AS CHAR(1))")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    @Test
    fun `concat with CLOB and VARCHAR overflow clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("CAST('some string' AS CLOB($maxInt)) || CAST('a' AS VARCHAR(1))")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    /**
     * An unbounded CLOB defaults to the maximum length, so `CLOB || CLOB` sums to twice the maximum.
     * Clamping is what makes this resolve at all — it used to fail as an overflow.
     */
    @Test
    fun `concat of two unbounded CLOBs clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("CAST('a' AS CLOB) || CAST('b' AS CLOB)")
        assertEquals(PType.CLOB, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    /**
     * REPLACE's result length is not computable at plan time, so it is typed as unbounded VARCHAR;
     * concatenating onto it clamps rather than raising.
     */
    @Test
    fun `concat with an unbounded function result clamps to max length`() {
        val actualType = FnTestUtils.getQueryResultType("replace(CAST('abc' AS VARCHAR(5)), 'a', 'z') || CAST('x' AS VARCHAR(1))")
        assertEquals(PType.VARCHAR, actualType.code())
        assertEquals(maxInt, actualType.length)
    }

    /**
     * Sums below the maximum are unaffected by clamping and stay exact.
     */
    @Test
    fun `concat below the maximum length keeps the exact sum`() {
        val varchars = FnTestUtils.getQueryResultType("CAST('a' AS VARCHAR(3)) || CAST('b' AS VARCHAR(4))")
        assertEquals(PType.VARCHAR, varchars.code())
        assertEquals(7, varchars.length)

        val chars = FnTestUtils.getQueryResultType("CAST('a' AS CHAR(3)) || CAST('b' AS CHAR(4))")
        assertEquals(PType.CHAR, chars.code())
        assertEquals(7, chars.length)
    }
}

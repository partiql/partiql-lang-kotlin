package org.partiql.lang.eval

import org.junit.Test
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import kotlin.test.assertEquals
import kotlin.test.fail

/** Tests permissive and legacy modes of [ErrorSignaler] implementations. */
class ErrorSignalerTests {
    private val dummyMetas = metaContainerOf(SourceLocationMeta(4, 2))

    @Test
    fun permissiveTest() {
        val b = TypingMode.PERMISSIVE.createErrorSignaler()

        assertEquals(50, runTest(b, 5).intValue())
        assertEquals(70, runTest(b, 7).intValue())
        assertEquals(ExprValueType.MISSING, runTest(b, 6).type)
    }

    @Test
    fun legacyTest() {
        val b = TypingMode.LEGACY.createErrorSignaler()

        assertEquals(10, runTest(b, 1).intValue())
        val ex = try {
            runTest(b, 6)
            fail("Didn't throw")
        } catch (ex: EvaluationException) {
            ex
        }
        assertEquals(ex.errorCode, ErrorCode.EVALUATOR_CAST_FAILED)
        assertEquals(ex.errorContext[Property.LINE_NUMBER]!!.longValue(), 4L)
        assertEquals(ex.errorContext[Property.COLUMN_NUMBER]!!.longValue(), 2L)
    }

    private fun runTest(ctx1: ErrorSignaler, value: Int): ExprValue =
        ctx1.errorIf(
            value == 6,
            // The choice of ErrorCode.EVALUATOR_CAST_FAILED is arbitrary just for this test.
            ErrorCode.EVALUATOR_CAST_FAILED,
            { ErrorDetails(dummyMetas, "The value can't be 6") },
            { exprInt(value * 10) }
        )
}

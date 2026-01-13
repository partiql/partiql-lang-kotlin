package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.partiql.ast.Query
import org.partiql.ast.expr.ExprError
import org.partiql.spi.Context
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for datetime field keywords (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND) used in expression contexts.
 * These keywords are reserved tokens in the grammar and should be converted to ExprError nodes
 * when used as expressions, with appropriate error reporting to the listener.
 */
class PartiQLParserDatetimeFieldTests {

    private val parser = PartiQLParserDefault()

    /**
     * Test that YEAR keyword in expression context produces ExprError node.
     */
    @Test
    fun yearKeywordAsExpression() = assertDatetimeFieldExpression("YEAR")

    /**
     * Test that MONTH keyword in expression context produces ExprError node.
     */
    @Test
    fun monthKeywordAsExpression() = assertDatetimeFieldExpression("MONTH")

    /**
     * Test that DAY keyword in expression context produces ExprError node.
     */
    @Test
    fun dayKeywordAsExpression() = assertDatetimeFieldExpression("DAY")

    /**
     * Test that HOUR keyword in expression context produces ExprError node.
     */
    @Test
    fun hourKeywordAsExpression() = assertDatetimeFieldExpression("HOUR")

    /**
     * Test that MINUTE keyword in expression context produces ExprError node.
     */
    @Test
    fun minuteKeywordAsExpression() = assertDatetimeFieldExpression("MINUTE")

    /**
     * Test that SECOND keyword in expression context produces ExprError node.
     */
    @Test
    fun secondKeywordAsExpression() = assertDatetimeFieldExpression("SECOND")

    /**
     * Test that lowercase datetime field keywords also produce ExprError nodes.
     */
    @Test
    fun lowercaseDatetimeFieldAsExpression() = assertDatetimeFieldExpression("year", "YEAR")

    /**
     * Test that mixed case datetime field keywords also produce ExprError nodes.
     */
    @Test
    fun mixedCaseDatetimeFieldAsExpression() = assertDatetimeFieldExpression("YeAr", "YEAR")

    /**
     * Test that error listener receives UNEXPECTED_TOKEN error for datetime field keywords.
     */
    @Test
    fun errorListenerReceivesDatetimeFieldError() {
        val errors = mutableListOf<PError>()
        val listener = PErrorListener { error -> errors.add(error) }
        val ctx = Context.of(listener)
        parser.parse("YEAR", ctx)

        assertEquals(1, errors.size, "Expected exactly one error to be reported")
        val error = errors[0]
        assertEquals(PError.UNEXPECTED_TOKEN, error.code(), "Expected UNEXPECTED_TOKEN error code")
        assertEquals("YEAR", error.getOrNull("TOKEN_NAME", String::class.java), "Expected TOKEN_NAME property to be 'YEAR'")
    }

    /**
     * Test that all datetime field keywords report errors to listener.
     */
    @Test
    fun allDatetimeFieldsReportErrors() {
        val datetimeFields = listOf("YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND")

        for (field in datetimeFields) {
            val errors = mutableListOf<PError>()
            val listener = PErrorListener { error -> errors.add(error) }
            val ctx = Context.of(listener)
            parser.parse(field, ctx)

            assertEquals(1, errors.size, "Expected exactly one error for $field")
            assertEquals(PError.UNEXPECTED_TOKEN, errors[0].code(), "Expected UNEXPECTED_TOKEN for $field")
            assertEquals(field, errors[0].getOrNull("TOKEN_NAME", String::class.java), "Expected TOKEN_NAME to be $field")
        }
    }

    /**
     * Helper function to assert that a datetime field keyword produces an ExprError node.
     */
    private fun assertDatetimeFieldExpression(input: String, expectedFieldName: String = input.uppercase()) {
        val errors = mutableListOf<PError>()
        val listener = PErrorListener { error -> errors.add(error) }
        val ctx = Context.of(listener)
        val result = parser.parse(input, ctx)

        assertEquals(1, result.statements.size, "Expected exactly one statement")

        val statement = result.statements[0]
        assertTrue(statement is Query, "Expected Query statement")

        val query = statement
        val expr = query.expr
        assertTrue(expr is ExprError, "Expected ExprError expression, got ${expr::class.simpleName}")

        val exprError = expr
        assertEquals(expectedFieldName, exprError.message, "Expected message to be '$expectedFieldName'")
        assertEquals(ExprError.DATETIME_FIELD_KEYWORD, exprError.code, "Expected DATETIME_FIELD_KEYWORD error code")

        // Verify error was reported to listener
        assertEquals(1, errors.size, "Expected exactly one error to be reported")
        val error = errors[0]
        assertEquals(PError.UNEXPECTED_TOKEN, error.code(), "Expected UNEXPECTED_TOKEN error code")
        assertEquals(expectedFieldName, error.getOrNull("TOKEN_NAME", String::class.java), "Expected TOKEN_NAME property to be '$expectedFieldName'")
    }
}

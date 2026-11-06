package org.partiql.ast.sql

import org.junit.jupiter.api.Test
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprVarRef
import kotlin.test.assertEquals

/**
 * [SqlDialect] quotes and escapes names and string-based literal values built through the AST API, so that they re-parse
 * as the same value; see also `SqlDialectInjectionTests` in partiql-parser.
 */
class SqlDialectEscapingTest {

    private fun lit(lit: Literal) = ExprLit(lit).sql(SqlLayout.ONELINE)

    private fun ref(id: Identifier) = ExprVarRef(id, false).sql(SqlLayout.ONELINE)

    @Test
    fun delimitedIdentifierDoublesQuotes() = assertEquals("\"a\"\"b\"", ref(Identifier.delimited("a\"b")))

    @Test
    fun regularIdentifierIsVerbatimOnlyIfValid() {
        assertEquals("foo_1\$", ref(Identifier.regular("foo_1\$")))
        assertEquals("\"a b\"", ref(Identifier.regular("a b")))
        assertEquals("\"1a\"", ref(Identifier.regular("1a")))
    }

    @Test
    fun typedStringLiteralEscapesQuotes() =
        assertEquals("DATE '2020-01-01'' OR ''1''=''1'", lit(Literal.typedString(DataType.DATE(), "2020-01-01' OR '1'='1")))

    @Test
    fun intervalLiteralEscapesQuotes() = assertEquals(
        "INTERVAL '1'' OR ''1''=''1' YEAR",
        lit(Literal.typedString(DataType.INTERVAL(IntervalQualifier.Single(org.partiql.ast.DatetimeField.YEAR(), null, null)), "1' OR '1'='1")).trimEnd()
    )
}

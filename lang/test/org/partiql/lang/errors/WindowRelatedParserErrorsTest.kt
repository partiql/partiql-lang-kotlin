package org.partiql.lang.errors

import org.junit.Test
import org.partiql.lang.syntax.SqlParserTestBase
import org.partiql.lang.syntax.TokenType

// since window function is experimental and are in the development stage
// we treat this as a standalone class for easiness to change
// once the feature for window function is finalized, we will move this to [ParserErrorsTest]
class WindowRelatedParserErrorsTest : SqlParserTestBase() {
    @Test
    fun lagWithoutOrderBy() {
        checkInputThrowingParserException(
            "SELECT lag(a) OVER () FROM b",
            ErrorCode.PARSE_EXPECTED_WINDOW_ORDER_BY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("lag")
            ),
            targetParsers = setOf(ParserTypes.PARTIQL_PARSER)
        )
    }

    @Test
    fun lagWrongNumberOfParameter() {
        checkInputThrowingParserException(
            "SELECT lag(a,b,c,d) OVER (ORDER BY e) FROM f",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_TYPE to TokenType.COMMA,
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),
            targetParsers = setOf(ParserTypes.PARTIQL_PARSER)
        )
    }

    fun leadWithoutOrderBy() {
        checkInputThrowingParserException(
            "SELECT lead(a) OVER () FROM b",
            ErrorCode.PARSE_EXPECTED_WINDOW_ORDER_BY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("lag")
            ),
            targetParsers = setOf(ParserTypes.PARTIQL_PARSER)
        )
    }

    @Test
    fun leadWrongNumberOfParameter() {
        checkInputThrowingParserException(
            "SELECT lead(a,b,c,d) OVER (ORDER BY e) FROM f",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_TYPE to TokenType.COMMA,
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),
            targetParsers = setOf(ParserTypes.PARTIQL_PARSER)
        )
    }
}

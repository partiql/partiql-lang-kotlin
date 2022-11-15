package org.partiql.lang.errors

import org.junit.Test
import org.partiql.lang.syntax.PartiQLParserTestBase
import org.partiql.lang.syntax.TokenType

// TODO: move this to [ParserErrorsTest] once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved

class WindowRelatedParserErrorsTest : PartiQLParserTestBase() {
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
            )
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
            )
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
            )
        )
    }

    @Test
    fun leadWrongNumberOfParameter() {
        checkInputThrowingParserException(
            "SELECT lead(a,b,c,d) OVER (ORDER BY e) FROM f",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_TYPE to TokenType.COMMA,
                Property.TOKEN_VALUE to ion.newSymbol(",")
            )
        )
    }
}

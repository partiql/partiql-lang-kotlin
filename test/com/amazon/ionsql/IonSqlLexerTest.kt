/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ionsql.TokenType.*
import org.junit.Ignore
import org.junit.Test
import java.util.*

class IonSqlLexerTest : Base() {
    val lexer = IonSqlLexer(ion)

    fun token(type: TokenType, lit: String?, line: Long, column: Long): Token {
        val value = when (lit) {
            null -> null
            else -> literal(lit)
        }
        return Token(type, value, SourcePosition(line, column))
    }

    fun assertTokens(text: String, vararg tokens: Token) {
        val expected = listOf(*tokens)
        val actual = lexer.tokenize(text)
        assertEquals(expected, actual)
    }

    @Test
    fun punctuation() = assertTokens(
        "()[]{}:,.*",
        token(LEFT_PAREN, "'('", 1, 1),
        token(RIGHT_PAREN, "')'", 1, 2),
        token(LEFT_BRACKET, "'['", 1, 3),
        token(RIGHT_BRACKET, "']'", 1, 4),
        token(LEFT_CURLY, "'{'", 1, 5),
        token(RIGHT_CURLY, "'}'", 1, 6),
        token(COLON, "':'", 1, 7),
        token(COMMA, "','", 1, 8),
        token(DOT, "'.'", 1, 9),
        token(STAR, "'*'", 1, 10)
    )

    @Test
    fun whitespaceAndIdentifiers() = assertTokens(
        "ab\r\n_bc_  \r\r \$cd\$\n\r\tde1",
        token(IDENTIFIER, "ab", 1, 1),
        token(IDENTIFIER, "_bc_", 2, 1),
        token(IDENTIFIER, "\$cd\$", 4, 2),
        token(IDENTIFIER, "de1", 6, 2)
    )

    @Test
    fun booleans() = assertTokens(
        "true false truefalse",
        token(LITERAL, "true", 1, 1),
        token(LITERAL, "false", 1, 6),
        token(IDENTIFIER, "truefalse", 1, 12)
    )

    @Test
    fun numbers() = assertTokens(
        "500 600. 0.1 . .1 0000 0.00e0 1e+1",
        token(LITERAL, "500", 1, 1),
        token(LITERAL, "600d0", 1, 5),
        token(LITERAL, "1d-1", 1, 10),
        token(DOT, "'.'", 1, 14),
        token(LITERAL, "1d-1", 1, 16),
        token(LITERAL, "0", 1, 19),
        token(LITERAL, "0d-2", 1, 24),
        token(LITERAL, "1d1", 1, 31)
    )

    @Test
    fun signedNumbers() = assertTokens(
        "+500 -600. -0.1 . +.1 -0000 +0.00e0 -1e+1",
        token(LITERAL, "500", 1, 1),
        token(LITERAL, "-600d0", 1, 6),
        token(LITERAL, "-1d-1", 1, 12),
        token(DOT, "'.'", 1, 17),
        token(LITERAL, "1d-1", 1, 19),
        token(LITERAL, "0", 1, 23),
        token(LITERAL, "0d-2", 1, 29),
        token(LITERAL, "-1d1", 1, 37)
    )

    @Test
    fun quotedIon() = assertTokens(
        "`1e0` `{a:5}` `/*`*/\"`\"` `//`\n'''`'''`",
        token(LITERAL, "1e0", 1, 1),
        token(LITERAL, "{a:5}", 1, 7),
        token(LITERAL, "\"`\"", 1, 15),
        token(LITERAL, "\"`\"", 1, 26)
    )

    @Test
    fun quotedStrings() = assertTokens(
        "'1e0' '{''a'':5}'",
        token(LITERAL, "\"1e0\"", 1, 1),
        token(LITERAL, "\"{'a':5}\"", 1, 7)
    )

    @Test
    fun quotedIdentifiers() = assertTokens(
        "\"1e0\" \"{\"\"a\"\":5}\"",
        token(IDENTIFIER, "'1e0'", 1, 1),
        token(IDENTIFIER, "'{\"a\":5}'", 1, 7)
    )

    @Test
    fun operators() {
        val buf = StringBuilder()
        val expected = ArrayList<Token>()
        for (op in ALL_SINGLE_LEXEME_OPERATORS) {
            val type = when (op) {
                "*" -> STAR
                in ALL_OPERATORS -> OPERATOR
                in KEYWORDS -> KEYWORD
                else -> OPERATOR
            }
            expected.add(token(type, "'$op'", 1, buf.length + 1L))
            buf.append(op)
            buf.append(" ")
        }
        assertTokens(buf.toString(), *expected.toTypedArray())
    }

    @Test
    fun multiLexemeOperators() = assertTokens(
        "UNION ALL IS NOT union_all is_not",
        token(OPERATOR, "union_all", 1, 1),
        token(OPERATOR, "is_not", 1, 11),
        token(IDENTIFIER, "union_all", 1, 18),
        token(IDENTIFIER, "is_not", 1, 28)
    )

}

/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.syntax

import org.partiql.lang.*
import org.partiql.lang.syntax.TokenType.*
import org.junit.Test
import java.util.*

class SqlLexerTest : TestBase() {
    val lexer = SqlLexer(ion)

    // TODO: remove default value on [length] and specify the correct value for all invocations of this function
    fun token(type: TokenType, lit: String?, line: Long, column: Long, length: Long): Token {
        val value = when (lit) {
            null -> null
            else -> ion.singleValue(lit)
        }
        return Token(type, value, SourceSpan(line, column, length))
    }

    fun tokenize(text: String): Pair<Token, List<Token>> {
        val tokens = lexer.tokenize(text)
        return Pair(tokens[tokens.size - 1], tokens.dropLast(1))
    }

    fun assertTokens(text: String, vararg expectedTokens: Token) {
        val expected = listOf(*expectedTokens)
        val (eofToken, actualTokens) = tokenize(text)

        assertEquals("Expected and actualTokens number of tokens must match", expected.size, actualTokens.size)
        expectedTokens.zip(actualTokens).forEachIndexed { idx, (expected, actual) ->
            assertEquals("Token at position $idx must match", expected, actual)
        }
        assertEquals(TokenType.EOF, eofToken.type)
    }

    @Test
    fun punctuation() = assertTokens(
        "()[]{}:,.*<<>>;",
        token(LEFT_PAREN, "'('", 1, 1, 1),
        token(RIGHT_PAREN, "')'", 1, 2, 1),
        token(LEFT_BRACKET, "'['", 1, 3, 1),
        token(RIGHT_BRACKET, "']'", 1, 4, 1),
        token(LEFT_CURLY, "'{'", 1, 5, 1),
        token(RIGHT_CURLY, "'}'", 1, 6, 1),
        token(COLON, "':'", 1, 7, 1),
        token(COMMA, "','", 1, 8, 1),
        token(DOT, "'.'", 1, 9, 1),
        token(STAR, "'*'", 1, 10, 1),
        token(LEFT_DOUBLE_ANGLE_BRACKET, "'<<'", 1, 11, 2),
        token(RIGHT_DOUBLE_ANGLE_BRACKET, "'>>'", 1, 13, 2),
        token(SEMICOLON, "';'", 1, 15, 1)
    )

    @Test
    fun keywordsThatHaveTheirOwnTokenTypes() = assertTokens(
        "AS AT FOR",
            token(AS, "as", 1, 1, 2),
            token(AT, "at", 1, 4, 2),
            token(FOR, "for", 1, 7, 3)
    )

    @Test
    fun whitespaceAndIdentifiers() = assertTokens(
        "ab\r\n_bc_  \r\r \$cd\$\n\r\tde1",
        token(IDENTIFIER, "ab", 1, 1, 2),
        token(IDENTIFIER, "_bc_", 2, 1, 4),
        token(IDENTIFIER, "\$cd\$", 4, 2, 4),
        token(IDENTIFIER, "de1", 6, 2, 3)
    )

    @Test
    fun whitespaceAndQuotedIdentifiers() = assertTokens(
        "\"ab\"\r\n\"_bc_\"  \r\r \"\$cd\$\"\n\r\t\"de1\"",
        token(QUOTED_IDENTIFIER, "ab", 1, 1, 4),
        token(QUOTED_IDENTIFIER, "_bc_", 2, 1, 6),
        token(QUOTED_IDENTIFIER, "\$cd\$", 4, 2, 6),
        token(QUOTED_IDENTIFIER, "de1", 6, 2, 5)
    )

    @Test
    fun inlineCommentAtEnd() = assertTokens(
        "ab\n--Ignore Me",
        token(IDENTIFIER, "ab", 1, 1, 2)
    )

    @Test
    fun inlineCommentAtEndNoContent() = assertTokens(
        "ab--",
        token(IDENTIFIER, "ab", 1, 1, 2)
    )

    @Test
    fun blockCommentAtStart() = assertTokens(
        "/*Ignore Me*/ab",
        token(IDENTIFIER, "ab", 1, 14, 2)
    )

    @Test
    fun blockCommentAtEnd() = assertTokens(
        "ab\n/*Ignore Me*/",
        token(IDENTIFIER, "ab", 1, 1, 2)
    )

    @Test
    fun booleans() = assertTokens(
        "true false truefalse",
        token(LITERAL, "true", 1, 1, 4),
        token(LITERAL, "false", 1, 6, 5),
        token(IDENTIFIER, "truefalse", 1, 12, 9)
    )

    @Test
    fun nullAndMissing() = assertTokens(
        "null Null MISSING `null`",
        token(NULL, "null", 1, 1, 4),
        token(NULL, "null", 1, 6, 4),
        token(MISSING, "null", 1, 11, 7),
        token(ION_LITERAL, "null", 1, 19, 6)
    )

    @Test
    fun numbers() = assertTokens(
        "500 600. 0.1 . .1 0000 0.00e0 1e+1",
        token(LITERAL, "500", 1, 1, 3),
        token(LITERAL, "600d0", 1, 5, 4),
        token(LITERAL, "1d-1", 1, 10, 3),
        token(DOT, "'.'", 1, 14, 1),
        token(LITERAL, "1d-1", 1, 16, 2),
        token(LITERAL, "0", 1, 19, 4),
        token(LITERAL, "0d-2", 1, 24, 6),
        token(LITERAL, "1d1", 1, 31, 4)
    )

    @Test
    fun signedNumbers() = assertTokens(
        "+500 -600. -0.1 . +.1 -0000 +0.00e0 -+-1e+1",
        token(OPERATOR, "'+'", 1, 1, 1),
        token(LITERAL, "500", 1, 2, 3),
        token(OPERATOR, "'-'", 1, 6, 1),
        token(LITERAL, "600d0", 1, 7, 4),
        token(OPERATOR, "'-'", 1, 12, 1),
        token(LITERAL, "1d-1", 1, 13, 3),
        token(DOT, "'.'", 1, 17, 1),
        token(OPERATOR, "'+'", 1, 19, 1),
        token(LITERAL, "1d-1", 1, 20, 2),
        token(OPERATOR, "'-'", 1, 23, 1),
        token(LITERAL, "0", 1, 24, 4),
        token(OPERATOR, "'+'", 1, 29, 1),
        token(LITERAL, "0d-2", 1, 30, 6),
        token(OPERATOR, "'-'", 1, 37, 1),
        token(OPERATOR, "'+'", 1, 38, 1),
        token(OPERATOR, "'-'", 1, 39, 1),
        token(LITERAL, "1d1", 1, 40, 4)
    )

    @Test
    fun quotedIon() = assertTokens(
        "`1e0` `{a:5}` `/*`*/\"`\"` `//`\n'''`'''` `{{ +AB//A== }}` `{{ \"not a comment //\" }}`",
        token(ION_LITERAL, "1e0", 1, 1, 5),
        token(ION_LITERAL, "{a:5}", 1, 7, 7),
        token(ION_LITERAL, "\"`\"", 1, 15, 10),
        token(ION_LITERAL, "\"`\"", 1, 26, 13),
        token(ION_LITERAL, "{{ +AB//A== }}", 2, 10, 16),
        token(ION_LITERAL, "{{ \"not a comment //\" }}", 2, 27, 26)
    )


    @Test
    fun quotedStrings() = assertTokens(
        "'1e0' '{''a'':5}'",
        token(LITERAL, "\"1e0\"", 1, 1, 5),
        token(LITERAL, "\"{'a':5}\"", 1, 7, 11)
    )

    @Test
    fun quotedIdentifiers() = assertTokens(
        "\"1e0\" \"{\"\"a\"\":5}\"",
        token(QUOTED_IDENTIFIER, "'1e0'", 1, 1, 5),
        token(QUOTED_IDENTIFIER, "'{\"a\":5}'", 1, 7, 11)
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
            expected.add(token(type, "'$op'", 1, buf.length + 1L, op.length.toLong()))
            buf.append(op)
            // make sure we have a token between things to avoid hitting multi-lexeme
            // tokens by mistake
            expected.add(token(LITERAL, "1", 1, buf.length + 2L, 1))
            buf.append(" 1 ")
        }
        assertTokens(buf.toString(), *expected.toTypedArray())
    }

    @Test
    fun multiLexemeOperators() = assertTokens(
        "UNION ALL IS NOT union_all is_not",
        token(OPERATOR, "union_all", 1, 1, 5),
        token(OPERATOR, "is_not", 1, 11, 2),
        token(IDENTIFIER, "union_all", 1, 18, 9),
        token(IDENTIFIER, "is_not", 1, 28, 6)
    )

    @Test
    fun multiLexemeKeywords() = assertTokens(
        "CHARACTER VARYING DoUblE PrEcision double_precision character_varying",
        token(KEYWORD, "character_varying", 1, 1, 9),
        token(KEYWORD, "double_precision", 1, 19, 6),
        token(IDENTIFIER, "double_precision", 1, 36, 16),
        token(IDENTIFIER, "character_varying", 1, 53, 17)
    )

    @Test
    fun joinKeywords() = assertTokens(
        "cRoSs Join left join left left Inner joiN RIGHT JOIN RIGHT OUTER JOIN LEFT OUTER JOIN" +
        "\nFULL join OUTER JOIN FULL OUTER JOIN" +
        "\nCROSS CROSS JOIN JOIN",
        token(KEYWORD, "cross_join", 1, 1, 5),
        token(KEYWORD, "left_join", 1, 12, 4),
        token(KEYWORD, "left", 1, 22, 4),
        token(KEYWORD, "left", 1, 27, 4),
        token(KEYWORD, "inner_join", 1, 32, 5),
        token(KEYWORD, "right_join", 1, 43, 5),
        token(KEYWORD, "right_join", 1, 54, 5),
        token(KEYWORD, "left_join", 1, 71, 4),
        token(KEYWORD, "outer_join", 2, 1, 4),
        token(KEYWORD, "outer_join", 2, 11, 5),
        token(KEYWORD, "outer_join", 2, 22, 4),
        token(KEYWORD, "cross", 3, 1, 5),
        token(KEYWORD, "cross_join", 3, 7, 5),
        token(KEYWORD, "join", 3, 18, 4)
    )

    @Test
    fun functionKeywordNames() = assertTokens(
            "SUBSTRING",
            token(KEYWORD, "substring", 1, 1, 9)
    )

    @Test
    fun boolType() = assertTokens(
        "BOOL",
        token(KEYWORD, "boolean", 1, 1, 4)
    )
    @Test
    fun smallintType() = assertTokens(
        "SMALLINT",
        token(KEYWORD, "smallint", 1, 1, 8)
    )

    @Test
    fun intType() = assertTokens(
        "INT",
        token(KEYWORD, "integer", 1, 1, 3)
    )

    @Test
    fun integerType() = assertTokens(
        "INTEGER",
        token(KEYWORD, "integer", 1, 1, 7)
    )

    @Test
    fun floatType() = assertTokens(
        "FLOAT",
        token(KEYWORD, "float", 1, 1, 5)
    )

    @Test
    fun realType() = assertTokens(
        "REAL",
        token(KEYWORD, "real", 1, 1, 4)
    )

    @Test
    fun doublePrecisionType() = assertTokens(
        "DOUBLE PRECISION",
        token(KEYWORD, "double_precision", 1, 1, 6)
    )

    @Test
    fun decimalType() = assertTokens(
        "DECIMAL",
        token(KEYWORD, "decimal", 1, 1, 7)
    )

    @Test
    fun numericType() = assertTokens(
        "NUMERIC",
        token(KEYWORD, "numeric", 1, 1, 7)
    )

    @Test
    fun timestampType() = assertTokens(
        "TIMESTAMP",
        token(KEYWORD, "timestamp", 1, 1, 9)
    )

    @Test
    fun characterType() = assertTokens(
        "CHARACTER",
        token(KEYWORD, "character", 1, 1, 9)
    )

    @Test
    fun charType() = assertTokens(
        "CHAR",
        token(KEYWORD, "character", 1, 1, 4)
    )

    @Test
    fun varcharType() = assertTokens(
        "VARCHAR",
        token(KEYWORD, "character_varying", 1, 1, 7)
    )

    @Test
    fun characterVaryingType() = assertTokens(
        "CHARACTER VARYING",
        token(KEYWORD, "character_varying", 1, 1, 9)
    )

    @Test
    fun stringType() = assertTokens(
        "STRING",
        token(KEYWORD, "string", 1, 1, 6)
    )

    @Test
    fun symbolType() = assertTokens(
        "SYMBOL",
        token(KEYWORD, "symbol", 1, 1, 6)
    )

    @Test
    fun clobType() = assertTokens(
        "CLOB",
        token(KEYWORD, "clob", 1, 1, 4)
    )

    @Test
    fun blobType() = assertTokens(
        "BLOB",
        token(KEYWORD, "blob", 1, 1, 4)
    )

    @Test
    fun structType() = assertTokens(
        "STRUCT",
        token(KEYWORD, "struct", 1, 1, 6)
    )

    @Test
    fun listType() = assertTokens(
        "LIST",
        token(KEYWORD, "list", 1, 1, 4)
    )

    @Test
    fun sexpType() = assertTokens(
        "SEXP",
        token(KEYWORD, "sexp", 1, 1, 4)
    )

    @Test(expected = LexerException::class)
    fun invalidNumber() {
        tokenize("1E++0")
    }

    @Test(expected = LexerException::class)
    fun numberWithExponentTooLarge() {
        tokenize("1E2147483648") // exponent is represented by an int, this is bigger than 2^31-1 so doesn't fit
    }
}

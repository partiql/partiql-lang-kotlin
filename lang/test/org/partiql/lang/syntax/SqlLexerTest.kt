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

import org.junit.Test
import org.partiql.lang.TestBase

class SqlLexerTest : TestBase() {
    val lexer = SqlLexer(ion)

    // TODO: remove default value on [length] and specify the correct value for all invocations of this function
    fun token(type: TokenType, lit: String?, sourceText: String, line: Long, column: Long, length: Long): Token {
        val value = when (lit) {
            null -> null
            else -> ion.singleValue(lit)
        }
        return Token(type, value, sourceText, SourceSpan(line, column, length))
    }

    fun token(type: TokenType, lit: String, line: Long, column: Long, length: Long): Token {
        return token(type, lit, lit, line, column, length)
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
        token(TokenType.LEFT_PAREN, "'('", "(", 1, 1, 1),
        token(TokenType.RIGHT_PAREN, "')'", ")", 1, 2, 1),
        token(TokenType.LEFT_BRACKET, "'['", "[", 1, 3, 1),
        token(TokenType.RIGHT_BRACKET, "']'", "]", 1, 4, 1),
        token(TokenType.LEFT_CURLY, "'{'", "{", 1, 5, 1),
        token(TokenType.RIGHT_CURLY, "'}'", "}", 1, 6, 1),
        token(TokenType.COLON, "':'", ":", 1, 7, 1),
        token(TokenType.COMMA, "','", ",", 1, 8, 1),
        token(TokenType.DOT, "'.'", ".", 1, 9, 1),
        token(TokenType.STAR, "'*'", "*", 1, 10, 1),
        token(TokenType.LEFT_DOUBLE_ANGLE_BRACKET, "'<<'", "<<", 1, 11, 2),
        token(TokenType.RIGHT_DOUBLE_ANGLE_BRACKET, "'>>'", ">>", 1, 13, 2),
        token(TokenType.SEMICOLON, "';'", ";", 1, 15, 1)
    )

    @Test
    fun keywordsThatHaveTheirOwnTokenTypes() = assertTokens(
        "AS AT FOR",
        token(TokenType.AS, "as", "AS", 1, 1, 2),
        token(TokenType.AT, "at", "AT", 1, 4, 2),
        token(TokenType.FOR, "for", "FOR", 1, 7, 3)
    )

    @Test
    fun whitespaceAndIdentifiers() = assertTokens(
        "ab\r\n_bc_  \r\r \$cd\$\n\r\tde1",
        token(TokenType.IDENTIFIER, "ab", 1, 1, 2),
        token(TokenType.IDENTIFIER, "_bc_", 2, 1, 4),
        token(TokenType.IDENTIFIER, "\$cd\$", 4, 2, 4),
        token(TokenType.IDENTIFIER, "de1", 6, 2, 3)
    )

    @Test
    fun whitespaceAndQuotedIdentifiers() = assertTokens(
        "\"ab\"\r\n\"_bc_\"  \r\r \"\$cd\$\"\n\r\t\"de1\"",
        token(TokenType.QUOTED_IDENTIFIER, "ab", 1, 1, 4),
        token(TokenType.QUOTED_IDENTIFIER, "_bc_", 2, 1, 6),
        token(TokenType.QUOTED_IDENTIFIER, "\$cd\$", 4, 2, 6),
        token(TokenType.QUOTED_IDENTIFIER, "de1", 6, 2, 5)
    )

    @Test
    fun inlineCommentAtEnd() = assertTokens(
        "ab\n--Ignore Me",
        token(TokenType.IDENTIFIER, "ab", 1, 1, 2)
    )

    @Test
    fun inlineCommentAtEndNoContent() = assertTokens(
        "ab--",
        token(TokenType.IDENTIFIER, "ab", 1, 1, 2)
    )

    @Test
    fun blockCommentAtStart() = assertTokens(
        "/*Ignore Me*/ab",
        token(TokenType.IDENTIFIER, "ab", 1, 14, 2)
    )

    @Test
    fun blockCommentAtEnd() = assertTokens(
        "ab\n/*Ignore Me*/",
        token(TokenType.IDENTIFIER, "ab", 1, 1, 2)
    )

    @Test
    fun booleans() = assertTokens(
        "true false truefalse",
        token(TokenType.LITERAL, "true", 1, 1, 4),
        token(TokenType.LITERAL, "false", 1, 6, 5),
        token(TokenType.IDENTIFIER, "truefalse", 1, 12, 9)
    )

    @Test
    fun nullAndMissing() = assertTokens(
        "null Null MISSING `null`",
        token(TokenType.NULL, "null", "null", 1, 1, 4),
        token(TokenType.NULL, "null", "Null", 1, 6, 4),
        token(TokenType.MISSING, "null", "MISSING", 1, 11, 7),
        token(TokenType.ION_LITERAL, "null", "null", 1, 19, 6)
    )

    @Test
    fun numbers() = assertTokens(
        "500 600. 0.1 . .1 0000 0.00e0 1e+1",
        token(TokenType.LITERAL, "500", 1, 1, 3),
        token(TokenType.LITERAL, "600d0", "600.", 1, 5, 4),
        token(TokenType.LITERAL, "1d-1", "0.1", 1, 10, 3),
        token(TokenType.DOT, "'.'", ".", 1, 14, 1),
        token(TokenType.LITERAL, "1d-1", ".1", 1, 16, 2),
        token(TokenType.LITERAL, "0", "0000", 1, 19, 4),
        token(TokenType.LITERAL, "0d-2", "0.00e0", 1, 24, 6),
        token(TokenType.LITERAL, "1d1", "1e+1", 1, 31, 4)
    )

    @Test
    fun signedNumbers() = assertTokens(
        "+500 -600. -0.1 . +.1 -0000 +0.00e0 -+-1e+1",
        token(TokenType.OPERATOR, "'+'", "+", 1, 1, 1),
        token(TokenType.LITERAL, "500", 1, 2, 3),
        token(TokenType.OPERATOR, "'-'", "-", 1, 6, 1),
        token(TokenType.LITERAL, "600d0", "600.", 1, 7, 4),
        token(TokenType.OPERATOR, "'-'", "-", 1, 12, 1),
        token(TokenType.LITERAL, "1d-1", "0.1", 1, 13, 3),
        token(TokenType.DOT, "'.'", ".", 1, 17, 1),
        token(TokenType.OPERATOR, "'+'", "+", 1, 19, 1),
        token(TokenType.LITERAL, "1d-1", ".1", 1, 20, 2),
        token(TokenType.OPERATOR, "'-'", "-", 1, 23, 1),
        token(TokenType.LITERAL, "0", "0000", 1, 24, 4),
        token(TokenType.OPERATOR, "'+'", "+", 1, 29, 1),
        token(TokenType.LITERAL, "0d-2", "0.00e0", 1, 30, 6),
        token(TokenType.OPERATOR, "'-'", "-", 1, 37, 1),
        token(TokenType.OPERATOR, "'+'", "+", 1, 38, 1),
        token(TokenType.OPERATOR, "'-'", "-", 1, 39, 1),
        token(TokenType.LITERAL, "1d1", "1e+1", 1, 40, 4)
    )

    @Test
    fun quotedIon() = assertTokens(
        "`1e0` `{a:5}` `/*`*/\"`\"` `//`\n'''`'''` `{{ +AB//A== }}` `{{ \"not a comment //\" }}`",
        token(TokenType.ION_LITERAL, "1e0", 1, 1, 5),
        token(TokenType.ION_LITERAL, "{a:5}", 1, 7, 7),
        token(TokenType.ION_LITERAL, "\"`\"", "/*`*/\"`\"", 1, 15, 10),
        token(TokenType.ION_LITERAL, "\"`\"", "//`\n'''`'''", 1, 26, 13),
        token(TokenType.ION_LITERAL, "{{ +AB//A== }}", 2, 10, 16),
        token(TokenType.ION_LITERAL, "{{ \"not a comment //\" }}", 2, 27, 26)
    )

    @Test
    fun quotedStrings() = assertTokens(
        "'1e0' '{''a'':5}'",
        token(TokenType.LITERAL, "\"1e0\"", "1e0", 1, 1, 5),
        token(TokenType.LITERAL, "\"{'a':5}\"", "{'a':5}", 1, 7, 11)
    )

    @Test
    fun quotedIdentifiers() = assertTokens(
        "\"1e0\" \"{\"\"a\"\":5}\"",
        token(TokenType.QUOTED_IDENTIFIER, "'1e0'", "1e0", 1, 1, 5),
        token(TokenType.QUOTED_IDENTIFIER, "'{\"a\":5}'", "{\"a\":5}", 1, 7, 11)
    )

    @Test
    fun operators() {
        val buf = StringBuilder()
        val expected = ArrayList<Token>()
        for (op in ALL_SINGLE_LEXEME_OPERATORS) {
            val type = when (op) {
                "*" -> TokenType.STAR
                in ALL_OPERATORS -> TokenType.OPERATOR
                in KEYWORDS -> TokenType.KEYWORD
                else -> TokenType.OPERATOR
            }
            expected.add(token(type, "'$op'", op, 1, buf.length + 1L, op.length.toLong()))
            buf.append(op)
            // make sure we have a token between things to avoid hitting multi-lexeme
            // tokens by mistake
            expected.add(token(TokenType.LITERAL, "1", 1, buf.length + 2L, 1))
            buf.append(" 1 ")
        }
        assertTokens(buf.toString(), *expected.toTypedArray())
    }

    @Test
    fun multiLexemeOperators() {
        assertTokens(
            "UNION ALL IS NOT union_all is_not",
            token(TokenType.OPERATOR, "union_all", "UNION ALL", 1, 1, 5),
            token(TokenType.OPERATOR, "is_not", "IS NOT", 1, 11, 2),
            token(TokenType.IDENTIFIER, "union_all", "union_all", 1, 18, 9),
            token(TokenType.IDENTIFIER, "is_not", "is_not", 1, 28, 6)
        )
        assertTokens(
            "OUTER UNION ALL IS NOT outer_union_all is_not",
            token(TokenType.OPERATOR, "outer_union_all", "OUTER UNION ALL", 1, 1, 5),
            token(TokenType.OPERATOR, "is_not", "IS NOT", 1, 17, 2),
            token(TokenType.IDENTIFIER, "outer_union_all", "outer_union_all", 1, 24, 15),
            token(TokenType.IDENTIFIER, "is_not", "is_not", 1, 40, 6)
        )
    }

    @Test
    fun multiLexemeKeywords() = assertTokens(
        "CHARACTER VARYING DoUblE PrEcision double_precision character_varying",
        token(TokenType.KEYWORD, "character_varying", "CHARACTER VARYING", 1, 1, 9),
        token(TokenType.KEYWORD, "double_precision", "DoUblE PrEcision", 1, 19, 6),
        token(TokenType.IDENTIFIER, "double_precision", "double_precision", 1, 36, 16),
        token(TokenType.IDENTIFIER, "character_varying", "character_varying", 1, 53, 17)
    )

    @Test
    fun joinKeywords() = assertTokens(
        "cRoSs Join left join left left Inner joiN RIGHT JOIN RIGHT OUTER JOIN LEFT OUTER JOIN" +
            "\nFULL join OUTER JOIN FULL OUTER JOIN" +
            "\nCROSS CROSS JOIN JOIN",
        token(TokenType.KEYWORD, "cross_join", "cRoSs Join", 1, 1, 5),
        token(TokenType.KEYWORD, "left_join", "left join", 1, 12, 4),
        token(TokenType.KEYWORD, "left", "left", 1, 22, 4),
        token(TokenType.KEYWORD, "left", "left", 1, 27, 4),
        token(TokenType.KEYWORD, "inner_join", "Inner joiN", 1, 32, 5),
        token(TokenType.KEYWORD, "right_join", "RIGHT JOIN", 1, 43, 5),
        token(TokenType.KEYWORD, "right_join", "RIGHT OUTER JOIN", 1, 54, 5),
        token(TokenType.KEYWORD, "left_join", "LEFT OUTER JOIN", 1, 71, 4),
        token(TokenType.KEYWORD, "outer_join", "FULL join", 2, 1, 4),
        token(TokenType.KEYWORD, "outer_join", "OUTER JOIN", 2, 11, 5),
        token(TokenType.KEYWORD, "outer_join", "FULL OUTER JOIN", 2, 22, 4),
        token(TokenType.KEYWORD, "cross", "CROSS", 3, 1, 5),
        token(TokenType.KEYWORD, "cross_join", "CROSS JOIN", 3, 7, 5),
        token(TokenType.KEYWORD, "join", "JOIN", 3, 18, 4)
    )

    @Test
    fun functionKeywordNames() = assertTokens(
        "SUBSTRING",
        token(TokenType.KEYWORD, "substring", "SUBSTRING", 1, 1, 9)
    )

    @Test
    fun boolType() = assertTokens(
        "BOOL",
        token(TokenType.KEYWORD, "boolean", "BOOL", 1, 1, 4)
    )

    @Test
    fun smallintType() = assertTokens(
        "SMALLINT",
        token(TokenType.KEYWORD, "smallint", "SMALLINT", 1, 1, 8)
    )

    @Test
    fun integer4Type() = assertTokens(
        "INTEGER4",
        token(TokenType.KEYWORD, "integer4", "INTEGER4", 1, 1, 8)
    )

    @Test
    fun int4Type() = assertTokens(
        "INT4",
        token(TokenType.KEYWORD, "integer4", "INT4", 1, 1, 4)
    )

    @Test
    fun intType() = assertTokens(
        "INT",
        token(TokenType.KEYWORD, "integer", "INT", 1, 1, 3)
    )

    @Test
    fun integerType() = assertTokens(
        "INTEGER",
        token(TokenType.KEYWORD, "integer", "INTEGER", 1, 1, 7)
    )

    @Test
    fun floatType() = assertTokens(
        "FLOAT",
        token(TokenType.KEYWORD, "float", "FLOAT", 1, 1, 5)
    )

    @Test
    fun realType() = assertTokens(
        "REAL",
        token(TokenType.KEYWORD, "real", "REAL", 1, 1, 4)
    )

    @Test
    fun doublePrecisionType() = assertTokens(
        "DOUBLE PRECISION",
        token(TokenType.KEYWORD, "double_precision", "DOUBLE PRECISION", 1, 1, 6)
    )

    @Test
    fun decimalType() = assertTokens(
        "DECIMAL",
        token(TokenType.KEYWORD, "decimal", "DECIMAL", 1, 1, 7)
    )

    @Test
    fun numericType() = assertTokens(
        "NUMERIC",
        token(TokenType.KEYWORD, "numeric", "NUMERIC", 1, 1, 7)
    )

    @Test
    fun timestampType() = assertTokens(
        "TIMESTAMP",
        token(TokenType.KEYWORD, "timestamp", "TIMESTAMP", 1, 1, 9)
    )

    @Test
    fun characterType() = assertTokens(
        "CHARACTER",
        token(TokenType.KEYWORD, "character", "CHARACTER", 1, 1, 9)
    )

    @Test
    fun charType() = assertTokens(
        "CHAR",
        token(TokenType.KEYWORD, "character", "CHAR", 1, 1, 4)
    )

    @Test
    fun varcharType() = assertTokens(
        "VARCHAR",
        token(TokenType.KEYWORD, "character_varying", "VARCHAR", 1, 1, 7)
    )

    @Test
    fun characterVaryingType() = assertTokens(
        "CHARACTER VARYING",
        token(TokenType.KEYWORD, "character_varying", "CHARACTER VARYING", 1, 1, 9)
    )

    @Test
    fun stringType() = assertTokens(
        "STRING",
        token(TokenType.KEYWORD, "string", "STRING", 1, 1, 6)
    )

    @Test
    fun symbolType() = assertTokens(
        "SYMBOL",
        token(TokenType.KEYWORD, "symbol", "SYMBOL", 1, 1, 6)
    )

    @Test
    fun clobType() = assertTokens(
        "CLOB",
        token(TokenType.KEYWORD, "clob", "CLOB", 1, 1, 4)
    )

    @Test
    fun blobType() = assertTokens(
        "BLOB",
        token(TokenType.KEYWORD, "blob", "BLOB", 1, 1, 4)
    )

    @Test
    fun structType() = assertTokens(
        "STRUCT",
        token(TokenType.KEYWORD, "struct", "STRUCT", 1, 1, 6)
    )

    @Test
    fun listType() = assertTokens(
        "LIST",
        token(TokenType.KEYWORD, "list", "LIST", 1, 1, 4)
    )

    @Test
    fun sexpType() = assertTokens(
        "SEXP",
        token(TokenType.KEYWORD, "sexp", "SEXP", 1, 1, 4)
    )

    @Test
    fun esBooleanType() = assertTokens(
        "ES_boolean",
        token(TokenType.IDENTIFIER, "ES_boolean", 1, 1, 10)
    )

    @Test
    fun esIntegerType() = assertTokens(
        "ES_integer",
        token(TokenType.IDENTIFIER, "ES_integer", 1, 1, 10)
    )

    @Test
    fun esFloatType() = assertTokens(
        "ES_float",
        token(TokenType.IDENTIFIER, "ES_float", 1, 1, 8)
    )

    @Test
    fun esTextType() = assertTokens(
        "ES_text",
        token(TokenType.IDENTIFIER, "ES_text", 1, 1, 7)
    )

    @Test(expected = LexerException::class)
    fun invalidNumber() {
        tokenize("1E++0")
    }

    @Test(expected = LexerException::class)
    fun numberWithExponentTooLarge() {
        tokenize("1E2147483648") // exponent is represented by an int, this is bigger than 2^31-1 so doesn't fit
    }

    @Test
    fun rsVarcharMax() = assertTokens(
        "RS_varchar_max",
        token(TokenType.IDENTIFIER, "RS_varchar_max", 1, 1, 14)
    )

    @Test
    fun rsReal() = assertTokens(
        "RS_real",
        token(TokenType.IDENTIFIER, "RS_real", 1, 1, 7)
    )

    @Test
    fun rsFloat4() = assertTokens(
        "RS_float4",
        token(TokenType.IDENTIFIER, "RS_float4", 1, 1, 9)
    )

    @Test
    fun rsDoublePrecision() = assertTokens(
        "RS_double_precision",
        token(TokenType.IDENTIFIER, "RS_double_precision", 1, 1, 19)
    )

    @Test
    fun rsFloat() = assertTokens(
        "RS_float",
        token(TokenType.IDENTIFIER, "RS_float", 1, 1, 8)
    )

    @Test
    fun rsFloat8() = assertTokens(
        "RS_float8",
        token(TokenType.IDENTIFIER, "RS_float8", 1, 1, 9)
    )

    @Test
    fun sparkFloat() = assertTokens(
        "SPARK_float",
        token(TokenType.IDENTIFIER, "SPARK_float", 1, 1, 11)
    )

    @Test
    fun sparkShort() = assertTokens(
        "SPARK_short",
        token(TokenType.IDENTIFIER, "SPARK_short", 1, 1, 11)
    )

    @Test
    fun sparkInteger() = assertTokens(
        "SPARK_integer",
        token(TokenType.IDENTIFIER, "SPARK_integer", 1, 1, 13)
    )

    @Test
    fun sparkLong() = assertTokens(
        "SPARK_long",
        token(TokenType.IDENTIFIER, "SPARK_long", 1, 1, 10)
    )

    @Test
    fun sparkDouble() = assertTokens(
        "SPARK_double",
        token(TokenType.IDENTIFIER, "SPARK_double", 1, 1, 12)
    )

    @Test
    fun sparkBoolean() = assertTokens(
        "SPARK_boolean",
        token(TokenType.IDENTIFIER, "SPARK_boolean", 1, 1, 13)
    )

    @Test
    fun rsInteger() = assertTokens(
        "RS_integer",
        token(TokenType.IDENTIFIER, "RS_integer", 1, 1, 10)
    )

    @Test
    fun rsBigint() = assertTokens(
        "RS_bigint",
        token(TokenType.IDENTIFIER, "RS_bigint", 1, 1, 9)
    )

    @Test
    fun rsBoolean() = assertTokens(
        "RS_boolean",
        token(TokenType.IDENTIFIER, "RS_boolean", 1, 1, 10)
    )
}

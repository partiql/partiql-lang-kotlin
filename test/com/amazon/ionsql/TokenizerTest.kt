/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import org.junit.Test
import com.amazon.ionsql.Token.Type
import com.amazon.ionsql.Token.Type.*

class TokenizerTest : Base() {
    val tokenizer = Tokenizer(ion)

    fun tokenize(text: String): List<Token> = tokenizer.tokenize(literal(text)!!)

    infix fun Type.of(text: String?) = when (text) {
        null -> Token(this, null)
        else -> Token(this, literal(text))
    }

    fun assertTokens(text: String, expectedTokens: List<Token>) {
        val actual = tokenize(text)
        assertEquals(expectedTokens, actual)
    }

    @Test
    fun empty() = assertTokens("()", emptyList())

    @Test
    fun select() = assertTokens(
        "(SELECT * FROM $ WHERE $.a > 5)",
        listOf(
            KEYWORD of "select",
            STAR of "'*'",
            KEYWORD of "from",
            IDENTIFIER of "$",
            KEYWORD of "where",
            IDENTIFIER of "$",
            DOT of "'.'",
            IDENTIFIER of "a",
            OPERATOR of "'>'",
            LITERAL of "5"
        )
    )

    @Test
    fun function() = assertTokens(
        "(extract_first(SELECT a, b FROM $))",
        listOf(
            IDENTIFIER of "extract_first",
            LEFT_PAREN of null,
            KEYWORD of "select",
            IDENTIFIER of "a",
            COMMA of "','",
            IDENTIFIER of "b",
            KEYWORD of "from",
            IDENTIFIER of "$",
            RIGHT_PAREN of null
        )
    )

    @Test
    fun multidot() = assertTokens(
        "(a..**..>=)",
        listOf(
            IDENTIFIER of "a",
            DOT of "'.'",
            DOT of "'.'",
            STAR of "'*'",
            STAR of "'*'",
            DOT of "'.'",
            DOT of "'.'",
            OPERATOR of "'>='"
        )
    )

    @Test(expected = IllegalArgumentException::class)
    fun illegalSymbol() { tokenize("(a>>)") }
}
/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import org.junit.Test
import com.amazon.ionsql.Token.Type
import com.amazon.ionsql.Token.Type.*

class TokenizerTest : Base() {
    fun tokenize(text: String): List<Token> = Tokenizer.tokenize(literal(text)!!)

    infix fun Type.of(text: String?) = when (text) {
        null -> Token(this, null)
        else -> Token(this, literal(text))
    }

    @Test
    fun empty() {
        val actual = tokenize("()")
        val expected = emptyList<Token>()

        assertEquals(expected, actual)
    }

    @Test
    fun select() {
        val actual = tokenize("(SELECT * FROM $ WHERE $.a > 5)")
        val expected = listOf(
            KEYWORD of "select",
            OPERATOR of "'*'",
            KEYWORD of "from",
            IDENTIFIER of "$",
            KEYWORD of "where",
            IDENTIFIER of "$",
            DOT of "'.'",
            IDENTIFIER of "a",
            OPERATOR of "'>'",
            LITERAL of "5"
        )

        assertEquals(expected, actual)
    }

    @Test
    fun function() {
        val actual = tokenize("(EXTRACT_FIRST(SELECT a, b FROM $))")
        val expected = listOf(
            IDENTIFIER of "extract_first",
            LEFT_PAREN of null,
            KEYWORD of "select",
            IDENTIFIER of "a",
            SEPARATOR of "','",
            IDENTIFIER of "b",
            KEYWORD of "from",
            IDENTIFIER of "$",
            RIGHT_PAREN of null
        )

        assertEquals(expected, actual)
    }
}
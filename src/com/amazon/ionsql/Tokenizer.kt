/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonList
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonType.*
import com.amazon.ion.IonValue
import com.amazon.ionsql.Token.Type
import com.amazon.ionsql.Token.Type.*
import java.util.*

/**
 * Provides generates a list of tokens from an expression.
 */
object Tokenizer {
    fun tokenize(source: IonValue): List<Token> {
        // make sure we have an expression
        val expr = when(source.type) {
            SEXP -> source
            else -> source.system.newSexp(source.clone())
        }

        val tokens = ArrayList<Token>()
        tokens.tokenize(expr)
        return tokens
    }

    private fun MutableList<Token>.tokenizeContainer(left: Type, right: Type, value: IonValue) {
        add(Token(left))
        this.tokenize(value)
        add(Token(right))
    }

    private fun MutableList<Token>.tokenize(source: IonValue) {
        for (value in source) {
            when (value) {
                is IonList -> tokenizeContainer(LEFT_BRACKET, RIGHT_BRACKET, value)
                is IonSexp -> tokenizeContainer(LEFT_PAREN, RIGHT_PAREN, value)
                is IonSymbol -> add(value.toToken())
                else -> add(Token(LITERAL, value))
            }
        }
    }

    private val KEYWORDS = setOf(
        "select",
        "from",
        "where"
    )

    private val OPERATORS = setOf(
        "+", "-", "*", "/", "%", "**",
        "<", "<=", ">", ">=", "==", "!=",
        "!"
    )

    private fun IonSymbol.toToken(): Token {
        // names are not case sensitive
        val text = stringValue().toLowerCase()

        val type = when (text) {
            in KEYWORDS -> KEYWORD
            in OPERATORS -> OPERATOR
            "," -> SEPARATOR
            "." -> DOT
            else -> IDENTIFIER
        }

        val tokenValue = system.newSymbol(text)

        return Token(type, tokenValue)
    }
}
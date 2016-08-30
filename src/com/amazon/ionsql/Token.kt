/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSymbol
import com.amazon.ion.IonValue
import com.amazon.ionsql.Token.Type.*

/**
 * Simple [IonValue] based token for lexing Ion SQL.
 */
data class Token(val type: Type, val value: IonValue? = null) {
    companion object {
        val KEYWORDS = setOf(
            "select",
            "from",
            "where",
            "as"
        )

        val BINARY_OPERATORS = setOf(
            "+", "-", "*", "/", "%",
            "<", "<=", ">", ">=", "==", "!=",
            "and", "or"
        )

        val UNARY_OPERATORS = setOf(
            "+", "-", "not"
        )

        val ALL_OPERATORS = BINARY_OPERATORS union UNARY_OPERATORS

        /**
         * Constructs a [Token] from an [IonSymbol].
         *
         * In particular, Ion symbolic values represent things like keywords, identifiers,
         */
        fun fromSymbol(symbol: IonSymbol): Token {
            // names are not case sensitive
            val text = symbol.stringValue().toLowerCase()

            val type = when (text) {
                in KEYWORDS -> KEYWORD
                in ALL_OPERATORS -> OPERATOR
                "," -> COMMA
                "." -> DOT
                else -> IDENTIFIER
            }

            val tokenValue = symbol.system.newSymbol(text)

            return Token(type, tokenValue)
        }
    }

    enum class Type {
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        IDENTIFIER,
        OPERATOR,
        KEYWORD,
        LITERAL,
        AS,
        DOT,
        COMMA
    }

    val text: String?
        get() = value?.stringValue()

    val keywordText: String?
        get() = when(type) {
            KEYWORD -> text
            else -> null
        }

    val isBinaryOperator: Boolean
        get() = type == OPERATOR && text in BINARY_OPERATORS

    val isUnaryOperator: Boolean
        get() = type == OPERATOR && text in UNARY_OPERATORS
}
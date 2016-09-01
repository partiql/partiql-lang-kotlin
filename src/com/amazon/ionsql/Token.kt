/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue
import com.amazon.ionsql.Token.Type.*

/**
 * Simple [IonValue] based token for lexing Ion SQL.
 */
data class Token(val type: Type, val value: IonValue? = null) {
    companion object {
        internal val KEYWORDS = setOf(
            "select",
            "from",
            "where",
            "as"
        )

        // note that '*' is treated specially
        internal val BINARY_OPERATORS = setOf(
            "+", "-", "/", "%",
            "<", "<=", ">", ">=", "==", "!=",
            "and", "or"
        )

        internal val UNARY_OPERATORS = setOf(
            "+", "-", "not"
        )

        internal val ALL_OPERATORS = BINARY_OPERATORS union UNARY_OPERATORS
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
        STAR,
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
        get() = when(type) {
            OPERATOR -> text in BINARY_OPERATORS
            STAR -> true
            else -> false
        }

    val isUnaryOperator: Boolean
        get() = when(type){
            OPERATOR -> text in UNARY_OPERATORS
            STAR -> true
            else -> false
        }
}
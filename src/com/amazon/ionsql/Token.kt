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
            "limit",
            "group",
            "order",
            "limit",
            "values",
            "pivot",
            "unpivot",
            "case",
            "when",
            "else",
            "end",
            "by",
            "as",
            "at"
        )

        // note that '*' is treated specially
        internal val BINARY_OPERATORS = setOf(
            "+", "-", "/", "%",
            "<", "<=", ">", ">=", "=", "!=",
            "and", "or"
        )

        internal val UNARY_OPERATORS = setOf(
            "+", "-", "not"
        )

        internal val ALL_OPERATORS = BINARY_OPERATORS union UNARY_OPERATORS

        internal val OPERATOR_PRECEDENCE = mapOf<String, Int>(
            "or"  to 1,
            "and" to 2,

            // equality group (TODO add other morphemes of equality/non-equality)
            "="  to 3,
            "!="  to 3,

            // comparison group
            "<"   to 4,
            "<="  to 4,
            ">"   to 4,
            ">="  to 4,

            // the addition group
            "+"   to 5,
            "-"   to 5,

            // multiply group (TODO add exponentiation)
            "*"   to 6,
            "/"   to 6,
            "%"   to 6
        )
    }

    enum class Type {
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        LEFT_CURLY,
        RIGHT_CURLY,
        IDENTIFIER,
        OPERATOR,
        KEYWORD,
        LITERAL,
        AS,
        DOT,
        STAR,
        COMMA,
        COLON
    }

    val text: String?
        get() = value?.stringValue()

    val keywordText: String?
        get() = when (type) {
            KEYWORD -> text
            else -> null
        }

    val isBinaryOperator: Boolean
        get() = when (type) {
            OPERATOR -> text in BINARY_OPERATORS
            STAR -> true
            else -> false
        }

    val isUnaryOperator: Boolean
        get() = when (type){
            OPERATOR -> text in UNARY_OPERATORS
            else -> false
        }

    val infixPrecedence: Int
        get() = when (isBinaryOperator) {
            true -> OPERATOR_PRECEDENCE.get(text) ?: 0
            else -> 0
        }
}

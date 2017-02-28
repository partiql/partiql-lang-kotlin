/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ion.IonValue
import com.amazon.ionsql.util.*
import com.amazon.ionsql.syntax.TokenType.*

/**
 * Simple [IonValue] based token for lexing Ion SQL.
 */
data class Token(val type: TokenType,
                 val value: IonValue? = null,
                 val position: SourcePosition? = null) {
    val text: String?
        get() = value?.stringValue()

    val keywordText: String?
        get() = when (type) {
            OPERATOR, KEYWORD, AS, AT -> text
            MISSING -> "missing"
            NULL -> "null"
            else -> null
        }

    val isSpecialOperator: Boolean
        get() = when (type) {
            OPERATOR -> text in SPECIAL_OPERATORS
            else -> false
        }

    val isBinaryOperator: Boolean
        get() = when (type) {
            OPERATOR, KEYWORD -> text in BINARY_OPERATORS
            STAR -> true
            else -> false
        }

    val isUnaryOperator: Boolean
        get() = when (type){
            OPERATOR, KEYWORD -> text in UNARY_OPERATORS
            else -> false
        }

    val infixPrecedence: Int
        get() = when {
            isBinaryOperator || isSpecialOperator -> INFIX_OPERATOR_PRECEDENCE[text] ?: 0
            else -> 0
        }
}

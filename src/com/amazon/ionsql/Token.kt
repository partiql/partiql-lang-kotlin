/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue
import com.amazon.ionsql.TokenType.*

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
            OPERATOR, KEYWORD -> text
            else -> null
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
        get() = when (isBinaryOperator) {
            true -> OPERATOR_PRECEDENCE.get(text) ?: 0
            else -> 0
        }
}

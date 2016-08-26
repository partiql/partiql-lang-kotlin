/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue

/**
 * Simple [IonValue] based token for lexing Ion SQL.
 */
data class Token(val type: Type, val value: IonValue? = null) {
    enum class Type {
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        IDENTIFIER,
        OPERATOR,
        KEYWORD,
        LITERAL,
        DOT,
        SEPARATOR
    }
}
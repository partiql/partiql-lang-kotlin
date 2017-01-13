/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** Supported categories of tokens. */
enum class TokenType {
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
    MISSING,
    COLON
}

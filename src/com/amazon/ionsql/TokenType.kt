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
    LEFT_DOUBLE_ANGLE_BRACKET,
    RIGHT_DOUBLE_ANGLE_BRACKET,
    IDENTIFIER,
    OPERATOR,
    KEYWORD,
    LITERAL,
    // punctuation
    DOT,
    STAR,
    COMMA,
    COLON,
    // keywords that get their own token type
    AS,
    MISSING
}

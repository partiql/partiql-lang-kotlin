/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

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
    QUOTED_IDENTIFIER,
    OPERATOR,
    KEYWORD,
    LITERAL,
    // punctuation
    DOT,
    STAR,
    COMMA,
    COLON,
    SEMICOLON,
    // keywords that get their own token type
    AS,
    FOR,
    AT,
    MISSING,
    NULL,
    // function specific
    TRIM_SPECIFICATION,
    DATE_PART,
    EOF // End of Stream token.
    ;
    fun isIdentifier() = this == TokenType.IDENTIFIER || this == TokenType.QUOTED_IDENTIFIER
}


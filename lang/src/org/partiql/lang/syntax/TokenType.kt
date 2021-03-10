/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.syntax

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
    ION_LITERAL,
    // punctuation
    DOT,
    STAR,
    COMMA,
    COLON,
    SEMICOLON,
    QUESTION_MARK,
    // keywords that get their own token type
    AS,
    FOR,
    AT,
    BY,
    MISSING,
    NULL,
    ASC,
    DESC,
    // function specific
    TRIM_SPECIFICATION,
    DATE_PART,
    EOF // End of Stream token.
    ;
    fun isIdentifier() = this == TokenType.IDENTIFIER || this == TokenType.QUOTED_IDENTIFIER
}


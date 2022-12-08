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
@Deprecated(
    message = "This class is subject to removal.",
    level = DeprecationLevel.WARNING
) // To be removed before 1.0
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
    NULLS,
    FIRST,
    LAST,
    // function specific
    TRIM_SPECIFICATION,
    DATETIME_PART,
    EOF // End of Stream token.
    ;
    fun isIdentifier() = this == IDENTIFIER || this == QUOTED_IDENTIFIER
}

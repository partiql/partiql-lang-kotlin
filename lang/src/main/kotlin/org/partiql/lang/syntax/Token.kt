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

@file:Suppress("DEPRECATION")

package org.partiql.lang.syntax

import com.amazon.ion.IonValue
import org.partiql.lang.util.stringValue

/**
 * Simple [IonValue] based token for lexing PartiQL.
 *
 * @property type the [TokenType] of this token
 * @property value an optional [IonValue] representing this token (may be transformed)
 * @property sourceText the literal source [String] of the token, if available
 * @property span the [SourceSpan] in the source where this token appears
 */
@Deprecated(
    message = "This class is subject to removal.",
    level = DeprecationLevel.WARNING
) // To be removed before 1.0
data class Token(
    val type: TokenType,
    val value: IonValue? = null,
    val sourceText: String,
    val span: SourceSpan
) {
    val text: String?
        get() = value?.stringValue()

    val keywordText: String?
        get() = when (type) {
            TokenType.OPERATOR, TokenType.KEYWORD, TokenType.AS, TokenType.AT -> text
            TokenType.MISSING -> "missing"
            TokenType.NULL -> "null"
            else -> null
        }

    val isSpecialOperator: Boolean
        get() = when (type) {
            TokenType.OPERATOR -> text in SPECIAL_OPERATORS
            else -> false
        }

    val isBinaryOperator: Boolean
        get() = when (type) {
            TokenType.OPERATOR, TokenType.KEYWORD -> text in BINARY_OPERATORS
            TokenType.STAR -> true
            else -> false
        }

    val isUnaryOperator: Boolean
        get() = when (type) {
            TokenType.OPERATOR, TokenType.KEYWORD -> text in UNARY_OPERATORS
            else -> false
        }

    val prefixPrecedence: Int
        get() = when {
            isUnaryOperator -> OPERATOR_PRECEDENCE[text] ?: 0
            else -> 0
        }

    val infixPrecedence: Int
        get() = when {
            isBinaryOperator || isSpecialOperator -> OPERATOR_PRECEDENCE[text] ?: 0
            else -> 0
        }

    val isDataType: Boolean
        get() = when {
            type == TokenType.KEYWORD && CORE_TYPE_NAME_ARITY_MAP.keys.union(TYPE_ALIASES.keys)
                .contains(text?.toLowerCase()) -> true
            else -> false
        }
}

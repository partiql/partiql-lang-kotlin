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

import com.amazon.ion.IonValue
import org.partiql.lang.util.*
import org.partiql.lang.syntax.TokenType.*

/**
 * Simple [IonValue] based token for lexing PartiQL.
 */
data class Token(val type: TokenType,
                 val value: IonValue? = null,
                 val span: SourceSpan) {
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
}

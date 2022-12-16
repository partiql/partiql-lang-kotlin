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

package org.partiql.lang.util

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.SourceSpan
import org.partiql.lang.syntax.Token

/**
 * Given an error context ([PropertyValueMap]) and a source position ([SourcePosition]) populate the given
 * error context with line and column information found in source position.
 */
private fun populateLineAndColumn(errorContext: PropertyValueMap, sourceSpan: SourceSpan?): PropertyValueMap {
    when (sourceSpan) {
        null -> {
            return errorContext
        }
        else -> {
            val (line, col) = sourceSpan
            errorContext[Property.LINE_NUMBER] = line
            errorContext[Property.COLUMN_NUMBER] = col
            return errorContext
        }
    }
}

internal fun Token?.err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap = PropertyValueMap()): Nothing {
    when (this) {
        null -> throw ParserException(errorCode = errorCode, errorContext = errorContext)
        else -> {
            val pvmap = populateLineAndColumn(errorContext, this.span)
            pvmap[Property.TOKEN_DESCRIPTION] = type.toString()
            value?.let { pvmap[Property.TOKEN_VALUE] = it }
            throw ParserException(message, errorCode, pvmap)
        }
    }
}

internal fun List<Token>.err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap = PropertyValueMap()): Nothing =
    head.err(message, errorCode, errorContext)

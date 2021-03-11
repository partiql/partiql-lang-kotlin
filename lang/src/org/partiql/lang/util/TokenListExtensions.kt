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

package org.partiql.lang.util

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.syntax.*

/**
 * Predicate to check if the list of [Token]'s only contains a end of statement with
 * a [TokenType.EOF] or a [TokenType.SEMICOLON] followed by a [TokenType.EOF]
 *
 * @receiver List<Token> the current list of tokens being processed by the parser
 * @return true if the list has size 1 and the only element in the list is EOF or if the list has size 2 and contains
 *  [TokenType.SEMICOLON] and [TokenType.EOF] in order
 */
internal fun List<Token>.onlyEndOfStatement() =
    (size == 1 && this[0].type == TokenType.EOF) ||
    (size == 2 && this[0].type == TokenType.SEMICOLON && this[1].type == TokenType.EOF)

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
            pvmap[Property.TOKEN_TYPE] = type
            value?.let { pvmap[Property.TOKEN_VALUE] = it }
            throw ParserException(message, errorCode, pvmap)
        }
    }
}

internal fun Token?.errExpectedTokenType(expectedType: TokenType): Nothing {
    val pvmap = PropertyValueMap()
    pvmap[Property.EXPECTED_TOKEN_TYPE] = expectedType
    err("Expected $expectedType", ErrorCode.PARSE_EXPECTED_TOKEN_TYPE, pvmap)
}

internal fun List<Token>.atomFromHead(parseType: SqlParser.ParseType = SqlParser.ParseType.ATOM): SqlParser.ParseNode =
        SqlParser.ParseNode(parseType, head, emptyList(), tail)

internal fun List<Token>.err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap = PropertyValueMap()): Nothing =
        head.err(message, errorCode, errorContext)

internal fun List<Token>.tailExpectedKeyword(keyword: String): List<Token> {
    when (head?.keywordText) {
        keyword -> return tail
        else -> {
            val pvmap = PropertyValueMap()
            pvmap[Property.KEYWORD] = keyword.toUpperCase()
            err("Expected ${keyword.toUpperCase()} keyword", ErrorCode.PARSE_EXPECTED_KEYWORD, pvmap)
        }
    }
}

internal fun List<Token>.tailExpectedToken(tokenType: TokenType): List<Token> =
    when (head?.type) {
        tokenType -> tail
        else      -> head.errExpectedTokenType(tokenType)
    }



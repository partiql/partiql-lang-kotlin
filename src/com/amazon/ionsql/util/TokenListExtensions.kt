package com.amazon.ionsql.util

import com.amazon.ionsql.errors.ErrorCode
import com.amazon.ionsql.errors.Property
import com.amazon.ionsql.errors.PropertyValueMap
import com.amazon.ionsql.syntax.*

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
private fun populateLineAndColumn(errorContext: PropertyValueMap, sourcePosition: SourcePosition?): PropertyValueMap {
    when (sourcePosition) {
        null -> {
            return errorContext
        }
        else -> {
            val (line, col) = sourcePosition
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
            val pvmap = populateLineAndColumn(errorContext, this.position)
            pvmap[Property.TOKEN_TYPE] = type
            value?.let { pvmap[Property.TOKEN_VALUE] = it }
            throw ParserException(message, errorCode, pvmap)
        }
    }
}

internal fun List<Token>.atomFromHead(parseType: IonSqlParser.ParseType = IonSqlParser.ParseType.ATOM): IonSqlParser.ParseNode =
        IonSqlParser.ParseNode(parseType, head, emptyList(), tail)

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



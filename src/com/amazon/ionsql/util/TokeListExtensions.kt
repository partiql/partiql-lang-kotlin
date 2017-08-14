package com.amazon.ionsql.util

import com.amazon.ionsql.syntax.Token
import com.amazon.ionsql.syntax.TokenType

/**
 * Predicate to check if the list of [Token]'s only contains a [ParseNode] with [Token.Type] [Token.EOF]
 *
 * @receiver List<Token> the current list of tokens being processed by the parser
 * @return true if the list has size 1 and the only element in the list is EOF
 *
 */
internal fun List<Token>.onlyEof() =
    this.size == 1 && this[0].type == TokenType.EOF
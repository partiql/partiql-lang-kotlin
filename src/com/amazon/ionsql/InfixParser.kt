/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ionsql.InfixParser.ParseType.*
import com.amazon.ionsql.Token.Type
import com.amazon.ionsql.Token.Type.*
import java.util.*

/**
 * Parses a list of tokens as infix query expression into a prefix s-expression
 * as the abstract syntax tree.
 */
class InfixParser(val ion: IonSystem) {
    internal enum class ParseType {
        ATOM,
        FUNCTION_CALL,
        ARG_LIST,
        ALIAS
    }

    internal data class ParseNode(val type: ParseType,
                                  val token: Token?,
                                  val children: List<ParseNode>,
                                  val remaining: List<Token>) {
        /** Derives a [ParseNode] transforming the list of remaining tokens. */
        fun derive(tokensHandler: (List<Token>) -> List<Token>): ParseNode =
            copy(remaining = tokensHandler(remaining))

        fun deriveExpected(vararg types: Type): ParseNode = derive {
            var rem = it
            for (type in types) {
                if (type != rem.head?.type) {
                    throw IllegalArgumentException("Expected ${type}, got ${rem.head}: ${it}")
                }
                rem = rem.tail
            }
            rem
        }
    }

    private fun List<Token>.atomFromHead(): ParseNode =
        ParseNode(ATOM, head, emptyList(), tail)

    fun parse(tokens: List<Token>): IonSexp {
        throw UnsupportedOperationException("FIXME!")
    }

    internal fun parseExpression(tokens: List<Token>): ParseNode {
        throw UnsupportedOperationException("FIXME!")
    }

    internal fun parseTerm(tokens: List<Token>): ParseNode = when (tokens.head?.type) {
        LEFT_PAREN -> parseExpression(tokens.tail).deriveExpected(RIGHT_PAREN)
        IDENTIFIER -> when (tokens.tail.head?.type) {
            LEFT_PAREN -> parseFunctionCall(tokens.head!!, tokens.tail.tail)
            else -> tokens.atomFromHead()
        }
        LITERAL -> tokens.atomFromHead()
        else -> throw IllegalArgumentException("Unexpected term: ${tokens}")
    }

    internal fun parseFunctionCall(name: Token, tokens: List<Token>): ParseNode =
        parseArgList(
            tokens,
            supportsAlias = false
        ).copy(
            type = FUNCTION_CALL,
            token = name
        ).deriveExpected(RIGHT_PAREN)

    internal fun parseArgList(tokens: List<Token>,
                              supportsAlias: Boolean): ParseNode {
        val argList = ArrayList<ParseNode>()
        var rem = tokens
        while (rem.isNotEmpty()) {
            var child = parseExpression(tokens)
            if (supportsAlias && rem.head?.keywordText == "as") {
                val name = rem.tail.head
                if (name == null || name.type != IDENTIFIER) {
                    throw IllegalArgumentException("Expected identifier for alias: ${rem}")
                }
                rem = rem.tail.tail
                child = ParseNode(ALIAS, name, listOf(child), rem)
            }

            argList.add(child)
            rem = child.remaining

            if (rem.head?.type != COMMA) {
                break
            }
        }

        return ParseNode(ARG_LIST, null, argList, rem)
    }
}
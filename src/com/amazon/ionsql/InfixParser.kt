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
    companion object {
        private val SELECT_BOUNDARY_TOKEN_TYPES =
            setOf(KEYWORD)
        private val GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_PAREN)
        private val ARGLIST_BOUNDARY_TOKEN_TYPES =
            setOf(COMMA)
        private val ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES =
            ARGLIST_BOUNDARY_TOKEN_TYPES union setOf(AS)
    }

    internal enum class ParseType {
        ATOM,
        SELECT,
        CALL,
        ARG_LIST,
        ALIAS,
        PATH,
        UNARY,
        BINARY
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
                    throw IllegalArgumentException("Expected $type, got ${rem.head}: $it")
                }
                rem = rem.tail
            }
            rem
        }
    }

    inline private fun sexp(builder: IonSexp.() -> Unit): IonSexp =
        ion.newEmptySexp().apply(builder)

    inline private fun IonSexp.addSexp(builder: IonSexp.() -> Unit) =
        add().newEmptySexp().apply(builder)

    private fun IonSexp.addChildNodes(parent: ParseNode) {
        for (node in parent.children) {
            add(node.toSexp())
        }
    }

    internal fun ParseNode.toSexp(): IonSexp = when (type) {
        ATOM -> when (token?.type) {
            LITERAL -> sexp {
                addSymbol("lit")
                addClone(token?.value!!)
            }
            IDENTIFIER -> sexp {
                addSymbol("id")
                addSymbol(token?.text!!)
            }
            DOT -> sexp {
                // an atom that is a dot is a parent reference
                addSymbol("..")
            }
            STAR -> sexp {
                addSymbol("*")
            }
            else -> throw IllegalStateException("Unsupported atom: $this")
        }
        UNARY -> sexp {
            addSymbol(token?.text!!)
            addChildNodes(this@toSexp)
        }
        PATH -> sexp {
            addSymbol(".")
            addChildNodes(this@toSexp)
        }
        SELECT -> sexp {
            addSymbol("select")
            addSexp {
                addChildNodes(children[0])
            }
            addSexp {
                addSymbol("from")
                addChildNodes(children[1])
            }
            if (children.size > 2) {
                addSexp {
                    addSymbol("where")
                    add(children[2].toSexp())
                }
            }
        }
        CALL -> sexp {
            addSymbol("call")
            addSymbol(token?.text!!)
            addChildNodes(this@toSexp)
        }
        ALIAS -> sexp {
            addSymbol("as")
            addSymbol(token?.text!!)
            addChildNodes(this@toSexp)
        }
        else -> throw IllegalStateException("Unsupported type for ion value: $this")
    }

    private fun List<Token>.atomFromHead(): ParseNode =
        ParseNode(ATOM, head, emptyList(), tail)

    /** Entry point into the parser. */
    fun parse(tokens: List<Token>): IonSexp = parseExpression(tokens).toSexp()

    internal fun parseExpression(tokens: List<Token>,
                                 boundaryTokenTypes: Set<Type> = emptySet()): ParseNode {
        var rem = tokens
        while (rem.isNotEmpty()) {
            val term = parseUnaryOperatorTerm(rem)
            rem = term.remaining

            // FIXME support operators via Shunting-Yard infix translation
            if (rem.isNotEmpty() && rem.head?.type !in boundaryTokenTypes) {
                throw UnsupportedOperationException("FIXME! $rem")
            }
            return term
        }
        throw IllegalArgumentException("Empty expression not allowed")
    }

    private fun parseUnaryOperatorTerm(tokens: List<Token>): ParseNode =
        when (tokens.head?.isUnaryOperator) {
            true -> {
                val term = parseUnaryOperatorTerm(tokens.tail)

                ParseNode(
                    UNARY,
                    tokens.head,
                    listOf(term),
                    term.remaining
                )
            }
            else -> parseDottedTerm(tokens)
        }

    private fun parseDottedTerm(tokens: List<Token>): ParseNode {
        val term = parseTerm(tokens)
        val path = ArrayList<ParseNode>(listOf(term))
        var rem = term.remaining
        while (rem.head?.type == DOT) {
            // consume first dot
            rem = rem.tail

            // consume all dots succeeding the initial one as a parent ref
            while (rem.head?.type == DOT) {
                path.add(rem.atomFromHead())
                rem = rem.tail
            }

            when (rem.head?.type) {
                IDENTIFIER, STAR -> {
                    path.add(rem.atomFromHead())
                }
                else -> throw IllegalArgumentException("Path must have identifier: $tokens")
            }
            rem = rem.tail
        }

        return when (path.size) {
            1 -> term
            else -> ParseNode(PATH,  null, path, rem)
        }
    }

    private fun parseTerm(tokens: List<Token>): ParseNode = when (tokens.head?.type) {
        KEYWORD -> when (tokens.head?.keywordText) {
            "select" -> parseSelect(tokens.tail)
            else -> throw IllegalArgumentException("Unexpected keyword: $tokens")
        }
        LEFT_PAREN -> parseExpression(
            tokens.tail,
            boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
        ).deriveExpected(
            RIGHT_PAREN
        )
        IDENTIFIER -> when (tokens.tail.head?.type) {
            LEFT_PAREN -> parseFunctionCall(tokens.head!!, tokens.tail.tail)
            else -> tokens.atomFromHead()
        }
        LITERAL -> tokens.atomFromHead()
        else -> throw IllegalArgumentException("Unexpected term: $tokens")
    }

    private fun parseSelect(tokens: List<Token>): ParseNode {
        val children = ArrayList<ParseNode>()

        val selectList = parseArgList(
            tokens, supportsAlias = true, boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES)
        var rem = selectList.remaining
        children.add(selectList)

        if (rem.head?.keywordText != "from") {
            throw IllegalArgumentException("Expected FROM after select list $tokens")
        }

        val fromList = parseArgList(
            rem.tail, supportsAlias = true, boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES)
        rem = fromList.remaining
        children.add(fromList)

        if (rem.head?.keywordText == "where") {
            val whereExpr = parseExpression(rem.tail)
            rem = whereExpr.remaining
            children.add(whereExpr)
        }

        return ParseNode(SELECT, null, children, rem)
    }

    private fun parseFunctionCall(name: Token, tokens: List<Token>): ParseNode =
        parseArgList(
            tokens,
            supportsAlias = false,
            boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = CALL,
            token = name
        ).deriveExpected(RIGHT_PAREN)

    private fun parseArgList(tokens: List<Token>,
                             supportsAlias: Boolean,
                             boundaryTokenTypes: Set<Type> = emptySet()): ParseNode {
        val argListBoundaryTokenTypes = when (supportsAlias) {
            true -> ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES
            false -> ARGLIST_BOUNDARY_TOKEN_TYPES
        } union boundaryTokenTypes
        val argList = ArrayList<ParseNode>()
        var rem = tokens
        while (rem.isNotEmpty()
                && rem.head?.type !in boundaryTokenTypes) {
            var child = parseExpression(rem, argListBoundaryTokenTypes)
            rem = child.remaining
            if (supportsAlias && rem.head?.keywordText == "as") {
                val name = rem.tail.head
                if (name == null || name.type != IDENTIFIER) {
                    throw IllegalArgumentException("Expected identifier for alias: $rem")
                }
                rem = rem.tail.tail
                child = ParseNode(ALIAS, name, listOf(child), rem)
                rem = child.remaining
            }

            argList.add(child)

            if (rem.head?.type != COMMA) {
                break
            }
            rem = rem.tail
        }

        return ParseNode(ARG_LIST, null, argList, rem)
    }
}
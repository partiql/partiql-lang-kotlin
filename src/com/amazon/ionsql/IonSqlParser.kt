/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionsql.IonSqlParser.ParseType.*
import com.amazon.ionsql.TokenType.*
import java.util.*

/**
 * Parses a list of tokens as infix query expression into a prefix s-expression
 * as the abstract syntax tree.
 */
class IonSqlParser(private val ion: IonSystem) : Parser {
    companion object {
        private val SELECT_BOUNDARY_TOKEN_TYPES =
            setOf(KEYWORD, RIGHT_PAREN)

        private val GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_PAREN)

        private val BRACKET_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_BRACKET)

        private val STRUCT_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_CURLY)

        private val ARGLIST_BOUNDARY_TOKEN_TYPES =
            setOf(COMMA)

        private val ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES =
            ARGLIST_BOUNDARY_TOKEN_TYPES union setOf(AS)

        private val FIELD_NAME_BOUNDARY_TOKEN_TYPES =
            setOf(COLON)
    }

    internal enum class ParseType {
        ATOM,
        SELECT_LIST,
        SELECT_VALUES,
        CALL,
        ARG_LIST,
        ALIAS,
        PATH,
        UNARY,
        BINARY,
        LIST,
        STRUCT,
        MEMBER
    }

    internal data class ParseNode(val type: ParseType,
                                  val token: Token?,
                                  val children: List<ParseNode>,
                                  val remaining: List<Token>) {
        /** Derives a [ParseNode] transforming the list of remaining tokens. */
        fun derive(tokensHandler: (List<Token>) -> List<Token>): ParseNode =
            copy(remaining = tokensHandler(remaining))

        fun deriveExpected(vararg types: TokenType): ParseNode = derive {
            var rem = it
            for (type in types) {
                if (type != rem.head?.type) {
                    throw IllegalArgumentException("Expected $type, got ${rem.head}: $it")
                }
                rem = rem.tail
            }
            rem
        }

        fun deriveChildren(transform: (List<ParseNode>) -> List<ParseNode>) =
            copy(children = transform(children))
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

    private fun IonSequence.addSymbol(text: String) = add().newSymbol(text)

    private fun IonSequence.addClone(value: IonValue) = add(value.clone())

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
            STAR -> sexp {
                addSymbol("*")
            }
            else -> throw IllegalStateException("Unsupported atom: $this")
        }
        LIST -> sexp {
            addSymbol("list")
            addChildNodes(this@toSexp)
        }
        STRUCT -> sexp {
            addSymbol("struct")
            addChildNodes(this@toSexp)
        }
        MEMBER -> sexp {
            // we translate this to construct a list to normalize with the struct function
            addSymbol("list")
            addChildNodes(this@toSexp)
        }
        UNARY, BINARY -> sexp {
            addSymbol(token?.text!!)
            addChildNodes(this@toSexp)
        }
        PATH -> sexp {
            addSymbol("path")
            addChildNodes(this@toSexp)
        }
        SELECT_LIST, SELECT_VALUES -> sexp {
            addSymbol("select")
            addSexp {
                addSymbol(when (this@toSexp.type) {
                    SELECT_LIST -> when {
                        children[0].children.isEmpty() -> "*"
                        else -> "list"
                    }
                    SELECT_VALUES -> "values"
                    else -> throw IllegalStateException("Unsupported SELECT type: $this")
                })
                when (this@toSexp.type) {
                    SELECT_VALUES -> add(children[0].toSexp())
                    else -> addChildNodes(children[0])
                }

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
    override fun parse(tokens: List<Token>): IonSexp = parseExpression(tokens).toSexp()

    /**
     * Parses the given token list.
     *
     * @param tokens The list of tokens to parse.
     * @param precedence The precedence of the current expression parsing.
     *                   A negative value represents the "top-level" parsing.
     * @param boundaryTokenTypes The token types that are considered the "end" of the parse.
     *                           Empty implies until end of stream.
     *
     * @return The parse tree for the given expression.
     */
    internal fun parseExpression(tokens: List<Token>,
                                 precedence: Int = -1,
                                 boundaryTokenTypes: Set<TokenType> = emptySet()): ParseNode {
        var expr = parseUnaryTerm(tokens)
        var rem = expr.remaining

        fun atBoundary() = rem.isEmpty() || rem.head?.type in boundaryTokenTypes
        fun headPrecedence() = rem.head?.infixPrecedence ?: 0

        // XXX this is a Pratt Top-Down Operator Precedence implementation
        while (!atBoundary() && precedence < headPrecedence()) {
            val op = rem.head!!
            if (!op.isBinaryOperator) {
                throw IllegalArgumentException("Expected binary operator: $rem")
            }
            val right = parseExpression(
                rem.tail,
                precedence = op.infixPrecedence,
                boundaryTokenTypes = boundaryTokenTypes
            )
            rem = right.remaining
            expr = ParseNode(BINARY, op, listOf(expr, right), rem)
        }
        return expr
    }

    private fun parseUnaryTerm(tokens: List<Token>): ParseNode =
        when (tokens.head?.isUnaryOperator) {
            true -> {
                val term = parseUnaryTerm(tokens.tail)

                ParseNode(
                    UNARY,
                    tokens.head,
                    listOf(term),
                    term.remaining
                )
            }
            else -> parsePathTerm(tokens)
        }

    private fun parsePathTerm(tokens: List<Token>): ParseNode {
        val term = parseTerm(tokens)
        val path = ArrayList<ParseNode>(listOf(term))
        var rem = term.remaining
        var hasPath = true
        while (hasPath) {
            when (rem.head?.type) {
                DOT -> {
                    // consume first dot
                    rem = rem.tail

                    // consume all dots succeeding the initial one as a parent ref
                    while (rem.head?.type == DOT) {
                        path.add(rem.atomFromHead())
                        rem = rem.tail
                    }

                    when (rem.head?.type) {
                        IDENTIFIER -> {
                            // re-write the identifier as a literal string element
                            val token = Token(LITERAL, ion.newString(rem.head?.text!!))
                            path.add(ParseNode(ATOM, token, emptyList(), rem.tail))
                        }
                        STAR -> path.add(rem.atomFromHead())
                        else -> throw IllegalArgumentException(
                            "Dotted member access invalid: $tokens"
                        )
                    }
                    rem = rem.tail
                }
                LEFT_BRACKET -> {
                    val expr = parseExpression(
                        rem.tail,
                        boundaryTokenTypes = BRACKET_BOUNDARY_TOKEN_TYPES
                    ).deriveExpected(
                        RIGHT_BRACKET
                    )
                    path.add(expr)
                    rem = expr.remaining
                }
                else -> hasPath = false
            }
        }

        return when (path.size) {
            1 -> term
            else -> ParseNode(PATH, null, path, rem)
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
        LEFT_BRACKET -> parseListLiteral(tokens.tail)
        LEFT_CURLY -> parseStructLiteral(tokens.tail)
        IDENTIFIER -> when (tokens.tail.head?.type) {
            LEFT_PAREN -> parseFunctionCall(tokens.head!!, tokens.tail.tail)
            else -> tokens.atomFromHead()
        }
        LITERAL -> tokens.atomFromHead()
        else -> throw IllegalArgumentException("Unexpected term: $tokens")
    }

    private fun parseSelect(tokens: List<Token>): ParseNode {
        val head = tokens.head
        var type = SELECT_LIST
        val projection = when {
            head?.type == STAR -> {
                // special form for * is empty arg-list
                ParseNode(ARG_LIST, null, emptyList(), tokens.tail)
            }
            head?.keywordText == "values" -> {
                type = SELECT_VALUES
                parseExpression(tokens.tail, boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES)
            }
            else -> {
                val list = parseArgList(
                    tokens,
                    supportsAlias = true,
                    supportsMemberName = false,
                    boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
                )
                if (list.children.isEmpty()) {
                    throw IllegalArgumentException("Cannot have empty select list: $tokens")
                }

                list
            }
        }

        return parseSelectAfterProjection(type, projection)
    }

    private fun parseSelectAfterProjection(selectType: ParseType, projection: ParseNode): ParseNode {
        val children = ArrayList<ParseNode>()
        var rem = projection.remaining
        children.add(projection)

        if (rem.head?.keywordText != "from") {
            throw IllegalArgumentException("Expected FROM after select list $rem")
        }

        val fromList = parseArgList(
            rem.tail,
            supportsAlias = true,
            supportsMemberName = false,
            boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
        ).deriveChildren {
            // FROM <path> has an implicit wildcard, we need to rewrite the parse tree to
            // handle this
            it.map {
                when (it.type) {
                    PATH -> it.deriveChildren {
                        injectWildCardForFromClause(it)
                    }
                    ALIAS -> it.deriveChildren {
                        it.map {
                            when (it.type) {
                                PATH -> it.deriveChildren {
                                    injectWildCardForFromClause(it)
                                }
                                else -> it
                            }
                        }
                    }
                    else -> it
                }
            }
        }

        rem = fromList.remaining
        children.add(fromList)

        if (rem.head?.keywordText == "where") {
            val whereExpr = parseExpression(
                rem.tail,
                boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
            )
            rem = whereExpr.remaining
            children.add(whereExpr)
        }

        var parseNode = ParseNode(selectType, null, children, rem)

        if (rem.head?.keywordText == "limit") {
            val limitExpr = parseExpression(
                rem.tail,
                boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
            )
            rem = limitExpr.remaining
            // TODO figure out if this should be first class syntax (it's a bit of a hack)
            parseNode = ParseNode(
                CALL,
                Token(IDENTIFIER, ion.newSymbol("__limit")),
                listOf(parseNode, limitExpr),
                rem
            )
        }

        return parseNode
    }

    private fun injectWildCardForFromClause(nodes: List<ParseNode>): List<ParseNode> =
        listOf(nodes.head!!, ParseNode(ATOM, Token(STAR), emptyList(), emptyList())) + nodes.tail

    private fun parseFunctionCall(name: Token, tokens: List<Token>): ParseNode =
        parseArgList(
            tokens,
            supportsAlias = false,
            supportsMemberName = false,
            boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = CALL,
            token = name
        ).deriveExpected(RIGHT_PAREN)

    private fun parseListLiteral(tokens: List<Token>): ParseNode =
        parseArgList(
            tokens,
            supportsAlias = false,
            supportsMemberName = false,
            boundaryTokenTypes = BRACKET_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = LIST
        ).deriveExpected(RIGHT_BRACKET)

    private fun parseStructLiteral(tokens: List<Token>): ParseNode =
        parseArgList(
            tokens,
            supportsAlias = false,
            supportsMemberName = true,
            boundaryTokenTypes = STRUCT_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = STRUCT
        ).deriveExpected(RIGHT_CURLY)

    private fun parseArgList(tokens: List<Token>,
                             supportsAlias: Boolean,
                             supportsMemberName: Boolean,
                             boundaryTokenTypes: Set<TokenType> = emptySet()): ParseNode {
        val argListBoundaryTokenTypes = when {
            supportsAlias -> ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES
            else -> ARGLIST_BOUNDARY_TOKEN_TYPES
        } union boundaryTokenTypes
        val argList = ArrayList<ParseNode>()
        var rem = tokens

        fun parseField() = parseExpression(
            rem, boundaryTokenTypes = FIELD_NAME_BOUNDARY_TOKEN_TYPES)
        fun parseChild() = parseExpression(
            rem, boundaryTokenTypes = argListBoundaryTokenTypes)

        while (rem.isNotEmpty()
                && rem.head?.type !in boundaryTokenTypes) {
            var child = when {
                supportsMemberName -> {
                    val field = parseField().deriveExpected(COLON)
                    rem = field.remaining
                    val value = parseChild()
                    ParseNode(MEMBER, null, listOf(field, value), value.remaining)
                }
                else -> parseChild()
            }
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

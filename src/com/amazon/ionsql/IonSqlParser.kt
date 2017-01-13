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
            ARGLIST_BOUNDARY_TOKEN_TYPES union setOf(AS, IDENTIFIER)

        private val FIELD_NAME_BOUNDARY_TOKEN_TYPES =
            setOf(COLON)

        private fun List<Token>.err(message: String): Nothing {
            val tokenMessage = when (head) {
                null -> "end of expression"
                else -> "${head!!.type} ${head!!.value ?: "<NONE>"} at ${head?.position ?: "unknown position"}"
            }

            throw IllegalArgumentException(
                "$message at $tokenMessage"
            )
        }
    }

    internal enum class ParseType {
        ATOM,
        SELECT_LIST,
        SELECT_VALUES,
        WHERE,
        LIMIT,
        CALL,
        ARG_LIST,
        ALIAS,
        PATH,
        UNARY,
        BINARY,
        TERNARY,
        LIST,
        STRUCT,
        MEMBER
    }

    internal data class ParseNode(val type: ParseType,
                                  val token: Token?,
                                  val children: List<ParseNode>,
                                  val remaining: List<Token>) {
        val name = type.name.toLowerCase()

        /** Derives a [ParseNode] transforming the list of remaining tokens. */
        fun derive(tokensHandler: (List<Token>) -> List<Token>): ParseNode =
            copy(remaining = tokensHandler(remaining))

        fun deriveExpected(vararg types: TokenType): ParseNode = derive {
            var rem = it
            for (type in types) {
                if (type != rem.head?.type) {
                    rem.err("Expected $type")
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

    internal fun ParseNode.toSexp(): IonSexp {
        val astNode = when (type) {
            ATOM -> when (token?.type) {
                LITERAL -> sexp {
                    addSymbol("lit")
                    addClone(token?.value!!)
                }
                MISSING -> sexp {
                    addSymbol("missing")
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
                // unfold the MEMBER nodes
                for (child in children) {
                    if (child.type != MEMBER) {
                        throw IllegalStateException("Expected MEMBER node: $child")
                    }
                    addChildNodes(child)
                }

            }
            UNARY, BINARY, TERNARY -> sexp {
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
                        else -> throw IllegalStateException("Unsupported SELECT type: ${this@toSexp}")
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
                    for (clause in children.slice(2..children.lastIndex)) {
                        when (clause.type) {
                            WHERE, LIMIT -> addSexp {
                                addSymbol(clause.name)
                                addChildNodes(clause)
                            }
                            else -> throw IllegalStateException("Unsupported SELECT clause: ${this@toSexp}")
                        }
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

        // wrap the meta node as appropriate
        val sourcePos = token?.position
        return when (sourcePos) {
            null -> astNode
            else -> sexp {
                addSymbol("meta")
                add(astNode)

                val details = add().newEmptyStruct()
                details.put("line").newInt(sourcePos.line)
                details.put("column").newInt(sourcePos.column)
            }
        }
    }

    private fun List<Token>.atomFromHead(): ParseNode =
        ParseNode(ATOM, head, emptyList(), tail)

    /**
     * Parses the given token list.
     *
     * @param precedence The precedence of the current expression parsing.
     *                   A negative value represents the "top-level" parsing.
     * @param boundaryTokenTypes The token types that are considered the "end" of the parse.
     *                           Empty implies until end of stream.
     *
     * @return The parse tree for the given expression.
     */
    internal fun List<Token>.parseExpression(precedence: Int = -1,
                                             boundaryTokenTypes: Set<TokenType> = emptySet()): ParseNode {
        var expr = parseUnaryTerm()
        var rem = expr.remaining

        fun atBoundary() = rem.isEmpty() || rem.head?.type in boundaryTokenTypes
        fun headPrecedence() = rem.head?.infixPrecedence ?: 0

        // XXX this is a Pratt Top-Down Operator Precedence implementation
        while (!atBoundary() && precedence < headPrecedence()) {
            val op = rem.head!!
            if (!op.isBinaryOperator && op.keywordText != "between") {
                rem.err("Expected binary operator or BETWEEN")
            }

            val right = rem.tail.parseExpression(
                precedence = op.infixPrecedence,
                boundaryTokenTypes = boundaryTokenTypes
            )
            rem = right.remaining

            expr = when {
                op.isBinaryOperator -> ParseNode(BINARY, op, listOf(expr, right), rem)
                else -> when (op.keywordText) {
                    "between" -> {
                        if (rem.head?.keywordText != "and") {
                            rem.err("Expected AND after BETWEEN")
                        }
                        val third = rem.tail.parseExpression(
                            precedence = op.infixPrecedence,
                            boundaryTokenTypes = boundaryTokenTypes
                        )
                        rem = third.remaining
                        ParseNode(TERNARY, op, listOf(expr, right, third), rem)
                    }
                    else -> rem.err("Unknown infix operator")
                }
            }
        }
        return expr
    }

    private fun List<Token>.parseUnaryTerm(): ParseNode =
        when (head?.isUnaryOperator) {
            true -> {
                val term = tail.parseUnaryTerm()

                ParseNode(
                    UNARY,
                    head,
                    listOf(term),
                    term.remaining
                )
            }
            else -> parsePathTerm()
        }

    private fun List<Token>.parsePathTerm(): ParseNode {
        val term = parseTerm()
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
                        else -> err("Invalid path dot component")
                    }
                    rem = rem.tail
                }
                LEFT_BRACKET -> {
                    val expr = rem.tail.parseExpression(
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

    private fun List<Token>.parseTerm(): ParseNode = when (head?.type) {
        KEYWORD -> when (head?.keywordText) {
            "select" -> tail.parseSelect()
            in FUNCTION_NAME_KEYWORDS -> when (tail.head?.type) {
                LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
                else -> err("Unexpected keyword")
            }
            else -> err("Unexpected keyword")
        }
        LEFT_PAREN -> tail.parseExpression(
            boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
        ).deriveExpected(
            RIGHT_PAREN
        )
        LEFT_BRACKET -> tail.parseListLiteral()
        LEFT_CURLY -> tail.parseStructLiteral()
        IDENTIFIER -> when (tail.head?.type) {
            LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
            else -> atomFromHead()
        }
        LITERAL, MISSING -> atomFromHead()
        else -> err("Unexpected term")
    }

    private fun List<Token>.parseSelect(): ParseNode {
        var type = SELECT_LIST
        val projection = when {
            head?.type == STAR -> {
                // special form for * is empty arg-list
                ParseNode(ARG_LIST, null, emptyList(), tail)
            }
            head?.keywordText == "values" -> {
                type = SELECT_VALUES
                tail.parseExpression(boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES)
            }
            else -> {
                val list = parseArgList(
                    supportsAlias = true,
                    supportsMemberName = false,
                    boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
                )
                if (list.children.isEmpty()) {
                    err("Cannot have empty SELECT list")
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
            rem.err("Expected FROM after SELECT list")
        }

        val fromList = rem.tail.parseArgList(
            supportsAlias = true,
            supportsMemberName = false,
            boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
        ).deriveChildren {
            // FROM <path> has an implicit wildcard, we need to rewrite the parse tree to
            // handle this
            it.map {
                when (it.type) {
                    PATH -> it.deriveChildren {
                        it.injectWildCardForFromClause()
                    }
                    ALIAS -> it.deriveChildren {
                        it.map {
                            when (it.type) {
                                PATH -> it.deriveChildren {
                                    it.injectWildCardForFromClause()
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
            val whereExpr = rem.tail.parseExpression(
                boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
            )
            rem = whereExpr.remaining
            children.add(
                ParseNode(
                    WHERE,
                    null,
                    listOf(whereExpr),
                    rem
                )
            )
        }

        if (rem.head?.keywordText == "limit") {
            val limitExpr = rem.tail.parseExpression(
                boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
            )
            rem = limitExpr.remaining
            children.add(
                ParseNode(
                    LIMIT,
                    null,
                    listOf(limitExpr),
                    rem
                )
            )
        }

        return ParseNode(selectType, null, children, rem)
    }

    private fun List<ParseNode>.injectWildCardForFromClause(): List<ParseNode> =
        listOf(head!!, ParseNode(ATOM, Token(STAR), emptyList(), emptyList())) + tail

    private fun List<Token>.parseFunctionCall(name: Token): ParseNode =
        parseArgList(
            supportsAlias = false,
            supportsMemberName = false,
            boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = CALL,
            token = name
        ).deriveExpected(RIGHT_PAREN)

    private fun List<Token>.parseListLiteral(): ParseNode =
        parseArgList(
            supportsAlias = false,
            supportsMemberName = false,
            boundaryTokenTypes = BRACKET_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = LIST
        ).deriveExpected(RIGHT_BRACKET)

    private fun List<Token>.parseStructLiteral(): ParseNode =
        parseArgList(
            supportsAlias = false,
            supportsMemberName = true,
            boundaryTokenTypes = STRUCT_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = STRUCT
        ).deriveExpected(RIGHT_CURLY)

    private fun List<Token>.parseArgList(supportsAlias: Boolean,
                                         supportsMemberName: Boolean,
                                         boundaryTokenTypes: Set<TokenType> = emptySet()): ParseNode {
        val argListBoundaryTokenTypes = when {
            supportsAlias -> ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES
            else -> ARGLIST_BOUNDARY_TOKEN_TYPES
        } union boundaryTokenTypes
        val argList = ArrayList<ParseNode>()
        var rem = this

        fun parseField() = rem.parseExpression(
            boundaryTokenTypes = FIELD_NAME_BOUNDARY_TOKEN_TYPES)
        fun parseChild() = rem.parseExpression(
            boundaryTokenTypes = argListBoundaryTokenTypes)

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
            val aliasType = rem.head?.type
            if (supportsAlias && (aliasType == AS || aliasType == IDENTIFIER)) {
                if (aliasType == AS) {
                    rem = rem.tail
                }
                val name = rem.head
                if (name == null || name.type != IDENTIFIER) {
                    rem.err("Expected identifier for alias")
                }
                rem = rem.tail
                child = ParseNode(ALIAS, name, listOf(child), rem)
            }

            argList.add(child)

            if (rem.head?.type != COMMA) {
                break
            }
            rem = rem.tail
        }

        return ParseNode(ARG_LIST, null, argList, rem)
    }

    /** Entry point into the parser. */
    override fun parse(tokens: List<Token>): IonSexp =
        tokens.parseExpression().toSexp().apply { makeReadOnly() }
}

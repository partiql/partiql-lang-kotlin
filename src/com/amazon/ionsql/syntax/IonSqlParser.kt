/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionsql.syntax.IonSqlParser.ParseType.*
import com.amazon.ionsql.syntax.IonSqlParser.AliasSupportType.*
import com.amazon.ionsql.syntax.TokenType.*
import com.amazon.ionsql.util.*
import java.util.*

/**
 * Parses a list of tokens as infix query expression into a prefix s-expression
 * as the abstract syntax tree.
 */
class IonSqlParser(private val ion: IonSystem) : Parser {
    companion object {
        private val SELECT_BOUNDARY_TOKEN_TYPES =
            setOf(KEYWORD, RIGHT_PAREN)

        private val KEYWORD_BOUNDARY_TOKEN_TYPES =
            setOf(KEYWORD)

        private val CAST_BOUNDARY_TOKEN_TYPES =
            setOf(AS)

        private val GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_PAREN)

        private val BRACKET_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_BRACKET)

        private val BAG_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_DOUBLE_ANGLE_BRACKET)

        private val STRUCT_BOUNDARY_TOKEN_TYPES =
            setOf(RIGHT_CURLY)

        private val ARGLIST_BOUNDARY_TOKEN_TYPES =
            setOf(COMMA)

        private val ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES =
            IonSqlParser.Companion.ARGLIST_BOUNDARY_TOKEN_TYPES union setOf(AS, IDENTIFIER)

        private val ARGLIST_WITH_AT_BOUNDARY_TOKEN_TYPES =
            ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES union setOf(AT)

        private val FIELD_NAME_BOUNDARY_TOKEN_TYPES =
            setOf(COLON)

        private fun Token?.err(message: String,
                               ctor: (String) -> Throwable = ::IllegalArgumentException): Nothing {
            val tokenMessage = when (this) {
                null -> "end of expression"
                else -> "[${type} ${value ?: "<NONE>"}] at ${position ?: "<UNKNOWN>"}"
            }

            throw ctor(
                "$message at $tokenMessage"
            )
        }

        private fun List<Token>.err(message: String,
                                    ctor: (String) -> Throwable = ::IllegalArgumentException): Nothing =
            head.err(message, ctor)

        private fun List<Token>.atomFromHead(): ParseNode = ParseNode(ATOM, head, emptyList(), tail)

        private fun List<Token>.tailExpectedKeyword(keyword: String): List<Token> =
            when (head?.keywordText) {
                keyword -> tail
                else -> err("Expected ${keyword.toUpperCase()} keyword")
            }
    }

    private val lexer = IonSqlLexer(ion)

    internal enum class AliasSupportType(val supportsAs: Boolean, val supportsAt: Boolean) {
        NONE(supportsAs = false, supportsAt = false),
        AS_ONLY(supportsAs = true, supportsAt = false),
        AS_AND_AT(supportsAs = true, supportsAt = true)
    }

    internal enum class ParseType {
        ATOM,
        SELECT_LIST,
        SELECT_VALUE,
        DISTINCT,
        WHERE,
        GROUP,
        GROUP_PARTIAL,
        LIMIT,
        CALL,
        ARG_LIST,
        AS_ALIAS,
        AT_ALIAS,
        PATH,
        UNARY,
        BINARY,
        TERNARY,
        LIST,
        STRUCT,
        MEMBER,
        CAST,
        TYPE,
        CASE,
        WHEN,
        ELSE,
        BAG
    }

    internal data class ParseNode(val type: ParseType,
                                  val token: Token?,
                                  val children: List<ParseNode>,
                                  val remaining: List<Token>) {
        val name = type.name.toLowerCase()

        /** Derives a [ParseNode] transforming the list of remaining tokens. */
        fun derive(tokensHandler: List<Token>.() -> List<Token>): ParseNode =
            copy(remaining = tokensHandler(remaining))

        fun deriveExpected(vararg types: TokenType): ParseNode = derive {
            var rem = this
            for (type in types) {
                if (type != rem.head?.type) {
                    rem.err("Expected $type")
                }
                rem = rem.tail
            }
            rem
        }

        fun deriveExpectedKeyword(keyword: String): ParseNode = derive { tailExpectedKeyword(keyword) }

        fun deriveChildren(transform: (List<ParseNode>) -> List<ParseNode>) =
            copy(children = transform(children))

        fun unsupported(message: String): Nothing =
            remaining.err(message, ::IllegalStateException)
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
        val node = this
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
                else -> unsupported("Unsupported atom token")
            }
            CAST, PATH, LIST, BAG -> sexp {
                addSymbol(node.name)
                addChildNodes(node)
            }
            STRUCT -> sexp {
                addSymbol("struct")
                // unfold the MEMBER nodes
                for (child in children) {
                    if (child.type != MEMBER) {
                        unsupported("Expected MEMBER node")
                    }
                    addChildNodes(child)
                }

            }
            UNARY, BINARY, TERNARY -> sexp {
                addSymbol(token?.text!!)
                addChildNodes(node)
            }
            SELECT_LIST, SELECT_VALUE -> sexp {
                addSymbol("select")
                addSexp {
                    var projection = children[0]

                    // unwrap the DISTINCT modifier
                    if (children[0].type == DISTINCT) {
                        addSymbol("project_distinct")
                        projection = projection.children[0]
                    } else {
                        addSymbol("project")
                    }

                    addSexp {
                        addSymbol(when (node.type) {
                            SELECT_LIST -> when {
                                projection.children.isEmpty() -> "*"
                                else -> "list"
                            }
                            SELECT_VALUE -> "value"
                            else -> unsupported("Unsupported SELECT type")
                        })
                        when (node.type) {
                            SELECT_VALUE -> add(projection.toSexp())
                            else -> addChildNodes(projection)
                        }
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
                            GROUP, GROUP_PARTIAL -> addSexp {
                                addSymbol(clause.name)
                                addSexp {
                                    addSymbol("by")
                                    addChildNodes(clause.children[0])
                                }
                                if (clause.children.size > 1) {
                                    addSexp {
                                        addSymbol("name")
                                        addSymbol(clause.children[1].token?.text!!)
                                    }
                                }
                            }
                            else -> clause.unsupported("Unsupported SELECT clause")
                        }
                    }
                }
            }
            CALL -> sexp {
                addSymbol("call")
                addSymbol(token?.text!!)
                addChildNodes(node)
            }
            CASE -> sexp {
                val clauses = when (children.size) {
                    1 -> {
                        addSymbol("searched_case")
                        children[0]
                    }
                    2 -> {
                        addSymbol("simple_case")
                        add(children[0].toSexp())
                        children[1]
                    }
                    else -> unsupported("CASE must be searched or simple")
                }

                clauses.children.forEachIndexed { idx, clause ->
                    add(
                        sexp {
                            addSymbol(
                                when (clause.type) {
                                    WHEN -> "when"
                                    ELSE -> "else"
                                    else -> unsupported("CASE clause must be WHEN or ELSE")
                                }
                            )
                        }.apply { addChildNodes(clause) }
                    )
                }
            }
            TYPE -> sexp {
                addSymbol("type")
                addSymbol(token?.text!!)
                for (child in children) {
                    add().newInt(child.token!!.value!!.longValue())
                }
            }
            AS_ALIAS, AT_ALIAS -> sexp {
                val tag = when (node.type) {
                    AS_ALIAS -> "as"
                    AT_ALIAS -> "at"
                    else -> unsupported("Bad alias node: ${node.type}")
                }
                addSymbol(tag)
                addSymbol(token?.text!!)
                addChildNodes(node)
            }
            else -> unsupported("Unsupported syntax for $type")
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
                rem.err("Expected infix operator")
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
                        rem = rem.tailExpectedKeyword("and")
                        val third = rem.parseExpression(
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
        OPERATOR -> when (head?.keywordText) {
            // the lexical scope operator is **only** allowed with identifiers
            "@" -> when (tail.head?.type) {
                IDENTIFIER -> ParseNode(
                    UNARY,
                    head,
                    listOf(tail.atomFromHead()),
                    tail.tail
                )
                else -> err("Identifier must follow @-operator")
            }
            else -> err("Unexpected operator")
        }
        KEYWORD -> when (head?.keywordText) {
            "case" -> when (tail.head?.keywordText) {
                "when" -> tail.parseCase(isSimple = false)
                else -> tail.parseCase(isSimple = true)
            }.deriveExpected()
            "cast" -> tail.parseCast()
            "select" -> tail.parseSelect()
            // table value constructor--which aliases to bag constructor in SQL++ with very
            // specific syntax
            "values" -> tail.parseTableValues().copy(type = BAG)
            in FUNCTION_NAME_KEYWORDS -> when (tail.head?.type) {
                LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
                else -> err("Unexpected keyword")
            }
            else -> err("Unexpected keyword")
        }
        LEFT_PAREN -> {
            val group = tail.parseArgList(
                aliasSupportType = NONE,
                supportsMemberName = false,
                boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
            ).deriveExpected(RIGHT_PAREN)

            when (group.children.size) {
                0 -> tail.err("Expression group cannot be empty")
                // expression grouping
                1 -> group.children[0].copy(remaining = group.remaining)
                // row value constructor--which aliases to list constructor in SQL++
                else -> group.copy(type = LIST)
            }
        }
        LEFT_BRACKET -> tail.parseListLiteral()
        LEFT_DOUBLE_ANGLE_BRACKET -> tail.parseBagLiteral()
        LEFT_CURLY -> tail.parseStructLiteral()
        IDENTIFIER -> when (tail.head?.type) {
            LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
            else -> atomFromHead()
        }
        LITERAL, MISSING -> atomFromHead()
        else -> err("Unexpected term")
    }

    private fun List<Token>.parseCase(isSimple: Boolean): ParseNode {
        var rem = this
        val children = ArrayList<ParseNode>()
        if (isSimple) {
            val valueExpr = parseExpression(boundaryTokenTypes = KEYWORD_BOUNDARY_TOKEN_TYPES)
            children.add(valueExpr)
            rem = valueExpr.remaining
        }

        val caseBody = rem.parseCaseBody()
        children.add(caseBody)
        rem = caseBody.remaining

        return ParseNode(CASE, null, children, rem)
    }

    private fun List<Token>.parseCaseBody(): ParseNode {
        val children = ArrayList<ParseNode>()
        var rem = this
        while (rem.head?.keywordText == "when") {
            val conditionExpr = rem.tail.parseExpression(
                boundaryTokenTypes = KEYWORD_BOUNDARY_TOKEN_TYPES
            ).deriveExpectedKeyword("then")
            rem = conditionExpr.remaining

            val result = rem.parseExpression(
                boundaryTokenTypes = KEYWORD_BOUNDARY_TOKEN_TYPES
            )
            rem = result.remaining

            children.add(ParseNode(WHEN, null, listOf(conditionExpr, result), rem))
        }
        if (children.isEmpty()) {
            err("Expected a WHEN clause in CASE")
        }
        if (rem.head?.keywordText == "else") {
            val elseExpr = rem.tail.parseExpression(
                boundaryTokenTypes = KEYWORD_BOUNDARY_TOKEN_TYPES
            )
            rem = elseExpr.remaining

            children.add(ParseNode(ELSE, null, listOf(elseExpr), rem))
        }

        return ParseNode(ARG_LIST, null, children, rem)
            .deriveExpectedKeyword("end")
    }

    private fun List<Token>.parseCast(): ParseNode {
        if (head?.type != LEFT_PAREN) {
            err("Missing left parenthesis after CAST")
        }
        val valueExpr = tail.parseExpression(
            boundaryTokenTypes = CAST_BOUNDARY_TOKEN_TYPES
        ).deriveExpected(AS)
        var rem = valueExpr.remaining

        val typeNode = rem.parseType().deriveExpected(RIGHT_PAREN)
        rem = typeNode.remaining

        return ParseNode(CAST, null, listOf(valueExpr, typeNode), rem)
    }

    private fun List<Token>.parseType(): ParseNode {
        val typeName = head?.keywordText
        val typeArity = TYPE_NAME_ARITY_MAP[typeName] ?: err("Expected type for CAST")

        val typeNode = when (tail.head?.type) {
            LEFT_PAREN -> tail.tail.parseArgList(
                aliasSupportType = NONE,
                supportsMemberName = false,
                boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
            ).copy(
                type = TYPE,
                token = head
            ).deriveExpected(RIGHT_PAREN)

            else -> ParseNode(TYPE, head, emptyList(), tail)
        }
        if (typeNode.children.size !in typeArity) {
            tail.err("CAST for $typeName must have arity of $typeArity")
        }
        for (child in typeNode.children) {
            if (child.type != ATOM
                    || child.token?.type != LITERAL
                    || !(child.token?.value?.isUnsignedInteger ?: false)) {
                err("Type parameter must be an unsigned integer literal")
            }
        }

        return typeNode
    }

    private fun List<Token>.parseSelect(): ParseNode {
        var rem = this
        val distinct = when (head?.keywordText) {
            "distinct" -> {
                rem = tail
                true
            }
            "all" -> {
                // SELECT ALL is default semantics
                rem = tail
                false
            }
            else -> false
        }


        var type = SELECT_LIST
        var projection = when {
            rem.head?.type == STAR -> {
                // special form for * is empty arg-list
                ParseNode(ARG_LIST, null, emptyList(), rem.tail)
            }
            rem.head?.keywordText == "value" -> {
                type = SELECT_VALUE
                rem.tail.parseExpression(boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES)
            }
            else -> {
                val list = rem.parseArgList(
                    aliasSupportType = AS_ONLY,
                    supportsMemberName = false,
                    boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
                )
                if (list.children.isEmpty()) {
                    rem.err("Cannot have empty SELECT list")
                }

                list
            }
        }
        if (distinct) {
            projection = ParseNode(DISTINCT, null, listOf(projection), projection.remaining)
        }

        return parseSelectAfterProjection(type, projection)
    }

    private fun parseSelectAfterProjection(selectType: ParseType, projection: ParseNode): ParseNode {
        val children = ArrayList<ParseNode>()
        var rem = projection.remaining
        children.add(projection)

        // TODO support SELECT with no FROM
        if (rem.head?.keywordText != "from") {
            rem.err("Expected FROM after SELECT list")
        }

        val fromList = rem.tail.parseArgList(
            aliasSupportType = AS_AND_AT,
            supportsMemberName = false,
            boundaryTokenTypes = SELECT_BOUNDARY_TOKEN_TYPES
        )

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

        if (rem.head?.keywordText == "group") {
            rem = rem.tail
            val type = when (rem.head?.keywordText) {
                "partial" -> {
                    rem = rem.tail
                    GROUP_PARTIAL
                }
                else -> GROUP
            }

            val groupChildren = ArrayList<ParseNode>()

            rem = rem.tailExpectedKeyword("by")

            val groupKey = rem.parseArgList(
                aliasSupportType = NONE,
                supportsMemberName = false,
                boundaryTokenTypes = KEYWORD_BOUNDARY_TOKEN_TYPES
            )
            groupKey.children.forEach {
                // TODO support ordinal case
                if (it.token?.type == LITERAL) {
                    it.token.err("Literals (including ordinals) not supported in GROUP BY")
                }
            }
            groupChildren.add(groupKey)
            rem = groupKey.remaining

            if (rem.head?.keywordText == "group") {
                rem = rem.tail.tailExpectedKeyword("as")

                if (rem.head?.type != IDENTIFIER) {
                    rem.err("Expected identifier for GROUP name")
                }
                groupChildren.add(rem.atomFromHead())
                rem = rem.tail
            }
            children.add(
                ParseNode(
                    type,
                    null,
                    groupChildren,
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

    private fun List<Token>.parseFunctionCall(name: Token): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            supportsMemberName = false,
            boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = CALL,
            token = name
        ).deriveExpected(RIGHT_PAREN)

    private fun List<Token>.parseListLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            supportsMemberName = false,
            boundaryTokenTypes = BRACKET_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = LIST
        ).deriveExpected(RIGHT_BRACKET)

    private fun List<Token>.parseBagLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            supportsMemberName = false,
            boundaryTokenTypes = BAG_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = BAG
        ).deriveExpected(RIGHT_DOUBLE_ANGLE_BRACKET)

    private fun List<Token>.parseStructLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            supportsMemberName = true,
            boundaryTokenTypes = STRUCT_BOUNDARY_TOKEN_TYPES
        ).copy(
            type = STRUCT
        ).deriveExpected(RIGHT_CURLY)

    private fun List<Token>.parseTableValues(): ParseNode =
        parseCommaList {
            var rem = this
            if (rem.head?.type != LEFT_PAREN) {
                err("Expected $LEFT_PAREN for row value constructor")
            }
            rem = rem.tail
            rem.parseArgList(
                aliasSupportType = NONE,
                supportsMemberName = false,
                boundaryTokenTypes = GROUP_AND_CALL_BOUNDARY_TOKEN_TYPES
            ).copy(
                type = LIST
            ).deriveExpected(RIGHT_PAREN)
        }

    private fun List<Token>.parseArgList(aliasSupportType: AliasSupportType,
                                         supportsMemberName: Boolean,
                                         boundaryTokenTypes: Set<TokenType> = emptySet()): ParseNode {
        val argListBoundaryTokenTypes = when(aliasSupportType) {
            AS_ONLY -> ARGLIST_WITH_ALIAS_BOUNDARY_TOKEN_TYPES
            AS_AND_AT -> ARGLIST_WITH_AT_BOUNDARY_TOKEN_TYPES
            else -> ARGLIST_BOUNDARY_TOKEN_TYPES
        } union boundaryTokenTypes

        fun List<Token>.parseField() = parseExpression(
            boundaryTokenTypes = FIELD_NAME_BOUNDARY_TOKEN_TYPES)
        fun List<Token>.parseChild() = parseExpression(
            boundaryTokenTypes = argListBoundaryTokenTypes)

        return parseCommaList(boundaryTokenTypes) {
            var rem = this
            var child = when {
                supportsMemberName -> {
                    val field = rem.parseField().deriveExpected(COLON)
                    rem = field.remaining
                    val value = rem.parseChild()
                    ParseNode(MEMBER, null, listOf(field, value), value.remaining)
                }
                else -> parseChild()
            }
            rem = child.remaining
            val aliasTokenType = rem.head?.type
            if (aliasSupportType.supportsAs
                    && (aliasTokenType == AS || aliasTokenType == IDENTIFIER)) {
                if (aliasTokenType == AS) {
                    rem = rem.tail
                }
                val name = rem.head
                if (name == null || name.type != IDENTIFIER) {
                    rem.err("Expected identifier for alias")
                }
                rem = rem.tail
                child = ParseNode(AS_ALIAS, name, listOf(child), rem)
            }

            if (aliasSupportType.supportsAt && rem.head?.type == AT) {
                rem = rem.tail
                val name = rem.head
                if (name?.type != IDENTIFIER) {
                    rem.err("Expected identifier for AT-name")
                }
                rem = rem.tail
                child = ParseNode(AT_ALIAS, name, listOf(child), rem)
            }

            child
        }
    }

    private inline fun List<Token>.parseCommaList(boundaryTokenTypes: Set<TokenType> = emptySet(),
                                                  parseItem: List<Token>.() -> ParseNode): ParseNode {
        val items = ArrayList<ParseNode>()
        var rem = this

        while (rem.isNotEmpty() && rem.head?.type !in boundaryTokenTypes) {
            val child = rem.parseItem()
            items.add(child)

            rem = child.remaining

            if (rem.head?.type != COMMA) {
                break
            }
            rem = rem.tail

        }
        return ParseNode(ARG_LIST, null, items, rem)
    }

    /** Entry point into the parser. */
    override fun parse(source: String): IonSexp =
        lexer.tokenize(source).parseExpression().toSexp().apply { makeReadOnly() }
}

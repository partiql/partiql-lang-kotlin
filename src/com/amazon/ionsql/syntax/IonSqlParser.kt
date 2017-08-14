/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionsql.errorhandling.*
import com.amazon.ionsql.syntax.IonSqlParser.AliasSupportType.*
import com.amazon.ionsql.syntax.IonSqlParser.ArgListMode.*
import com.amazon.ionsql.syntax.IonSqlParser.ParseType.*
import com.amazon.ionsql.syntax.TokenType.*
import com.amazon.ionsql.util.*
import java.util.*

/**
 * Parses a list of tokens as infix query expression into a prefix s-expression
 * as the abstract syntax tree.
 */
class IonSqlParser(private val ion: IonSystem,
                   private var errorHandler: IErrorHandler = DefaultErrorHandler()) : Parser {

    constructor (ion: IonSystem) : this(ion, DefaultErrorHandler())

    companion object {

        /**
         * Given an error context ([PropertyBag]) and a source position ([SourcePosition]) populate the given
         * error context with line and column information found in source position.
         */
        private fun populateLineAndColumn(errorContext: PropertyBag, sourcePosition: SourcePosition?) =
        when(sourcePosition) {
            null -> errorContext.addProperty(Property.LINE_NO, null)
                .addProperty(Property.COLUMN_NO, null)
            else -> {
                val (line, col) = sourcePosition
                errorContext.addProperty(Property.LINE_NO, line).
                    addProperty(Property.COLUMN_NO, col)
            }
        }

        private fun Token?.err(message: String, errorCode: ErrorCode, errorContext: PropertyBag = PropertyBag()): Nothing {
            when (this) {
                null -> throw ParserException(errorCode = errorCode, errorContext = errorContext)
                else -> throw ParserException(message,
                    errorCode,
                    populateLineAndColumn(errorContext, this.position)
                        .addPropertyIfKeyNotPresent(Property.TOKEN_TYPE, type)
                        .addPropertyIfKeyNotPresent(Property.TOKEN_VALUE, value))
            }

        }

        private fun List<Token>.err(message: String, errorCode: ErrorCode, errorContext: PropertyBag = PropertyBag()): Nothing =
            head.err(message, errorCode, errorContext)

        private fun List<Token>.atomFromHead(parseType: ParseType = ATOM): ParseNode =
            ParseNode(parseType, head, emptyList(), tail)

        private fun List<Token>.tailExpectedKeyword(keyword: String): List<Token> =
            when (head?.keywordText) {
                keyword -> tail
                else -> err("Expected ${keyword.toUpperCase()} keyword",
                    ErrorCode.PARSE_EXPECTED_KEYWORD,
                    PropertyBag().addProperty(Property.KEYWORD, keyword.toUpperCase()))
            }
    }

    private val lexer = IonSqlLexer(ion)

    internal enum class AliasSupportType(val supportsAs: Boolean, val supportsAt: Boolean) {
        NONE(supportsAs = false, supportsAt = false),
        AS_ONLY(supportsAs = true, supportsAt = false),
        AS_AND_AT(supportsAs = true, supportsAt = true)
    }

    internal enum class ArgListMode {
        NORMAL_ARG_LIST,
        STRUCT_LITERAL_ARG_LIST,
        FROM_CLAUSE_ARG_LIST
    }

    internal enum class ParseType(val isJoin: Boolean = false) {
        ATOM,
        PATH_WILDCARD,
        PATH_WILDCARD_UNPIVOT,
        SELECT_LIST,
        SELECT_VALUE,
        DISTINCT,
        INNER_JOIN(isJoin = true),
        LEFT_JOIN(isJoin = true),
        RIGHT_JOIN(isJoin = true),
        OUTER_JOIN(isJoin = true),
        WHERE,
        GROUP,
        GROUP_PARTIAL,
        HAVING,
        LIMIT,
        PIVOT,
        UNPIVOT,
        CALL,
        CALL_AGG,
        CALL_AGG_WILDCARD,
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
        BAG;

        val identifier = name.toLowerCase()
    }

    internal data class ParseNode(val type: ParseType,
                                  val token: Token?,
                                  val children: List<ParseNode>,
                                  val remaining: List<Token>) {
        /** Derives a [ParseNode] transforming the list of remaining tokens. */
        fun derive(tokensHandler: List<Token>.() -> List<Token>): ParseNode =
            copy(remaining = tokensHandler(remaining))

        fun deriveExpected(vararg types: TokenType): ParseNode = derive {
            var rem = this
            for (type in types) {
                if (type != rem.head?.type) {
                    rem.err("Expected $type",
                        ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
                        PropertyBag().addProperty(Property.TOKEN_TYPE, type))
                }
                rem = rem.tail
            }
            rem
        }

        fun deriveExpectedKeyword(keyword: String): ParseNode = derive { tailExpectedKeyword(keyword) }

        val isNumericLiteral = type == ATOM && when (token?.type) {
            LITERAL -> token.value?.isNumeric ?: false
            else -> false
        }

        fun numberValue(): Number = token?.value?.numberValue()
            ?: unsupported("Could not interpret token as number", ErrorCode.PARSE_EXPECTED_NUMBER)

        fun unsupported(message: String, errorCode: ErrorCode, errorContext: PropertyBag = PropertyBag()): Nothing =
            remaining.err(message, errorCode, errorContext)
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
                LITERAL, NULL -> sexp {
                    addSymbol("lit")
                    addClone(token.value!!)
                }
                MISSING -> sexp {
                    addSymbol("missing")
                }
                IDENTIFIER -> sexp {
                    addSymbol("id")
                    addSymbol(token.text!!)
                }
                else -> unsupported("Unsupported atom token", ErrorCode.PARSE_UNSUPPORTED_TOKEN)
            }
            CAST, PATH, LIST, BAG, UNPIVOT, MEMBER,
            INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, OUTER_JOIN -> sexp {
                addSymbol(node.type.identifier)
                addChildNodes(node)
            }
            STRUCT -> sexp {
                addSymbol("struct")
                // unfold the MEMBER nodes
                for (child in children) {
                    if (child.type != MEMBER) {
                        unsupported("Expected MEMBER node", ErrorCode.PARSE_EXPECTED_MEMBER)
                    }
                    addChildNodes(child)
                }

            }
            PATH_WILDCARD, PATH_WILDCARD_UNPIVOT -> sexp {
                addSymbol("*")
                if (node.type == PATH_WILDCARD_UNPIVOT) {
                    addSymbol("unpivot")
                }
            }
            UNARY, BINARY, TERNARY -> sexp {
                addSymbol(token?.text!!)
                addChildNodes(node)
            }
            PIVOT, SELECT_LIST, SELECT_VALUE -> sexp {
                when (node.type) {
                    PIVOT -> {
                        addSymbol("pivot")
                        add(children[0].toSexp())
                    }
                    else -> {
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
                                    else -> unsupported("Unsupported SELECT type", ErrorCode.PARSE_UNSUPPORTED_SELECT)
                                })
                                when (node.type) {
                                    SELECT_VALUE -> add(projection.toSexp())
                                    else -> addChildNodes(projection)
                                }
                            }
                        }
                    }
                }
                addSexp {
                    addSymbol("from")
                    // The FROM list in the parse tree is a ARG_LIST with
                    // potential JOIN nodes--we need to translate that into a single prefix
                    // node for the joins
                    val fromItem = children[1].children
                    var source = fromItem.head!!
                    for (fromJoinItem in fromItem.tail) {
                        if (!fromJoinItem.type.isJoin) {
                            unsupported("Non-first FROM clause item must be a JOIN")
                        }
                        // derive a binary operator type node
                        source = fromJoinItem.copy(
                            children = listOf(source) + fromJoinItem.children
                        )
                    }
                    add(source.toSexp())
                }
                if (children.size > 2) {
                    for (clause in children.slice(2..children.lastIndex)) {
                        when (clause.type) {
                            WHERE, HAVING, LIMIT -> addSexp {
                                addSymbol(clause.type.identifier)
                                addChildNodes(clause)
                            }
                            GROUP, GROUP_PARTIAL -> addSexp {
                                addSymbol(clause.type.identifier)
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
                            else -> clause.unsupported("Unsupported SELECT clause", ErrorCode.PARSE_UNSUPPORTED_SELECT)
                        }
                    }
                }
            }
            CALL, CALL_AGG, CALL_AGG_WILDCARD -> sexp {
                addSymbol(node.type.identifier)
                addSymbol(token?.text!!)
                if (node.type == CALL_AGG) {
                    // TODO IONSQL-93 support DISTINCT node modifier
                    addSymbol("all")
                }
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
                    else -> unsupported("CASE must be searched or simple", ErrorCode.PARSE_UNSUPPORTED_CASE)
                }

                clauses.children.forEach {
                    add(
                        sexp {
                            addSymbol(
                                when (it.type) {
                                    WHEN -> "when"
                                    ELSE -> "else"
                                    else -> unsupported("CASE clause must be WHEN or ELSE", ErrorCode.PARSE_UNSUPPORTED_CASE_CLAUSE)
                                }
                            )
                        }.apply { addChildNodes(it) }
                    )
                }
            }
            TYPE -> sexp {
                addSymbol("type")
                addSymbol(token?.keywordText!!)
                for (child in children) {
                    add().newInt(child.token!!.value!!.longValue())
                }
            }
            AS_ALIAS, AT_ALIAS -> sexp {
                val tag = when (node.type) {
                    AS_ALIAS -> "as"
                    AT_ALIAS -> "at"
                    else -> unsupported("Bad alias node: ${node.type}", ErrorCode.PARSE_UNSUPPORTED_ALIAS)
                }
                addSymbol(tag)
                addSymbol(token?.text!!)
                addChildNodes(node)
            }
            else -> unsupported("Unsupported syntax for $type", ErrorCode.PARSE_UNSUPPORTED_SYNTAX)
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
     *
     * @return The parse tree for the given expression.
     */
    internal fun List<Token>.parseExpression(precedence: Int = -1): ParseNode {
        var expr = parseUnaryTerm()
        var rem = expr.remaining

        fun headPrecedence() = rem.head?.infixPrecedence ?: 0

        // XXX this is a Pratt Top-Down Operator Precedence implementation
        while (!rem.isEmpty() && precedence < headPrecedence()) {
            val op = rem.head!!
            if (!op.isBinaryOperator && op.keywordText !in SPECIAL_INFIX_OPERATORS) {
                // unrecognized operator
                break
            }

            val right = when (op.keywordText) {
                // IS/IS NOT requires a type
                "is", "is_not" -> rem.tail.parseType()
                else -> {
                    if (rem.size < 3) {
                        rem.err("Missing right-hand side expression of infix operator", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                    } else {
                        rem.tail.parseExpression(
                            precedence = op.infixPrecedence
                        )
                    }
                }
            }
            rem = right.remaining

            expr = when {
                op.isBinaryOperator -> ParseNode(BINARY, op, listOf(expr, right), rem)
                else -> when (op.keywordText) {
                    "between", "not_between" -> {
                        val rest = rem.tailExpectedKeyword("and")
                        if (rest.onlyEof()) {
                            rem.head.err("Expected expression after AND", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                        } else {
                            rem = rest
                            val third = rem.parseExpression(
                                precedence = op.infixPrecedence
                            )
                            rem = third.remaining
                            ParseNode(TERNARY, op, listOf(expr, right, third), rem)
                        }
                    }
                    "like", "not_like" -> {
                        when  {
                            rem.head?.keywordText == "escape" -> {
                                val rest = rem.tailExpectedKeyword("escape")
                                if (rest.onlyEof()) {
                                    rem.head.err("Expected expression after ESCAPE", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                                } else {
                                    rem = rest
                                    val third = rem.parseExpression(precedence = op.infixPrecedence)
                                    rem = third.remaining
                                    ParseNode(TERNARY, op, listOf(expr, right, third), rem)
                                }
                            }
                            else -> ParseNode(BINARY, op, listOf(expr, right), rem)
                        }
                    }
                    else -> rem.err("Unknown infix operator", ErrorCode.PARSE_UNKNOWN_OPERATOR)
                }
            }
        }
        return expr
    }

    private fun List<Token>.parseUnaryTerm(): ParseNode =
        when (head?.isUnaryOperator) {
            true -> {
                val term = tail.parseUnaryTerm()

                var expr = ParseNode(
                    UNARY,
                    head,
                    listOf(term),
                    term.remaining
                )
                // constant fold unary plus/minus into constant literals
                when (head?.keywordText) {
                    "+" -> when {
                        term.isNumericLiteral -> {
                            // unary plus is a NO-OP
                            expr = term
                        }
                    }
                    "-" -> when {
                        term.isNumericLiteral -> {
                            val num = -term.numberValue()
                            expr = ParseNode(
                                ATOM,
                                term.token!!.copy(
                                    value = num.ionValue(ion)
                                ),
                                emptyList(),
                                term.remaining
                            )
                        }
                    }
                }

                expr
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

                    when (rem.head?.type) {
                        IDENTIFIER -> {
                            // re-write the identifier as a literal string element
                            val token = Token(LITERAL, ion.newString(rem.head?.text!!))
                            path.add(ParseNode(ATOM, token, emptyList(), rem.tail))
                        }
                        STAR -> path.add(
                            ParseNode(PATH_WILDCARD_UNPIVOT, rem.head, emptyList(), rem.tail)
                        )
                        else -> err("Invalid path dot component", ErrorCode.PARSE_INVALID_PATH_COMPONENT)
                    }
                    rem = rem.tail
                }
                LEFT_BRACKET -> {
                    rem = rem.tail
                    val expr = when (rem.head?.type) {
                        STAR -> ParseNode(PATH_WILDCARD, rem.head, emptyList(), rem.tail)
                        else -> rem.parseExpression()
                    }.deriveExpected(RIGHT_BRACKET)
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
                else -> err("Identifier must follow @-operator", ErrorCode.PARSE_MISSING_IDENT_AFTER_AT)
            }
            else -> err("Unexpected operator", ErrorCode.PARSE_UNEXPECTED_OPERATOR)
        }
        KEYWORD -> when (head?.keywordText) {
            "case" -> when (tail.head?.keywordText) {
                "when" -> tail.parseCase(isSimple = false)
                else -> tail.parseCase(isSimple = true)
            }.deriveExpected()
            "cast" -> tail.parseCast()
            "select" -> tail.parseSelect()
            "pivot" -> tail.parsePivot()
            // table value constructor--which aliases to bag constructor in SQL++ with very
            // specific syntax
            "values" -> tail.parseTableValues().copy(type = BAG)
            in FUNCTION_NAME_KEYWORDS -> when (tail.head?.type) {
                LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
                else -> err("Unexpected keyword", ErrorCode.PARSE_UNEXPECTED_KEYWORD)
            }
            else -> err("Unexpected keyword", ErrorCode.PARSE_UNEXPECTED_KEYWORD)
        }
        LEFT_PAREN -> {
            val group = tail.parseArgList(
                aliasSupportType = NONE,
                mode = NORMAL_ARG_LIST
            ).deriveExpected(RIGHT_PAREN)

            when (group.children.size) {
                0 -> tail.err("Expression group cannot be empty", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                // expression grouping
                1 -> group.children[0].copy(remaining = group.remaining)
                // row value constructor--which aliases to list constructor in SQL++
                else -> group.copy(type = LIST)
            }
        }
        LEFT_BRACKET -> when (tail.head?.type) {
            RIGHT_BRACKET -> ParseNode(LIST, null, emptyList(), tail.tail)
            else -> tail.parseListLiteral()
        }
        LEFT_DOUBLE_ANGLE_BRACKET -> when (tail.head?.type) {
            RIGHT_DOUBLE_ANGLE_BRACKET -> ParseNode(BAG, null, emptyList(), tail.tail)
            else -> tail.parseBagLiteral()
        }
        LEFT_CURLY -> when (tail.head?.type) {
            RIGHT_CURLY -> ParseNode(STRUCT, null, emptyList(), tail.tail)
            else -> tail.parseStructLiteral()
        }
        IDENTIFIER -> when (tail.head?.type) {
            LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
            else -> atomFromHead()
        }
        LITERAL, NULL, MISSING -> atomFromHead()
        else -> err("Unexpected term", ErrorCode.PARSE_UNEXPECTED_TERM)
    }

    private fun List<Token>.parseCase(isSimple: Boolean): ParseNode {
        var rem = this
        val children = ArrayList<ParseNode>()
        if (isSimple) {
            val valueExpr = parseExpression()
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
            val conditionExpr = rem.tail.parseExpression().deriveExpectedKeyword("then")
            rem = conditionExpr.remaining

            val result = rem.parseExpression()
            rem = result.remaining

            children.add(ParseNode(WHEN, null, listOf(conditionExpr, result), rem))
        }
        if (children.isEmpty()) {
            err("Expected a WHEN clause in CASE", ErrorCode.PARSE_EXPECTED_WHEN_CLAUSE)
        }
        if (rem.head?.keywordText == "else") {
            val elseExpr = rem.tail.parseExpression()
            rem = elseExpr.remaining

            children.add(ParseNode(ELSE, null, listOf(elseExpr), rem))
        }

        return ParseNode(ARG_LIST, null, children, rem)
            .deriveExpectedKeyword("end")
    }

    private fun List<Token>.parseCast(): ParseNode {
        if (head?.type != LEFT_PAREN) {
            err("Missing left parenthesis after CAST", ErrorCode.PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST)
        }
        val valueExpr = tail.parseExpression().deriveExpected(AS)
        var rem = valueExpr.remaining

        val typeNode = rem.parseType().deriveExpected(RIGHT_PAREN)
        rem = typeNode.remaining

        return ParseNode(CAST, null, listOf(valueExpr, typeNode), rem)
    }

    private fun List<Token>.parseType(): ParseNode {
        val typeName = head?.keywordText
        val typeArity = TYPE_NAME_ARITY_MAP[typeName] ?: err("Expected type name", ErrorCode.PARSE_EXPECTED_TYPE_NAME)

        val typeNode = when (tail.head?.type) {
            LEFT_PAREN -> tail.tail.parseArgList(
                aliasSupportType = NONE,
                mode = NORMAL_ARG_LIST
            ).copy(
                type = TYPE,
                token = head
            ).deriveExpected(RIGHT_PAREN)

            else -> ParseNode(TYPE, head, emptyList(), tail)
        }
        if (typeNode.children.size !in typeArity) {
            tail.err("CAST for $typeName must have arity of $typeArity",
                ErrorCode.PARSE_CAST_ARITY,
                PropertyBag().addProperty(Property.CAST_TO, typeName)
                    .addProperty(Property.EXPECTED_ARITY_MIN, typeArity.first)
                    .addProperty(Property.EXPECTED_ARITY_MAX, typeArity.last))
        }
        for (child in typeNode.children) {
            if (child.type != ATOM
                    || child.token?.type != LITERAL
                    || !(child.token.value?.isUnsignedInteger ?: false)) {
                err("Type parameter must be an unsigned integer literal", ErrorCode.PARSE_INVALID_TYPE_PARAM)
            }
        }

        return typeNode
    }

    private fun List<Token>.parsePivot(): ParseNode {
        var rem = this
        val value = rem.parseExpression().deriveExpectedKeyword("at")
        rem = value.remaining
        val name = rem.parseExpression()
        rem = name.remaining
        return parseSelectAfterProjection(PIVOT, ParseNode(MEMBER, null, listOf(name, value), rem))
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
                rem.tail.parseExpression()
            }
            else -> {
                val list = rem.parseArgList(
                    aliasSupportType = AS_ONLY,
                    mode = NORMAL_ARG_LIST
                )
                if (list.children.isEmpty()) {
                    rem.err("Cannot have empty SELECT list", ErrorCode.PARSE_EMPTY_SELECT)
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
            rem.err("Expected FROM after SELECT list", ErrorCode.PARSE_SELECT_MISSING_FROM)
        }

        val fromList = rem.tail.parseArgList(
            aliasSupportType = AS_AND_AT,
            mode = FROM_CLAUSE_ARG_LIST
        )

        rem = fromList.remaining
        children.add(fromList)

        fun parseOptionalSingleExpressionClause(type: ParseType) {
            if (rem.head?.keywordText == type.identifier) {
                val expr = rem.tail.parseExpression()
                rem = expr.remaining
                children.add(ParseNode(type, null, listOf(expr), rem))
            }
        }

        parseOptionalSingleExpressionClause(WHERE)

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
                aliasSupportType = AS_ONLY,
                mode = NORMAL_ARG_LIST
            )
            groupKey.children.forEach {
                // TODO support ordinal case
                if (it.token?.type == LITERAL) {
                    it.token.err("Literals (including ordinals) not supported in GROUP BY", ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY)
                }
            }
            groupChildren.add(groupKey)
            rem = groupKey.remaining

            if (rem.head?.keywordText == "group") {
                rem = rem.tail.tailExpectedKeyword("as")

                if (rem.head?.type != IDENTIFIER) {
                    rem.err("Expected identifier for GROUP name", ErrorCode.PARSE_EXPECTED_IDENT_FOR_GROUP_NAME)
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

        parseOptionalSingleExpressionClause(HAVING)

        parseOptionalSingleExpressionClause(LIMIT)

        return ParseNode(selectType, null, children, rem)
    }

    private fun List<Token>.parseFunctionCall(name: Token): ParseNode {
        val nameText = name.text!!
        var callType = when {
            // TODO make this injectable
            nameText in STANDARD_AGGREGATE_FUNCTIONS -> CALL_AGG
            else -> CALL
        }

        // TODO IONSQL-93 support DISTINCT/ALL syntax

        var call =  when (head?.type) {
            RIGHT_PAREN -> ParseNode(callType, name, emptyList(), tail)
            STAR -> {
                // support for special form COUNT(*)
                callType = CALL_AGG_WILDCARD
                if (nameText != "count") {
                    err("$nameText(*) is not allowed")
                }
                ParseNode(
                    callType,
                    name,
                    emptyList(),
                    tail
                ).deriveExpected(RIGHT_PAREN)
            }
            else -> {
                parseArgList(
                    aliasSupportType = NONE,
                    mode = NORMAL_ARG_LIST
                ).copy(
                    type = callType,
                    token = name
                ).deriveExpected(RIGHT_PAREN)
            }
        }

        if (callType == CALL_AGG && call.children.size != 1) {
            err("SQL aggregate functions are always unary")
        }

        return call
    }

    private fun List<Token>.parseListLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            mode = NORMAL_ARG_LIST
        ).copy(
            type = LIST
        ).deriveExpected(RIGHT_BRACKET)

    private fun List<Token>.parseBagLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            mode = NORMAL_ARG_LIST
        ).copy(
            type = BAG
        ).deriveExpected(RIGHT_DOUBLE_ANGLE_BRACKET)

    private fun List<Token>.parseStructLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = NONE,
            mode = STRUCT_LITERAL_ARG_LIST
        ).copy(
            type = STRUCT
        ).deriveExpected(RIGHT_CURLY)

    private fun List<Token>.parseTableValues(): ParseNode =
        parseCommaList {
            var rem = this
            if (rem.head?.type != LEFT_PAREN) {
                err("Expected $LEFT_PAREN for row value constructor", ErrorCode.PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR)
            }
            rem = rem.tail
            rem.parseArgList(
                aliasSupportType = NONE,
                mode = NORMAL_ARG_LIST
            ).copy(
                type = LIST
            ).deriveExpected(RIGHT_PAREN)
        }

    private val parseCommaDelim: List<Token>.() -> ParseNode? = {
        when (head?.type) {
            COMMA -> atomFromHead()
            else -> null
        }
    }

    private val parseJoinDelim: List<Token>.() -> ParseNode? = {
        when (head?.type) {
            COMMA -> atomFromHead(INNER_JOIN)
            KEYWORD -> when (head?.keywordText) {
                "join", "inner_join" -> atomFromHead(INNER_JOIN)
                "left_join" -> atomFromHead(LEFT_JOIN)
                "right_join" -> atomFromHead(RIGHT_JOIN)
                "outer_join" -> atomFromHead(OUTER_JOIN)
                else -> null
            }
            else -> null
        }
    }

    private fun List<Token>.parseArgList(aliasSupportType: AliasSupportType,
                                         mode: ArgListMode): ParseNode {
        val parseDelim = when (mode) {
            FROM_CLAUSE_ARG_LIST -> parseJoinDelim
            else -> parseCommaDelim
        }

        return parseDelimitedList(parseDelim) { delim ->
            var rem = this
            var child = when (mode) {
                STRUCT_LITERAL_ARG_LIST -> {
                    val field = rem.parseExpression().deriveExpected(COLON)
                    rem = field.remaining
                    val value = rem.parseExpression()
                    ParseNode(MEMBER, null, listOf(field, value), value.remaining)
                }
                FROM_CLAUSE_ARG_LIST -> {
                    when (rem.head?.keywordText) {
                        "unpivot" -> {
                            val actualChild = rem.tail.parseExpression()
                            ParseNode(
                                UNPIVOT,
                                null,
                                listOf(actualChild),
                                actualChild.remaining
                            )
                        }
                        else -> rem.parseExpression()
                    }
                }
                else -> rem.parseExpression()
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
                    rem.err("Expected identifier for alias", ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS)
                }
                rem = rem.tail
                child = ParseNode(AS_ALIAS, name, listOf(child), rem)
            }

            if (aliasSupportType.supportsAt && rem.head?.type == AT) {
                rem = rem.tail
                val name = rem.head
                if (name?.type != IDENTIFIER) {
                    rem.err("Expected identifier for AT-name", ErrorCode.PARSE_EXPECTED_IDENT_FOR_AT)
                }
                rem = rem.tail
                child = ParseNode(AT_ALIAS, name, listOf(child), rem)
            }

            if (delim?.type?.isJoin == true) {
                val operands = mutableListOf(child)

                // TODO determine if this should be restricted for some joins
                // check for an ON-clause
                if (rem.head?.keywordText == "on") {
                    val onClause = rem.tail.parseExpression()
                    rem = onClause.remaining
                    operands.add(onClause)
                }

                // wrap the join node based on the infix delimiter
                child = delim.copy(
                    children = operands,
                    remaining = rem
                )
            }

            child
        }
    }

    private inline fun List<Token>.parseCommaList(parseItem: List<Token>.() -> ParseNode) =
        parseDelimitedList(parseCommaDelim) { parseItem() }

    /**
     * Parses the given list-like construct.  This is typically for things like argument lists,
     * but can be used for other list-like constructs such as `JOIN` clauses.
     *
     * @param parseDelim the function to parse each delimiter, should return a non-null [ParseNode]
     *  if the delimiter is encountered and `null` if there is no delimiter (i.e. the end of the
     *  list has been reached.
     * @param parseItem the function to parse each item in a list, it is given the [ParseNode]
     *  of the delimiter that was encountered prior to the item to be parsed which could be `null`
     *  for the first item in the list.
     */
    private inline fun List<Token>.parseDelimitedList(parseDelim: List<Token>.() -> ParseNode?,
                                                      parseItem: List<Token>.(delim: ParseNode?) -> ParseNode): ParseNode {
        val items = ArrayList<ParseNode>()
        var delim: ParseNode? = null
        var rem = this

        while (rem.isNotEmpty()) {
            val child = rem.parseItem(delim)
            items.add(child)
            rem = child.remaining

            delim = rem.parseDelim()
            if (delim == null) {
                break
            }
            rem = delim.remaining

        }
        return ParseNode(ARG_LIST, null, items, rem)
    }

    /** Entry point into the parser. */
    override fun parse(source: String): IonSexp {
        val node = lexer.tokenize(source).parseExpression()
        val rem = node.remaining
        if (!rem.onlyEof()) {
            rem.err("Unexpected token after expression", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
        return node.toSexp().apply { makeReadOnly() }
    }

    /** Entry point into the parser. */
    override fun parse(source: String, errorHandler: IErrorHandler): IonSexp {
        this.errorHandler = errorHandler
        return parse(source)
    }
}

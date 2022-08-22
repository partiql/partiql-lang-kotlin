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

package org.partiql.lang.syntax

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.metaContainerOf
import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.ast.IonElementMetaContainer
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.IsImplictJoinMeta
import org.partiql.lang.ast.IsIonLiteralMeta
import org.partiql.lang.ast.LegacyLogicalNotMeta
import org.partiql.lang.ast.Meta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.SqlDataType
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.time.DATE_PATTERN_REGEX
import org.partiql.lang.eval.time.MAX_PRECISION_FOR_TIME
import org.partiql.lang.eval.time.genericTimeRegex
import org.partiql.lang.eval.time.getPrecisionFromTimeString
import org.partiql.lang.eval.time.timeWithoutTimeZoneRegex
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.BuiltInScalarTypeId
import org.partiql.lang.util.asIonInt
import org.partiql.lang.util.atomFromHead
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.err
import org.partiql.lang.util.errExpectedTokenType
import org.partiql.lang.util.head
import org.partiql.lang.util.ionValue
import org.partiql.lang.util.isNumeric
import org.partiql.lang.util.isText
import org.partiql.lang.util.isUnsignedInteger
import org.partiql.lang.util.longValue
import org.partiql.lang.util.numberValue
import org.partiql.lang.util.onlyEndOfStatement
import org.partiql.lang.util.stringValue
import org.partiql.lang.util.tail
import org.partiql.lang.util.tailExpectedKeyword
import org.partiql.lang.util.tailExpectedToken
import org.partiql.lang.util.unaryMinus
import org.partiql.pig.runtime.LongPrimitive
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.runtime.toIonElement
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_TIME
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal

/**
 * Parses a list of tokens as infix query expression into a prefix s-expression
 * as the abstract syntax tree.
 */
class SqlParser(
    private val ion: IonSystem,
    customTypes: List<CustomType> = listOf()
) : Parser {

    private val CUSTOM_KEYWORDS =
        customTypes.map { it.name.toLowerCase() }

    private val CUSTOM_TYPE_ALIASES =
        customTypes.map { customType ->
            customType.aliases.map { alias ->
                Pair(alias.toLowerCase(), customType.name.toLowerCase())
            }
        }.flatten().toMap()

    internal enum class AliasSupportType(val supportsAs: Boolean, val supportsAt: Boolean, val supportsBy: Boolean) {
        NONE(supportsAs = false, supportsAt = false, supportsBy = false),
        AS_ONLY(supportsAs = true, supportsAt = false, supportsBy = false),
        AS_AT_BY(supportsAs = true, supportsAt = true, supportsBy = true)
    }

    internal enum class ArgListMode {
        NORMAL_ARG_LIST,
        STRUCT_LITERAL_ARG_LIST,
        SIMPLE_PATH_ARG_LIST,
        SET_CLAUSE_ARG_LIST
    }

    private enum class PathMode {
        FULL_PATH,
        SIMPLE_PATH
    }

    /** If the property [isTopLevelType] is true, then the parse node of this ParseType will only be valid at top level in the query.
     *  For example, EXEC, DDL and DML keywords can only be used at the top level in the query.
     */
    internal enum class ParseType(val isJoin: Boolean = false, val isTopLevelType: Boolean = false, val isDml: Boolean = false) {
        ATOM,
        CASE_SENSITIVE_ATOM,
        CASE_INSENSITIVE_ATOM,
        PROJECT_ALL, // Wildcard, i.e. the * in `SELECT * FROM f` and a.b.c.* in `SELECT a.b.c.* FROM f`
        PATH_WILDCARD,
        PATH_UNPIVOT,
        LET,
        SELECT_LIST,
        SELECT_VALUE,
        DISTINCT,
        INNER_JOIN(isJoin = true),
        LEFT_JOIN(isJoin = true),
        RIGHT_JOIN(isJoin = true),
        OUTER_JOIN(isJoin = true),
        WHERE,
        ORDER_BY,
        SORT_SPEC,
        ORDERING_SPEC,
        NULLS_SPEC,
        GROUP,
        GROUP_PARTIAL,
        HAVING,
        LIMIT,
        OFFSET,
        PIVOT,
        UNPIVOT,
        CALL,
        DATE,
        TIME,
        TIME_WITH_TIME_ZONE,
        CALL_AGG,
        CALL_DISTINCT_AGG,
        CALL_AGG_WILDCARD,
        ARG_LIST,
        AS_ALIAS,
        AT_ALIAS,
        BY_ALIAS,
        PATH,
        PATH_DOT,
        PATH_SQB, // SQB = SQuare Bracket
        UNARY,
        BINARY,
        TERNARY,
        LIST,
        STRUCT,
        MEMBER,
        TYPE_FUNCTION,
        TYPE,
        CASE,
        WHEN,
        ELSE,
        NULLIF,
        COALESCE,
        BAG,
        INSERT(isTopLevelType = true, isDml = true),
        INSERT_VALUE(isTopLevelType = true, isDml = true),
        REMOVE(isTopLevelType = true, isDml = true),
        SET(isTopLevelType = true, isDml = true),
        UPDATE(isTopLevelType = true, isDml = true),
        DELETE(isTopLevelType = true, isDml = true),
        ASSIGNMENT,
        FROM,
        FROM_CLAUSE,
        FROM_SOURCE_JOIN,
        MATCH,
        MATCH_EXPR,
        MATCH_EXPR_NODE,
        MATCH_EXPR_EDGE,
        MATCH_EXPR_EDGE_DIRECTION,
        MATCH_EXPR_NAME,
        MATCH_EXPR_LABEL,
        MATCH_EXPR_QUANTIFIER,
        MATCH_EXPR_RESTRICTOR,
        MATCH_EXPR_SELECTOR,
        CHECK,
        ON_CONFLICT,
        CONFLICT_ACTION,
        DML_LIST(isTopLevelType = true, isDml = true),
        RETURNING,
        RETURNING_ELEM,
        RETURNING_MAPPING,
        RETURNING_WILDCARD,
        CREATE_TABLE(isTopLevelType = true),
        DROP_TABLE(isTopLevelType = true),
        DROP_INDEX(isTopLevelType = true),
        CREATE_INDEX(isTopLevelType = true),
        PARAMETER,
        EXEC(isTopLevelType = true),
        PRECISION;

        val identifier = name.toLowerCase()
    }

    internal data class ParseNode(
        val type: ParseType,
        val token: Token?,
        val children: List<ParseNode>,
        val remaining: List<Token>
    ) {

        /** Derives a [ParseNode] transforming the list of remaining tokens. */
        private fun derive(tokensHandler: List<Token>.() -> List<Token>): ParseNode =
            copy(remaining = tokensHandler(remaining))

        fun deriveExpected(expectedType: TokenType): ParseNode = derive {
            if (expectedType != this.head?.type) {
                head.errExpectedTokenType(expectedType)
            }
            this.tail
        }

        fun deriveExpected(expectedType1: TokenType, expectedType2: TokenType): Pair<ParseNode, Token> =
            if (expectedType1 != this.remaining.head?.type && expectedType2 != this.remaining.head?.type) {
                val pvmap = PropertyValueMap()
                pvmap[Property.EXPECTED_TOKEN_TYPE_1_OF_2] = expectedType1
                pvmap[Property.EXPECTED_TOKEN_TYPE_2_OF_2] = expectedType2
                this.remaining.err("Expected $type", ErrorCode.PARSE_EXPECTED_2_TOKEN_TYPES, pvmap)
            } else {
                Pair(copy(remaining = this.remaining.tail), this.remaining.head!!)
            }

        fun deriveExpectedKeyword(keyword: String): ParseNode = derive { tailExpectedKeyword(keyword) }

        val isNumericLiteral = type == ParseType.ATOM && when (token?.type) {
            TokenType.LITERAL, TokenType.ION_LITERAL -> token.value?.isNumeric ?: false
            else -> false
        }

        fun numberValue(): Number = token?.value?.numberValue()
            ?: unsupported("Could not interpret token as number", ErrorCode.PARSE_EXPECTED_NUMBER)

        fun unsupported(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap = PropertyValueMap()): Nothing =
            remaining.err(message, errorCode, errorContext)

        fun errMalformedParseTree(message: String): Nothing {
            val context = PropertyValueMap()
            token?.span?.let {
                context[Property.LINE_NUMBER] = it.line
                context[Property.COLUMN_NUMBER] = it.column
            }
            throw ParserException(message, ErrorCode.PARSE_MALFORMED_PARSE_TREE, context)
        }
    }

    private fun Token.toSourceLocation() = SourceLocationMeta(span.line, span.column, span.length)

    private fun Token?.toSourceLocationMetaContainer(): IonElementMetaContainer =
        if (this == null) {
            emptyMetaContainer()
        } else {
            val sourceLocation = toSourceLocation()
            metaContainerOf(Pair(sourceLocation.tag, sourceLocation))
        }

    private fun ParseNode.malformedIfNotEmpty(unconsumedChildren: List<ParseNode>) {
        if (unconsumedChildren.isNotEmpty()) {
            errMalformedParseTree("Unprocessed components remaining")
        }
    }

    // ***************************************
    // toAstStatement
    // ***************************************
    private fun ParseNode.toAstStatement(): PartiqlAst.Statement {
        return when (type) {
            ParseType.ATOM, ParseType.LIST, ParseType.BAG, ParseType.STRUCT, ParseType.UNARY, ParseType.BINARY,
            ParseType.TERNARY, ParseType.TYPE_FUNCTION, ParseType.CALL, ParseType.CALL_AGG, ParseType.NULLIF,
            ParseType.COALESCE, ParseType.CALL_DISTINCT_AGG, ParseType.CALL_AGG_WILDCARD, ParseType.PATH,
            ParseType.PARAMETER, ParseType.CASE, ParseType.SELECT_LIST, ParseType.SELECT_VALUE, ParseType.PIVOT,
            ParseType.DATE, ParseType.TIME, ParseType.TIME_WITH_TIME_ZONE -> PartiqlAst.build { query(toAstExpr(), getMetas()) }

            ParseType.FROM, ParseType.INSERT, ParseType.INSERT_VALUE, ParseType.SET, ParseType.UPDATE, ParseType.REMOVE,
            ParseType.DELETE, ParseType.DML_LIST -> toAstDml()

            ParseType.CREATE_TABLE, ParseType.DROP_TABLE, ParseType.CREATE_INDEX, ParseType.DROP_INDEX -> toAstDdl()

            ParseType.EXEC -> toAstExec()

            else -> unsupported("Unsupported syntax for $type", ErrorCode.PARSE_UNSUPPORTED_SYNTAX)
        }
    }

    private fun ParseNode.toAstExpr(): PartiqlAst.Expr {
        checkThreadInterrupted()
        val metas = getMetas()

        return PartiqlAst.build {
            when (type) {
                ParseType.ATOM -> when (token?.type) {
                    TokenType.LITERAL, TokenType.NULL, TokenType.TRIM_SPECIFICATION, TokenType.DATETIME_PART -> lit(token.value!!.toIonElement(), metas)
                    TokenType.ION_LITERAL -> lit(token.value!!.toIonElement(), metas + metaContainerOf(IsIonLiteralMeta.instance))
                    TokenType.MISSING -> missing(metas)
                    TokenType.QUOTED_IDENTIFIER -> id(token.text!!, caseSensitive(), unqualified(), metas)
                    TokenType.IDENTIFIER -> id(token.text!!, caseInsensitive(), unqualified(), metas)
                    else -> errMalformedParseTree("Unsupported atom token type ${token?.type}")
                }
                ParseType.LIST -> list(children.map { it.toAstExpr() }, metas)
                ParseType.BAG -> bag(children.map { it.toAstExpr() }, metas)
                ParseType.STRUCT -> {
                    val fields = children.map {
                        if (it.type != ParseType.MEMBER) {
                            errMalformedParseTree("Expected MEMBER node as direct descendant of a STRUCT node but instead found ${it.type}")
                        }
                        if (it.children.size != 2) {
                            errMalformedParseTree("Expected MEMBER node to have 2 children but found ${it.children.size}")
                        }
                        val keyExpr = it.children[0].toAstExpr()
                        val valueExpr = it.children[1].toAstExpr()
                        PartiqlAst.ExprPair(keyExpr, valueExpr)
                    }
                    struct(fields, metas)
                }
                ParseType.UNARY, ParseType.BINARY, ParseType.TERNARY -> {
                    when (token!!.text) {
                        "is" -> isType(children[0].toAstExpr(), children[1].toAstType(), metas)
                        "is_not" -> not(
                            isType(children[0].toAstExpr(), children[1].toAstType(), metas),
                            metas + metaContainerOf(LegacyLogicalNotMeta.instance)
                        )
                        else -> {
                            val (opName, wrapInNot) = when (token.text) {
                                "not_between" -> Pair("between", true)
                                "not_like" -> Pair("like", true)
                                "not_in" -> Pair("in", true)
                                else -> Pair(token.text!!, false)
                            }

                            when (opName) {
                                "@" -> {
                                    val childNode = children[0]
                                    val childToken = childNode.token
                                        ?: errMalformedParseTree("@ node does not have a token")
                                    when (childToken.type) {
                                        TokenType.QUOTED_IDENTIFIER -> id(childToken.text!!, caseSensitive(), localsFirst(), childNode.getMetas())
                                        TokenType.IDENTIFIER -> id(childToken.text!!, caseInsensitive(), localsFirst(), childNode.getMetas())
                                        else -> errMalformedParseTree("Unexpected child node token type of @ operator node $childToken")
                                    }
                                }
                                else -> {
                                    val args = children.map { it.toAstExpr() }
                                    val node = opName.toOperator(args, metas)

                                    if (wrapInNot) {
                                        not(node, metas + metaContainerOf(LegacyLogicalNotMeta.instance))
                                    } else {
                                        node
                                    }
                                }
                            }
                        }
                    }
                }
                ParseType.NULLIF -> {
                    nullIf(
                        children[0].toAstExpr(),
                        children[1].toAstExpr(),
                        metas
                    )
                }
                ParseType.COALESCE -> {
                    coalesce(
                        children.map { it.toAstExpr() },
                        metas
                    )
                }
                ParseType.TYPE_FUNCTION -> {
                    val funcExpr = children[0].toAstExpr()
                    val dataType = children[1].toAstType()
                    when (token?.keywordText) {
                        "cast" -> cast(funcExpr, dataType, metas)
                        "can_cast" -> canCast(funcExpr, dataType, metas)
                        "can_lossless_cast" -> canLosslessCast(funcExpr, dataType, metas)
                        else -> errMalformedParseTree("Unexpected type function token: $token")
                    }
                }
                ParseType.CALL -> {
                    when (val funcName = token?.text!!.toLowerCase()) {
                        // special case--list/sexp/bag "functions" are intrinsic to the literal form
                        "sexp" -> sexp(children.map { it.toAstExpr() }, metas)
                        "list" -> list(children.map { it.toAstExpr() }, metas)
                        "bag" -> bag(children.map { it.toAstExpr() }, metas)
                        else -> {
                            // Note:  we are forcing all function name lookups to be case-insensitive here...
                            // This seems like the right thing to do because that is consistent with the
                            // previous behavior.
                            call(funcName, children.map { it.toAstExpr() }, metas)
                        }
                    }
                }
                ParseType.CALL_AGG -> {
                    val funcName = SymbolPrimitive(token?.text!!.toLowerCase(), emptyMetaContainer())
                    callAgg_(all(), funcName, children[0].toAstExpr(), metas)
                }
                ParseType.CALL_DISTINCT_AGG -> {
                    val funcName = SymbolPrimitive(token?.text!!.toLowerCase(), emptyMetaContainer())
                    callAgg_(distinct(), funcName, children[0].toAstExpr(), metas)
                }
                ParseType.CALL_AGG_WILDCARD -> {
                    if (token!!.type != TokenType.KEYWORD || token.keywordText != "count") {
                        errMalformedParseTree("only COUNT can be used with a wildcard")
                    }
                    // Should only get the [SourceLocationMeta] if present, not any other metas.
                    val srcLocationMetaOnly = metas[SourceLocationMeta.TAG]
                        ?.let { metaContainerOf(it as Meta) } ?: emptyMetaContainer()
                    val lit = lit(ionInt(1), srcLocationMetaOnly)
                    val symbolicPrimitive = SymbolPrimitive("count", srcLocationMetaOnly)
                    callAgg_(all(), symbolicPrimitive, lit, metas + metaContainerOf(IsCountStarMeta.instance))
                }
                ParseType.PATH -> {
                    val rootExpr = children[0].toAstExpr()
                    val pathComponents = children.drop(1).map {
                        when (it.type) {
                            ParseType.PATH_DOT -> {
                                if (it.children.size != 1) {
                                    errMalformedParseTree("Unexpected number of child elements in PATH_DOT ParseNode")
                                }
                                val atomParseNode = it.children[0]
                                val atomMetas = atomParseNode.getMetas()
                                when (atomParseNode.type) {
                                    ParseType.CASE_SENSITIVE_ATOM, ParseType.CASE_INSENSITIVE_ATOM -> {
                                        val lit = lit(ionString(atomParseNode.token?.text!!), atomMetas)
                                        val caseSensitivity = if (atomParseNode.type == ParseType.CASE_SENSITIVE_ATOM) caseSensitive() else caseInsensitive()
                                        pathExpr(lit, caseSensitivity, atomMetas)
                                    }
                                    ParseType.PATH_UNPIVOT -> pathUnpivot(atomMetas)
                                    else -> errMalformedParseTree("Unsupported child path node of PATH_DOT")
                                }
                            }
                            ParseType.PATH_SQB -> {
                                if (it.children.size != 1) {
                                    errMalformedParseTree("Unexpected number of child elements in PATH_SQB ParseNode")
                                }
                                val child = it.children[0]
                                val childMetas = child.getMetas()
                                if (child.type == ParseType.PATH_WILDCARD) pathWildcard(childMetas) else pathExpr(child.toAstExpr(), caseSensitive(), childMetas)
                            }
                            else -> errMalformedParseTree("Unsupported path component: ${it.type}")
                        }
                    }
                    path(rootExpr, pathComponents, rootExpr.metas) // Here we use its root source location, since itself has no source location (not a token).
                }
                ParseType.PARAMETER -> parameter(token!!.value!!.longValue(), metas)
                ParseType.CASE -> {
                    val branches = ArrayList<PartiqlAst.ExprPair>()
                    val cases = exprPairList(branches)
                    var elseExpr: PartiqlAst.Expr? = null

                    fun ParseNode.addCases() = children.forEach {
                        when (it.type) {
                            ParseType.WHEN ->
                                branches.add(exprPair(it.children[0].toAstExpr(), it.children[1].toAstExpr()))
                            ParseType.ELSE -> elseExpr = it.children[0].toAstExpr()
                            else -> errMalformedParseTree("CASE clause must be WHEN or ELSE")
                        }
                    }

                    when (children.size) {
                        // Searched CASE
                        1 -> {
                            children[0].addCases()
                            searchedCase(cases, elseExpr, metas)
                        }
                        // Simple CASE
                        2 -> {
                            val valueExpr = children[0].toAstExpr()
                            children[1].addCases()
                            simpleCase(valueExpr, cases, elseExpr, metas)
                        }
                        else -> errMalformedParseTree("CASE must be searched or simple")
                    }
                }
                ParseType.SELECT_LIST, ParseType.SELECT_VALUE, ParseType.PIVOT -> {
                    // The first child of a SELECT_LIST parse node can be either DISTINCT or ARG_LIST.
                    // If it is ARG_LIST, the children of that node are the select items and the SetQuantifier is ALL
                    // If it is DISTINCT, the SetQuantifier is DISTINCT and there should be one child node, an ARG_LIST
                    // containing the select items.

                    // The second child of a SELECT_LIST is always an ARG_LIST containing the from clause.

                    // GROUP BY, GROUP PARTIAL BY, WHERE, HAVING, LIMIT and OFFSET parse nodes each have distinct ParseNodeTypes
                    // and if present, exist in children, starting at the third position.

                    // If the query parsed was a `SELECT DISTINCT ...`, children[0] is of type DISTINCT and its
                    // children are the actual select list.
                    val setQuantifier = if (children[0].type == ParseType.DISTINCT) distinct() else null
                    val selectList = if (children[0].type == ParseType.DISTINCT) children[0].children[0] else children[0]

                    val fromList = children[1]
                    if (fromList.type != ParseType.FROM_CLAUSE) {
                        errMalformedParseTree("Invalid second child of SELECT_LIST")
                    }

                    if (fromList.children.size != 1) {
                        errMalformedParseTree("Invalid FROM clause children length")
                    }

                    // We will remove items from this collection as we consume them.
                    // If any unconsumed children remain, we've missed something and should throw an exception.
                    val unconsumedChildren = children.drop(2).toMutableList()

                    val projection = when (type) {
                        ParseType.SELECT_LIST -> {
                            // We deal with ProjectStar first
                            val childNodes = selectList.children
                            if (childNodes.any { it.type == ParseType.PROJECT_ALL && it.children.isEmpty() }) {
                                if (childNodes.size > 1) error("More than one select item when SELECT * was present.")
                                projectStar(childNodes[0].getMetas())
                            } else {
                                val selectListItems = childNodes.map {
                                    when (it.type) {
                                        ParseType.PROJECT_ALL -> projectAll(it.children[0].toAstExpr())
                                        else -> {
                                            val (asAliasSymbol, parseNode) = it.unwrapAsAlias()
                                            projectExpr_(parseNode.toAstExpr(), asAliasSymbol)
                                        }
                                    }
                                }
                                projectList(selectListItems, metas)
                            }
                        }
                        ParseType.SELECT_VALUE -> projectValue(selectList.toAstExpr())
                        ParseType.PIVOT -> {
                            val member = children[0]
                            val asExpr = member.children[0].toAstExpr()
                            val atExpr = member.children[1].toAstExpr()
                            projectPivot(asExpr, atExpr)
                        }
                        else -> throw IllegalStateException("This can never happen!")
                    }

                    val fromSource = fromList.children[0].toFromSource()

                    val fromLet = unconsumedChildren.firstOrNull { it.type == ParseType.LET }?.let {
                        unconsumedChildren.remove(it)
                        it.toLetSource()
                    }

                    val whereExpr = unconsumedChildren.firstOrNull { it.type == ParseType.WHERE }?.let {
                        unconsumedChildren.remove(it)
                        it.children[0].toAstExpr()
                    }

                    val groupBy = unconsumedChildren.firstOrNull {
                        it.type == ParseType.GROUP ||
                            it.type == ParseType.GROUP_PARTIAL
                    }?.let {
                        unconsumedChildren.remove(it)
                        val groupingStrategy = when (it.type) {
                            ParseType.GROUP -> groupFull()
                            else -> groupPartial()
                        }
                        val groupAsName = if (it.children.size > 1) it.children[1].toSymbolicName() else null
                        val keyList = it.children[0].children.map {
                            val (alias, groupByItemNode) = it.unwrapAsAlias()
                            groupKey_(groupByItemNode.toAstExpr(), alias)
                        }

                        groupBy_(
                            groupingStrategy,
                            groupKeyList(keyList),
                            groupAsName,
                            it.getMetas()
                        )
                    }

                    val havingExpr = unconsumedChildren.firstOrNull { it.type == ParseType.HAVING }?.let {
                        unconsumedChildren.remove(it)
                        it.children[0].toAstExpr()
                    }

                    val orderBy = unconsumedChildren.firstOrNull { it.type == ParseType.ORDER_BY }?.let {
                        unconsumedChildren.remove(it)
                        orderBy(
                            it.children[0].children.map {
                                when (it.children.size) {
                                    1 -> sortSpec(it.children[0].toAstExpr(), asc(), nullsLast())
                                    2 -> when (it.children[1].type) {
                                        ParseType.ORDERING_SPEC -> {
                                            val orderingSpec = it.children[1].toOrderingSpec()
                                            val defaultNullsSpec = when (orderingSpec) {
                                                is PartiqlAst.OrderingSpec.Asc -> nullsLast()
                                                is PartiqlAst.OrderingSpec.Desc -> nullsFirst()
                                            }
                                            sortSpec(it.children[0].toAstExpr(), orderingSpec, defaultNullsSpec)
                                        }
                                        ParseType.NULLS_SPEC -> sortSpec(it.children[0].toAstExpr(), asc(), it.children[1].toNullsSpec())
                                        else -> errMalformedParseTree("Invalid ordering expressions syntax")
                                    }
                                    3 -> sortSpec(it.children[0].toAstExpr(), it.children[1].toOrderingSpec(), it.children[2].toNullsSpec())
                                    else -> errMalformedParseTree("Invalid ordering expressions syntax")
                                }
                            },
                            it.getMetas()
                        )
                    }

                    val limitExpr = unconsumedChildren.firstOrNull { it.type == ParseType.LIMIT }?.let {
                        unconsumedChildren.remove(it)
                        it.children[0].toAstExpr()
                    }

                    val offsetExpr = unconsumedChildren.firstOrNull { it.type == ParseType.OFFSET }?.let {
                        unconsumedChildren.remove(it)
                        it.children[0].toAstExpr()
                    }

                    malformedIfNotEmpty(unconsumedChildren)

                    select(
                        setq = setQuantifier,
                        project = projection,
                        from = fromSource,
                        fromLet = fromLet,
                        where = whereExpr,
                        group = groupBy,
                        having = havingExpr,
                        order = orderBy,
                        limit = limitExpr,
                        offset = offsetExpr,
                        metas = metas
                    )
                }
                ParseType.DATE -> {
                    val dateString = token!!.text!!
                    val (year, month, day) = dateString.split("-")
                    date(year.toLong(), month.toLong(), day.toLong(), metas)
                }
                ParseType.TIME -> {
                    val timeString = token!!.text!!
                    val precision = children[0].token!!.value!!.numberValue().toLong()
                    val time = LocalTime.parse(timeString, ISO_TIME)
                    litTime(
                        timeValue(
                            time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                            precision, false, null
                        ),
                        metas
                    )
                }
                ParseType.TIME_WITH_TIME_ZONE -> {
                    val timeString = token!!.text!!
                    val precision = children[0].token!!.value!!.longValue()
                    try {
                        val time = OffsetTime.parse(timeString)
                        litTime(
                            timeValue(
                                time.hour.toLong(), time.minute.toLong(), time.second.toLong(),
                                time.nano.toLong(), precision, true, (time.offset.totalSeconds / 60).toLong()
                            ),
                            metas
                        )
                    } catch (e: DateTimeParseException) {
                        // In case time zone not explicitly specified
                        val time = LocalTime.parse(timeString)
                        litTime(
                            timeValue(
                                time.hour.toLong(), time.minute.toLong(), time.second.toLong(),
                                time.nano.toLong(), precision, true, null
                            ),
                            metas
                        )
                    }
                }
                else -> error("Can't transform ParseType.$type to a PartiqlAst.expr }")
            }
        }
    }

    private fun ParseNode.toAstDml(): PartiqlAst.Statement.Dml {
        val metas = getMetas()

        return PartiqlAst.build {
            when (type) {
                ParseType.FROM -> {
                    // The first child is the operation, the second child is the from list,
                    // each child following is an optional clause (e.g. ORDER BY)
                    val operation = children[0].toAstDml()
                    val fromSource = children[1].also {
                        if (it.type != ParseType.FROM_CLAUSE) {
                            errMalformedParseTree("Invalid second child of FROM")
                        }

                        if (it.children.size != 1) {
                            errMalformedParseTree("Invalid FROM clause children length")
                        }
                    }.children[0].toFromSource()

                    // We will remove items from this collection as we consume them.
                    // If any unconsumed children remain, we've missed something and should throw an exception.
                    val unconsumedChildren = children.drop(2).toMutableList()

                    val where = unconsumedChildren.firstOrNull { it.type == ParseType.WHERE }?.let {
                        unconsumedChildren.remove(it)
                        it.children[0].toAstExpr()
                    }
                    val returning = unconsumedChildren.firstOrNull { it.type == ParseType.RETURNING }?.let {
                        unconsumedChildren.remove(it)
                        it.toReturningExpr()
                    }

                    // Throw an exception if any unconsumed children remain
                    malformedIfNotEmpty(unconsumedChildren)

                    operation.copy(from = fromSource, where = where, returning = returning, metas = metas)
                }
                ParseType.INSERT, ParseType.INSERT_VALUE -> {
                    val insertReturning = toInsertReturning()
                    dml(
                        dmlOpList(insertReturning.ops),
                        returning = insertReturning.returning,
                        metas = metas
                    )
                }
                ParseType.SET, ParseType.UPDATE, ParseType.REMOVE, ParseType.DELETE -> dml(
                    dmlOpList(toDmlOperation()),
                    metas = metas
                )
                ParseType.DML_LIST -> {
                    val dmlOps = children.flatMap { it.toDmlOperation() }
                    dml(
                        dmlOpList(dmlOps),
                        metas = metas
                    )
                }
                else -> error("Can't transform ParseType.$type to PartiqlAst.Statement.Dml }")
            }
        }
    }

    private fun ParseNode.toAstDdl(): PartiqlAst.Statement.Ddl {
        val metas = getMetas()

        return PartiqlAst.build {
            when (type) {
                ParseType.CREATE_TABLE -> ddl(
                    createTable(children[0].token!!.text!!),
                    metas
                )
                ParseType.DROP_TABLE -> ddl(
                    dropTable(children[0].toIdentifier()),
                    metas
                )
                ParseType.CREATE_INDEX -> ddl(
                    createIndex(
                        children[0].toIdentifier(),
                        children[1].children.map { it.toAstExpr() }
                    ),
                    metas
                )
                ParseType.DROP_INDEX -> ddl(
                    dropIndex(children[1].toIdentifier(), children[0].toIdentifier()),
                    metas
                )
                else -> error("Can't convert ParseType.$type to PartiqlAst.Statement.Ddl")
            }
        }
    }

    private fun ParseNode.toAstExec(): PartiqlAst.Statement.Exec {
        val metas = getMetas()

        return PartiqlAst.build {
            when (type) {
                ParseType.EXEC -> exec_(
                    SymbolPrimitive(token?.text!!.toLowerCase(), emptyMetaContainer()),
                    children.map { it.toAstExpr() },
                    metas
                )
                else -> error("Can't convert ParseType.$type to PartiqlAst.Statement.Exec")
            }
        }
    }

    private fun ParseNode.toAstType(): PartiqlAst.Type {
        if (type != ParseType.TYPE) {
            errMalformedParseTree("Expected ParseType.TYPE instead of $type")
        }

        val typeName = token?.keywordText ?: token?.customKeywordText
        val sqlDataType = SqlDataType.forTypeName(typeName!!)
            ?: (token?.customType ?: errMalformedParseTree("Invalid DataType: $typeName"))
        val metas = getMetas()
        val args = children.map {
            val argValue = it.token?.value
                ?: errMalformedParseTree("Data type argument did not have a token for some reason")

            if (argValue !is IonInt) {
                errMalformedParseTree("Data type argument was not an Ion INT for some reason")
            }

            when (argValue.integerSize!!) {
                IntegerSize.INT -> argValue.longValue()
                IntegerSize.LONG, IntegerSize.BIG_INTEGER ->
                    it.token.err(
                        "Type parameter exceeded maximum value",
                        ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE
                    )
            }
        }

        return PartiqlAst.build {
            when (sqlDataType) {
                SqlDataType.MISSING -> missingType(metas)
                SqlDataType.NULL -> nullType(metas)
                SqlDataType.BOOLEAN -> scalarType(BuiltInScalarTypeId.BOOLEAN, metas = metas)
                SqlDataType.SMALLINT -> scalarType(BuiltInScalarTypeId.SMALLINT, metas = metas)
                SqlDataType.INTEGER4 -> scalarType(BuiltInScalarTypeId.INTEGER4, metas = metas)
                SqlDataType.INTEGER8 -> scalarType(BuiltInScalarTypeId.INTEGER8, metas = metas)
                SqlDataType.INTEGER -> scalarType(BuiltInScalarTypeId.INTEGER, metas = metas)
                SqlDataType.FLOAT -> scalarType(BuiltInScalarTypeId.FLOAT, args, metas = metas)
                SqlDataType.REAL -> scalarType(BuiltInScalarTypeId.REAL, metas = metas)
                SqlDataType.DOUBLE_PRECISION -> scalarType(BuiltInScalarTypeId.DOUBLE_PRECISION, metas = metas)
                SqlDataType.DECIMAL -> scalarType(BuiltInScalarTypeId.DECIMAL, args, metas)
                SqlDataType.NUMERIC -> scalarType(BuiltInScalarTypeId.NUMERIC, args, metas)
                SqlDataType.TIMESTAMP -> scalarType(BuiltInScalarTypeId.TIMESTAMP, metas = metas)
                SqlDataType.CHARACTER -> scalarType(BuiltInScalarTypeId.CHARACTER, args, metas)
                SqlDataType.CHARACTER_VARYING -> scalarType(BuiltInScalarTypeId.CHARACTER_VARYING, args, metas)
                SqlDataType.STRING -> scalarType(BuiltInScalarTypeId.STRING, metas = metas)
                SqlDataType.SYMBOL -> scalarType(BuiltInScalarTypeId.SYMBOL, metas = metas)
                SqlDataType.CLOB -> scalarType(BuiltInScalarTypeId.CLOB, metas = metas)
                SqlDataType.BLOB -> scalarType(BuiltInScalarTypeId.BLOB, metas = metas)
                SqlDataType.STRUCT -> structType(metas)
                SqlDataType.TUPLE -> tupleType(metas)
                SqlDataType.LIST -> listType(metas)
                SqlDataType.SEXP -> sexpType(metas)
                SqlDataType.BAG -> bagType(metas)
                SqlDataType.DATE -> scalarType(BuiltInScalarTypeId.DATE, metas = metas)
                SqlDataType.TIME -> scalarType(BuiltInScalarTypeId.TIME, args, metas)
                SqlDataType.TIME_WITH_TIME_ZONE -> scalarType(BuiltInScalarTypeId.TIME_WITH_TIME_ZONE, args, metas)
                SqlDataType.ANY -> anyType(metas)
                is SqlDataType.CustomDataType -> customType(typeName, metas)
            }
        }
    }

    private fun String.toOperator(args: List<PartiqlAst.Expr>, metas: IonElementMetaContainer): PartiqlAst.Expr {
        return PartiqlAst.build {
            when (this@toOperator) {
                "+" -> when (args.size) {
                    0 -> throw IllegalArgumentException("Operator 'Add' must have at least one argument")
                    1 -> pos(args.first(), metas)
                    else -> plus(args, metas)
                }
                "-" -> when (args.size) {
                    0 -> throw IllegalArgumentException("Operator 'Sub' must have at least one argument")
                    1 -> neg(args.first(), metas)
                    else -> minus(args, metas)
                }
                "*" -> times(args, metas)
                "/" -> divide(args, metas)
                "%" -> modulo(args, metas)
                "=" -> eq(args, metas)
                "<" -> lt(args, metas)
                "<=" -> lte(args, metas)
                ">" -> gt(args, metas)
                ">=" -> gte(args, metas)
                "<>" -> ne(args, metas)
                "like" -> like(args[0], args[1], args.getOrNull(2), metas)
                "between" -> between(args[0], args[1], args[2], metas)
                "not" -> not(args[0], metas)
                "in" -> inCollection(args, metas)
                "and" -> and(args, metas)
                "or" -> or(args, metas)
                "||" -> concat(args, metas)
                "call" -> {
                    val idArg = args.first() as? PartiqlAst.Expr.Id
                        ?: error("First argument of call should be a VariableReference")
                    // the above error message says "VariableReference" and not PartiqlAst.expr.id because it would
                    // have been converted from a VariableReference when [args] was being built.

                    // TODO:  we are losing case-sensitivity of the function name here.  Do we care?
                    call(idArg.name.text, args.drop(1), metas)
                }
                "union",
                "union_distinct" -> bagOp(union(), distinct(), args, metas)
                "union_all" -> bagOp(union(), all(), args, metas)
                "intersect",
                "intersect_distinct" -> bagOp(intersect(), distinct(), args, metas)
                "intersect_all" -> bagOp(intersect(), all(), args, metas)
                "except",
                "except_distinct" -> bagOp(except(), distinct(), args, metas)
                "except_all" -> bagOp(except(), all(), args, metas)
                "outer_union",
                "outer_union_distinct" -> bagOp(outerUnion(), distinct(), args, metas)
                "outer_union_all" -> bagOp(outerUnion(), all(), args, metas)
                "outer_intersect",
                "outer_intersect_distinct" -> bagOp(outerIntersect(), distinct(), args, metas)
                "outer_intersect_all" -> bagOp(outerIntersect(), all(), args, metas)
                "outer_except",
                "outer_except_distinct" -> bagOp(outerExcept(), distinct(), args, metas)
                "outer_except_all" -> bagOp(outerExcept(), all(), args, metas)
                else -> throw IllegalArgumentException("Unsupported operator: ${this@toOperator}")
            }
        }
    }

    private fun ParseNode.toFromSource(): PartiqlAst.FromSource {
        val metas = getMetas()

        return PartiqlAst.build {
            when (type) {
                ParseType.FROM_SOURCE_JOIN -> {
                    val isCrossJoin = token?.keywordText?.contains("cross") ?: false
                    if (!isCrossJoin && children.size != 3) {
                        errMalformedParseTree("Incorrect number of clauses provided to JOIN")
                    }
                    val jt = when (token?.keywordText) {
                        "inner_join", "join", "cross_join" -> inner()
                        "left_join", "left_cross_join" -> left()
                        "right_join", "right_cross_join" -> right()
                        "outer_join", "outer_cross_join" -> full()
                        else -> errMalformedParseTree("Unsupported syntax for ${this@toFromSource.type}")
                    }
                    join(
                        jt,
                        children[0].toFromSource(),
                        children[1].unwrapAliasesAndUnpivot(),
                        if (isCrossJoin) null else children[2].toAstExpr(),
                        if (isCrossJoin) metas + metaContainerOf(IsImplictJoinMeta.instance) else metas
                    )
                }
                ParseType.MATCH -> toGraphMatch()
                else -> unwrapAliasesAndUnpivot()
            }
        }
    }

    private fun ParseNode.toGraphMatch(): PartiqlAst.FromSource {
        val metas = getMetas()
        var selector: PartiqlAst.GraphMatchSelector? = null

        return PartiqlAst.build {
            val expr = children[0].toAstExpr()

            var rest = children.tail
            if (rest.head?.type == ParseType.MATCH_EXPR_SELECTOR) {
                val selectorNode = rest.head!!
                selector = when (selectorNode.token!!.text) {
                    "ANY" -> selectorAny()
                    "ANY_SHORTEST" -> selectorAnyShortest()
                    "ALL_SHORTEST" -> selectorAllShortest()
                    "ANY_K" -> {
                        val k = selectorNode.children[0].numberValue()
                        selectorAnyK(k.toLong())
                    }
                    "SHORTEST_K" -> {
                        val k = selectorNode.children[0].numberValue()
                        selectorShortestK(k.toLong())
                    }
                    "SHORTEST_K_GROUP" -> {
                        val k = selectorNode.children[0].numberValue()
                        selectorShortestKGroup(k.toLong())
                    }
                    else -> null
                }

                rest = rest.tail
            }

            val patterns = rest.map {
                if (it.type != ParseType.MATCH_EXPR) error("Invalid parse tree: expecting match expression in MATCH")
                it.toGraphMatchPattern()
            }

            val matchExpr = PartiqlAst.GraphMatchExpr(selector = selector, patterns = patterns, metas = metas)
            PartiqlAst.FromSource.GraphMatch(expr, matchExpr, metas)
        }
    }

    private fun ParseNode.toGraphMatchPattern(): PartiqlAst.GraphMatchPattern {
        val metas = getMetas()

        return PartiqlAst.build {
            var restrictor: PartiqlAst.GraphMatchRestrictor? = null
            var predicate: PartiqlAst.Expr? = null
            var variable: SymbolPrimitive? = null
            var quantifier: PartiqlAst.GraphMatchQuantifier? = null
            val parts = mutableListOf<PartiqlAst.GraphMatchPatternPart>()

            for (child in children) {
                when (child.type) {
                    ParseType.MATCH_EXPR -> {
                        val subPattern = PartiqlAst.GraphMatchPatternPart.Pattern(
                            pattern = child.toGraphMatchPattern(),
                            metas = child.getMetas()
                        )
                        parts.add(subPattern)
                    }
                    ParseType.MATCH_EXPR_NAME -> {
                        variable = SymbolPrimitive(child.children[0].token!!.text!!, child.getMetas())
                    }
                    ParseType.MATCH_EXPR_NODE -> parts.add(child.toGraphMatchNode())
                    ParseType.MATCH_EXPR_EDGE -> parts.add(child.toGraphMatchEdge())
                    ParseType.MATCH_EXPR_QUANTIFIER -> quantifier = child.toGraphMatchQuantifier(quantifier)
                    ParseType.MATCH_EXPR_RESTRICTOR -> {
                        restrictor = when (child.children[0].token!!.sourceText.toUpperCase()) {
                            "TRAIL" -> restrictorTrail()
                            "ACYCLIC" -> restrictorAcyclic()
                            "SIMPLE" -> restrictorSimple()
                            else -> error("Invalid parse tree: unexpected restrictor `${child.children[0].token!!.sourceText}` for MATCH pattern")
                        }
                    }
                    ParseType.MATCH_EXPR_SELECTOR -> {
                        error("Invalid parse tree: unexpected selector for MATCH pattern")
                    }
                    else -> {
                        if (predicate == null) {
                            predicate = child.toAstExpr()
                        } else {
                            error("Invalid parse tree: unexpected subexpression for MATCH pattern")
                        }
                    }
                }
            }

            PartiqlAst.GraphMatchPattern(
                restrictor = restrictor,
                prefilter = predicate,
                variable = variable,
                quantifier = quantifier,
                parts = parts,
                metas = metas
            )
        }
    }

    private fun ParseNode.toGraphMatchNode(): PartiqlAst.GraphMatchPatternPart.Node {
        val metas = getMetas()

        var name: SymbolPrimitive? = null
        val label = mutableListOf<SymbolPrimitive>()
        var predicate: PartiqlAst.Expr? = null

        for (child in children) {
            when (child.type) {
                ParseType.MATCH_EXPR_NAME -> {
                    if (name != null) error("Invalid parse tree: name encountered more than once in MATCH")
                    val token = child.children[0].token!!
                    val nameText = when (token.type) {
                        TokenType.KEYWORD -> token.sourceText
                        else -> token.text!!
                    }
                    name = SymbolPrimitive(nameText, child.getMetas())
                }
                ParseType.MATCH_EXPR_LABEL -> {
                    val token = child.children[0].token!!
                    val labelText = when (token.type) {
                        TokenType.KEYWORD -> token.sourceText
                        else -> token.text!!
                    }
                    label.add(SymbolPrimitive(labelText, child.getMetas()))
                }
                else -> {
                    if (predicate == null) {
                        predicate = child.toAstExpr()
                    } else {
                        error("Invalid parse tree: unexpected subexpression for MATCH node")
                    }
                }
            }
        }

        return PartiqlAst.build {
            PartiqlAst.GraphMatchPatternPart.Node(
                variable = name,
                label = label,
                prefilter = predicate,
                metas = metas
            )
        }
    }

    private fun ParseNode.toGraphMatchQuantifier(previous: PartiqlAst.GraphMatchQuantifier?): PartiqlAst.GraphMatchQuantifier {
        val metas = getMetas()

        return when (token!!.type) {
            TokenType.STAR -> {
                PartiqlAst.GraphMatchQuantifier(
                    lower = LongPrimitive(0, metas),
                    upper = null
                )
            }
            TokenType.OPERATOR -> {
                when (token.keywordText) {
                    "+" -> {
                        PartiqlAst.GraphMatchQuantifier(
                            lower = LongPrimitive(1, metas),
                            upper = null
                        )
                    }
                    else -> {
                        error("Invalid parse tree: unexpected subexpression for MATCH quantifier")
                    }
                }
            }
            TokenType.LITERAL -> {
                val q = LongPrimitive(token.value!!.asIonInt().longValue(), metas)
                if (previous == null) {
                    PartiqlAst.GraphMatchQuantifier(lower = q, upper = null)
                } else {
                    PartiqlAst.GraphMatchQuantifier(lower = previous.lower, upper = q)
                }
            }
            else -> {
                error("Invalid parse tree: unexpected subexpression for MATCH quantifier")
            }
        }
    }

    private fun ParseNode.toGraphMatchEdge(): PartiqlAst.GraphMatchPatternPart.Edge {
        val metas = getMetas()

        var direction: PartiqlAst.GraphMatchDirection? = null
        var quantifier: PartiqlAst.GraphMatchQuantifier? = null
        var name: SymbolPrimitive? = null
        val label = mutableListOf<SymbolPrimitive>()
        var predicate: PartiqlAst.Expr? = null

        for (child in children) {
            when (child.type) {
                ParseType.MATCH_EXPR_NAME -> {
                    if (name != null) error("Invalid parse tree: name encountered more than once in MATCH")
                    val token = child.children[0].token!!
                    val nameText = when (token.type) {
                        TokenType.KEYWORD -> token.sourceText
                        else -> token.text!!
                    }
                    name = SymbolPrimitive(nameText, child.getMetas())
                }
                ParseType.MATCH_EXPR_LABEL -> {
                    val token = child.children[0].token!!
                    val labelText = when (token.type) {
                        TokenType.KEYWORD -> token.sourceText
                        else -> token.text!!
                    }
                    label.add(SymbolPrimitive(labelText, child.getMetas()))
                }
                ParseType.MATCH_EXPR_EDGE_DIRECTION -> {
                    direction = when (child.token!!.text!!) {
                        "<-" -> PartiqlAst.GraphMatchDirection.EdgeLeft()
                        "~" -> PartiqlAst.GraphMatchDirection.EdgeUndirected()
                        "->" -> PartiqlAst.GraphMatchDirection.EdgeRight()
                        "<~" -> PartiqlAst.GraphMatchDirection.EdgeLeftOrUndirected()
                        "~>" -> PartiqlAst.GraphMatchDirection.EdgeUndirectedOrRight()
                        "<->" -> PartiqlAst.GraphMatchDirection.EdgeLeftOrRight()
                        "-" -> PartiqlAst.GraphMatchDirection.EdgeLeftOrUndirectedOrRight()
                        else -> error("Invalid parse tree: unknown edge direction ${child.token.text!!}")
                    }
                }
                ParseType.MATCH_EXPR_QUANTIFIER -> quantifier = child.toGraphMatchQuantifier(quantifier)
                else -> {
                    if (predicate == null) {
                        predicate = child.toAstExpr()
                    } else {
                        error("Invalid parse tree: unexpected subexpression for MATCH edge")
                    }
                }
            }
        }

        if (direction == null) {
            error("Invalid parse tree: null edge direction")
        }

        return PartiqlAst.build {
            PartiqlAst.GraphMatchPatternPart.Edge(
                direction = direction,
                quantifier = quantifier,
                variable = name,
                label = label,
                prefilter = predicate,
                metas = metas
            )
        }
    }

    private fun ParseNode.unwrapAliasesAndUnpivot(): PartiqlAst.FromSource {
        val (aliases, unwrappedParseNode) = unwrapAliases()

        return PartiqlAst.build {
            when (unwrappedParseNode.type) {
                ParseType.UNPIVOT -> unpivot_(
                    unwrappedParseNode.children[0].toAstExpr(),
                    aliases.asName,
                    aliases.atName,
                    aliases.byName,
                    unwrappedParseNode.getMetas()
                )
                else -> {
                    val expr = unwrappedParseNode.toAstExpr()
                    scan_(expr, aliases.asName, aliases.atName, aliases.byName, expr.metas)
                }
            }
        }
    }

    private fun ParseNode.unwrapAliases(
        variables: LetVariables = LetVariables()
    ): Pair<LetVariables, ParseNode> {
        val metas = getMetas()

        return when (type) {
            ParseType.AS_ALIAS -> {
                if (variables.asName != null) error("Invalid parse tree: AS_ALIAS encountered more than once in FROM source")
                children[0].unwrapAliases(variables.copy(asName = SymbolPrimitive(token!!.text!!, metas)))
            }
            ParseType.AT_ALIAS -> {
                if (variables.atName != null) error("Invalid parse tree: AT_ALIAS encountered more than once in FROM source")
                children[0].unwrapAliases(variables.copy(atName = SymbolPrimitive(token!!.text!!, metas)))
            }
            ParseType.BY_ALIAS -> {
                if (variables.byName != null) error("Invalid parse tree: BY_ALIAS encountered more than once in FROM source")
                children[0].unwrapAliases(variables.copy(byName = SymbolPrimitive(token!!.text!!, metas)))
            }
            else -> Pair(variables, this)
        }
    }

    private fun ParseNode.toReturningExpr(): PartiqlAst.ReturningExpr =
        PartiqlAst.build {
            returningExpr(
                children[0].children.map {
                    returningElem(
                        it.children[0].toReturningMapping(),
                        it.children[1].toColumnComponent(it.getMetas())
                    )
                }
            )
        }

    private fun ParseNode.toReturningMapping(): PartiqlAst.ReturningMapping {
        if (type != ParseType.RETURNING_MAPPING) {
            errMalformedParseTree("Expected ParseType.RETURNING_MAPPING instead of $type")
        }
        return PartiqlAst.build {
            when (token?.keywordText) {
                "modified_old" -> modifiedOld()
                "modified_new" -> modifiedNew()
                "all_old" -> allOld()
                "all_new" -> allNew()
                else -> errMalformedParseTree("Invalid ReturningMapping parsing")
            }
        }
    }

    private fun ParseNode.toInsertReturning(): InsertReturning =
        when (type) {
            ParseType.INSERT -> {
                val ops = listOf(PartiqlAst.DmlOp.Insert(children[0].toAstExpr(), children[1].toAstExpr(), this.getMetas()))
                // We will remove items from this collection as we consume them.
                // If any unconsumed children remain, we've missed something and should throw an exception.
                val unconsumedChildren = children.drop(2).toMutableList()
                val returning = unconsumedChildren.firstOrNull { it.type == ParseType.RETURNING }?.let {
                    unconsumedChildren.remove(it)
                    it.toReturningExpr()
                }

                // Throw an exception if any unconsumed children remain
                malformedIfNotEmpty(unconsumedChildren)

                InsertReturning(ops, returning)
            }
            ParseType.INSERT_VALUE -> {
                fun getOnConflict(onConflictChildren: List<ParseNode>): PartiqlAst.OnConflict {
                    onConflictChildren.getOrNull(0)?.let { firstNode ->
                        val condition = firstNode.toAstExpr()
                        onConflictChildren.getOrNull(1)?.let { secondNode ->
                            if (ParseType.CONFLICT_ACTION == secondNode.type && "do_nothing" == secondNode.token?.keywordText) {
                                return PartiqlAst.build { onConflict(condition, doNothing()) }
                            }
                        }
                    }
                    errMalformedParseTree("invalid ON CONFLICT syntax")
                }

                val lvalue = children[0].toAstExpr()
                val value = children[1].toAstExpr()

                // We will remove items from this collection as we consume them.
                // If any unconsumed children remain, we've missed something and should throw an exception.
                val unconsumedChildren = children.drop(2).toMutableList()

                // Handle AT clause
                val position = unconsumedChildren.firstOrNull {
                    it.type != ParseType.ON_CONFLICT &&
                        it.type != ParseType.RETURNING
                }?.let {
                    unconsumedChildren.remove(it)
                    it.toAstExpr()
                }

                val onConflict = unconsumedChildren.firstOrNull { it.type == ParseType.ON_CONFLICT }?.let {
                    unconsumedChildren.remove(it)
                    getOnConflict(it.children)
                }

                val ops = listOf(PartiqlAst.build { insertValue(lvalue, value, position, onConflict) })
                val returning = unconsumedChildren.firstOrNull { it.type == ParseType.RETURNING }?.let {
                    unconsumedChildren.remove(it)
                    it.toReturningExpr()
                }

                // Throw an exception if any unconsumed children remain
                malformedIfNotEmpty(unconsumedChildren)

                InsertReturning(ops, returning)
            }
            else -> unsupported("Unsupported syntax for $type", ErrorCode.PARSE_UNSUPPORTED_SYNTAX)
        }

    private fun ParseNode.toColumnComponent(metas: IonElementMetaContainer): PartiqlAst.ColumnComponent =
        PartiqlAst.build {
            when (type) {
                ParseType.RETURNING_WILDCARD -> returningWildcard(metas)
                else -> returningColumn(this@toColumnComponent.toAstExpr())
            }
        }

    private fun ParseNode.toDmlOperation(): List<PartiqlAst.DmlOp> =
        when (type) {
            ParseType.INSERT -> {
                listOf(PartiqlAst.build { insert(children[0].toAstExpr(), children[1].toAstExpr()) })
            }
            ParseType.INSERT_VALUE -> {
                val lvalue = children[0].toAstExpr()
                val value = children[1].toAstExpr()

                // We will remove items from this collection as we consume them.
                // If any unconsumed children remain, we've missed something and should throw an exception.
                val unconsumedChildren = children.drop(2).toMutableList()

                // Handle AT clause
                val position = unconsumedChildren.firstOrNull {
                    it.type != ParseType.ON_CONFLICT &&
                        it.type != ParseType.RETURNING
                }?.let {
                    unconsumedChildren.remove(it)
                    it.toAstExpr()
                }

                val onConflict = unconsumedChildren.firstOrNull { it.type == ParseType.ON_CONFLICT }?.let {
                    unconsumedChildren.remove(it)
                    val onConflictChildren = it.children
                    onConflictChildren.getOrNull(0)?.let {
                        val condition = it.toAstExpr()
                        onConflictChildren.getOrNull(1)?.let {
                            if (ParseType.CONFLICT_ACTION == it.type && "do_nothing" == it.token?.keywordText) {
                                PartiqlAst.build { onConflict(condition, doNothing()) }
                            }
                        }
                    }
                    errMalformedParseTree("invalid ON CONFLICT syntax")
                }

                // Throw an exception if any unconsumed children remain
                malformedIfNotEmpty(unconsumedChildren)

                listOf(PartiqlAst.build { insertValue(lvalue, value, position, onConflict) })
            }
            ParseType.SET, ParseType.UPDATE -> children.map {
                PartiqlAst.build {
                    set(
                        assignment(
                            it.children[0].toAstExpr(),
                            it.children[1].toAstExpr(),
                            it.getMetas()
                        ),
                        this@toDmlOperation.getMetas()
                    )
                }
            }
            ParseType.REMOVE -> listOf(PartiqlAst.build { remove(children[0].toAstExpr(), this@toDmlOperation.getMetas()) })
            ParseType.DELETE -> listOf(PartiqlAst.build { delete(this@toDmlOperation.getMetas()) })
            else -> unsupported("Unsupported syntax for $type", ErrorCode.PARSE_UNSUPPORTED_SYNTAX)
        }

    private fun ParseNode.unwrapAsAlias(): AsAlias =
        if (type == ParseType.AS_ALIAS) {
            AsAlias(SymbolPrimitive(token!!.text!!, getMetas()), children[0])
        } else {
            AsAlias(null, this)
        }

    private fun ParseNode.toIdentifier(): PartiqlAst.Identifier {
        if (type != ParseType.ATOM) {
            errMalformedParseTree("Cannot transform ParseNode type: $type to identifier")
        }

        val metas = getMetas()

        return PartiqlAst.build {
            when (token?.type) {
                TokenType.QUOTED_IDENTIFIER -> identifier(token.text!!, caseSensitive(), metas)
                TokenType.IDENTIFIER -> identifier(token.text!!, caseInsensitive(), metas)
                else -> errMalformedParseTree("Cannot transform atom token type ${token?.type} to identifier")
            }
        }
    }

    private fun ParseNode.toOrderingSpec(): PartiqlAst.OrderingSpec {
        if (type != ParseType.ORDERING_SPEC) {
            errMalformedParseTree("Expected ParseType.ORDERING_SPEC instead of $type")
        }
        return PartiqlAst.build {
            when (token?.type) {
                TokenType.ASC -> asc()
                TokenType.DESC -> desc()
                else -> errMalformedParseTree("Invalid ordering spec parsing")
            }
        }
    }

    private fun ParseNode.toNullsSpec(): PartiqlAst.NullsSpec {
        if (type != ParseType.NULLS_SPEC) {
            errMalformedParseTree("Expected ParseType.NULLS_SPEC instead of $type")
        }
        return PartiqlAst.build {
            when (token?.type) {
                TokenType.FIRST -> nullsFirst()
                TokenType.LAST -> nullsLast()
                else -> errMalformedParseTree("Invalid nulls spec parsing")
            }
        }
    }

    private fun ParseNode.toSymbolicName(): SymbolPrimitive {
        if (token == null) {
            errMalformedParseTree("Expected ParseNode to have a token")
        }
        when (token.type) {
            TokenType.LITERAL, TokenType.ION_LITERAL, TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER -> {
                val tokenText = token.text ?: errMalformedParseTree("Expected ParseNode.token to have text")
                return SymbolPrimitive(tokenText, getMetas())
            }
            else -> errMalformedParseTree("TokenType.${token.type} cannot be converted to a SymbolicPrimitive")
        }
    }

    private fun ParseNode.toLetSource(): PartiqlAst.Let {
        return PartiqlAst.build {
            let(children.map { it.toLetBinding() })
        }
    }

    private fun ParseNode.toLetBinding(): PartiqlAst.LetBinding {
        val (asAliasSymbol, parseNode) = unwrapAsAlias()
        if (asAliasSymbol == null) {
            errMalformedParseTree("Unsupported syntax for $type")
        }
        return PartiqlAst.build {
            letBinding_(parseNode.toAstExpr(), asAliasSymbol, asAliasSymbol.metas)
        }
    }

    private val Token.customKeywordText: String?
        get() = when (type) {
            TokenType.IDENTIFIER -> when (text?.toLowerCase()) {
                in CUSTOM_KEYWORDS -> text?.toLowerCase()
                in CUSTOM_TYPE_ALIASES.keys -> CUSTOM_TYPE_ALIASES[text?.toLowerCase()]
                else -> null
            }
            else -> null
        }

    private val Token.customType: SqlDataType?
        get() = when (type) {
            TokenType.IDENTIFIER -> when (text?.toLowerCase()) {
                in CUSTOM_KEYWORDS -> SqlDataType.CustomDataType(text!!.toLowerCase())
                in CUSTOM_TYPE_ALIASES.keys -> CUSTOM_TYPE_ALIASES[text?.toLowerCase()]?.let {
                    SqlDataType.CustomDataType(it.toLowerCase())
                }
                else -> null
            }
            else -> null
        }

    private fun ParseNode.getMetas(): IonElementMetaContainer =
        token.toSourceLocationMetaContainer()

    private data class LetVariables(
        val asName: SymbolPrimitive? = null,
        val atName: SymbolPrimitive? = null,
        val byName: SymbolPrimitive? = null
    )

    private data class InsertReturning(
        val ops: List<PartiqlAst.DmlOp>,
        val returning: PartiqlAst.ReturningExpr? = null
    )

    private data class AsAlias(
        val name: SymbolPrimitive?,
        val node: ParseNode
    )

    /**********************************************************************************************
     * Parse logic below this line.
     **********************************************************************************************/

    // keywords that IN (<expr>) evaluate more like grouping than a singleton in value list
    private val IN_OP_NORMAL_EVAL_KEYWORDS = setOf("select", "values")

    /**
     * Parses the given token list.
     *
     * Throws [InterruptedException] if [Thread.interrupted] is set. This is the best place to do
     * that for the parser because this is the main function called to parse an expression and so
     * is called quite frequently during parsing by many parts of the parser.
     *
     * @param precedence The precedence of the current expression parsing.
     *                   A negative value represents the "top-level" parsing.
     *
     * @return The parse tree for the given expression.
     */
    internal fun List<Token>.parseExpression(precedence: Int = -1): ParseNode {
        checkThreadInterrupted()
        var expr = parseUnaryTerm()
        var rem = expr.remaining

        fun headPrecedence() = rem.head?.infixPrecedence ?: 0

        // XXX this is a Pratt Top-Down Operator Precedence implementation
        while (rem.isNotEmpty() && precedence < headPrecedence()) {
            val op = rem.head!!
            if (!op.isBinaryOperator && op.keywordText !in SPECIAL_INFIX_OPERATORS) {
                // unrecognized operator
                break
            }

            fun parseRightExpr() = if (rem.size < 3) {
                rem.err(
                    "Missing right-hand side expression of infix operator",
                    ErrorCode.PARSE_EXPECTED_EXPRESSION
                )
            } else {
                rem.tail.parseExpression(
                    precedence = op.infixPrecedence
                )
            }

            val right = when (op.keywordText) {
                // IS/IS NOT requires a type
                "is", "is_not" -> rem.tail.parseType(op.keywordText!!)
                // IN has context sensitive parsing rules around parenthesis
                "in", "not_in" -> when {
                    rem.tail.head?.type == TokenType.LEFT_PAREN &&
                        rem.tail.tail.head?.keywordText !in IN_OP_NORMAL_EVAL_KEYWORDS ->
                        rem.tail.tail.parseArgList(
                            aliasSupportType = AliasSupportType.NONE,
                            mode = ArgListMode.NORMAL_ARG_LIST
                        ).deriveExpected(TokenType.RIGHT_PAREN).copy(ParseType.LIST)
                    else -> parseRightExpr()
                }
                else -> parseRightExpr()
            }
            rem = right.remaining

            expr = when {
                op.isBinaryOperator -> ParseNode(ParseType.BINARY, op, listOf(expr, right), rem)
                else -> when (op.keywordText) {
                    "between", "not_between" -> {
                        val rest = rem.tailExpectedKeyword("and")
                        if (rest.onlyEndOfStatement()) {
                            rem.head.err("Expected expression after AND", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                        } else {
                            rem = rest
                            val third = rem.parseExpression(
                                precedence = op.infixPrecedence
                            )
                            rem = third.remaining
                            ParseNode(ParseType.TERNARY, op, listOf(expr, right, third), rem)
                        }
                    }
                    "like", "not_like" -> {
                        when {
                            rem.head?.keywordText == "escape" -> {
                                val rest = rem.tailExpectedKeyword("escape")
                                if (rest.onlyEndOfStatement()) {
                                    rem.head.err("Expected expression after ESCAPE", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                                } else {
                                    rem = rest
                                    val third = rem.parseExpression(precedence = op.infixPrecedence)
                                    rem = third.remaining
                                    ParseNode(ParseType.TERNARY, op, listOf(expr, right, third), rem)
                                }
                            }
                            else -> ParseNode(ParseType.BINARY, op, listOf(expr, right), rem)
                        }
                    }
                    else -> rem.err("Unknown infix operator", ErrorCode.PARSE_UNKNOWN_OPERATOR)
                }
            }
        }
        return expr
    }

    private fun List<Token>.parseUnaryTerm(): ParseNode {
        return when (head?.isUnaryOperator) {
            true -> {
                val op = head!!
                fun makeUnaryParseNode(term: ParseNode) =
                    ParseNode(ParseType.UNARY, op, listOf(term), term.remaining)

                // constant fold unary plus/minus into constant literals
                when (op.keywordText) {
                    "+" -> {
                        val term = tail.parseUnaryTerm()
                        when {
                            // unary plus is a no-op on numeric literals.
                            term.isNumericLiteral -> term
                            else -> makeUnaryParseNode(term)
                        }
                    }
                    "-" -> {
                        val term = tail.parseUnaryTerm()
                        when {
                            // for numbers, drop the minus sign but also negate the value
                            term.isNumericLiteral ->
                                term.copy(token = term.token!!.copy(value = (-term.numberValue()).ionValue(ion)))
                            else -> makeUnaryParseNode(term)
                        }
                    }
                    else -> makeUnaryParseNode(tail.parseExpression(op.prefixPrecedence))
                }
            }
            else -> parsePathTerm()
        }
    }

    private fun List<Token>.parsePathTerm(pathMode: PathMode = PathMode.FULL_PATH): ParseNode {
        val term = when (pathMode) {
            PathMode.FULL_PATH -> parseTerm()
            PathMode.SIMPLE_PATH -> when (head?.type) {
                TokenType.QUOTED_IDENTIFIER, TokenType.IDENTIFIER -> atomFromHead()
                else -> err("Expected identifier for simple path", ErrorCode.PARSE_INVALID_PATH_COMPONENT)
            }
        }
        val path = ArrayList<ParseNode>(listOf(term))
        var rem = term.remaining

        var hasPath = true
        while (hasPath) {
            when (rem.head?.type) {
                TokenType.DOT -> {
                    val dotToken = rem.head!!
                    // consume first dot
                    rem = rem.tail
                    val pathPart = when (rem.head?.type) {
                        TokenType.IDENTIFIER -> {
                            val litToken =
                                Token(
                                    TokenType.LITERAL,
                                    ion.newString(rem.head?.text!!),
                                    rem.head?.text!!,
                                    rem.head!!.span
                                )
                            ParseNode(ParseType.CASE_INSENSITIVE_ATOM, litToken, emptyList(), rem.tail)
                        }
                        TokenType.QUOTED_IDENTIFIER -> {
                            val litToken =
                                Token(
                                    TokenType.LITERAL,
                                    ion.newString(rem.head?.text!!),
                                    rem.head?.text!!,
                                    rem.head!!.span
                                )
                            ParseNode(ParseType.CASE_SENSITIVE_ATOM, litToken, emptyList(), rem.tail)
                        }
                        TokenType.STAR -> {
                            if (pathMode != PathMode.FULL_PATH) {
                                rem.err("Invalid path dot component for simple path", ErrorCode.PARSE_INVALID_PATH_COMPONENT)
                            }
                            ParseNode(ParseType.PATH_UNPIVOT, rem.head, emptyList(), rem.tail)
                        }
                        else -> {
                            rem.err("Invalid path dot component", ErrorCode.PARSE_INVALID_PATH_COMPONENT)
                        }
                    }
                    path.add(ParseNode(ParseType.PATH_DOT, dotToken, listOf(pathPart), rem))
                    rem = rem.tail
                }
                TokenType.LEFT_BRACKET -> {
                    val leftBracketToken = rem.head!!
                    rem = rem.tail
                    val expr = when (rem.head?.type) {
                        TokenType.STAR -> ParseNode(ParseType.PATH_WILDCARD, rem.head, emptyList(), rem.tail)
                        else -> rem.parseExpression()
                    }.deriveExpected(TokenType.RIGHT_BRACKET)
                    if (pathMode == PathMode.SIMPLE_PATH && expr.type != ParseType.ATOM && expr.token?.type != TokenType.LITERAL) {
                        rem.err("Invalid path component for simple path", ErrorCode.PARSE_INVALID_PATH_COMPONENT)
                    }

                    path.add(ParseNode(ParseType.PATH_SQB, leftBracketToken, listOf(expr), rem.tail))
                    rem = expr.remaining
                }
                else -> hasPath = false
            }
        }

        return when (path.size) {
            1 -> term
            else -> ParseNode(ParseType.PATH, null, path, rem)
        }
    }

    private fun List<Token>.parseTerm(): ParseNode = when (head?.type) {
        TokenType.OPERATOR -> when (head?.keywordText) {
            // the lexical scope operator is **only** allowed with identifiers
            "@" -> when (tail.head?.type) {
                TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER -> ParseNode(
                    ParseType.UNARY,
                    head,
                    listOf(tail.atomFromHead()),
                    tail.tail
                )
                else -> err("Identifier must follow @-operator", ErrorCode.PARSE_MISSING_IDENT_AFTER_AT)
            }
            else -> err("Unexpected operator", ErrorCode.PARSE_UNEXPECTED_OPERATOR)
        }

        TokenType.KEYWORD -> when (head?.keywordText) {
            in BASE_DML_KEYWORDS -> parseBaseDml()
            "update" -> tail.parseUpdate()
            "delete" -> tail.parseDelete(head!!)
            "case" -> when (tail.head?.keywordText) {
                "when" -> tail.parseCase(isSimple = false)
                else -> tail.parseCase(isSimple = true)
            }
            "cast", "can_cast", "can_lossless_cast" -> parseTypeFunction()
            "select" -> tail.parseSelect()
            "create" -> tail.parseCreate()
            "drop" -> tail.parseDrop()
            "pivot" -> tail.parsePivot()
            "from" -> tail.parseFrom()
            // table value constructor--which aliases to bag constructor in PartiQL with very
            // specific syntax
            "values" -> tail.parseTableValues().copy(type = ParseType.BAG)
            "substring" -> tail.parseSubstring(head!!)
            "trim" -> tail.parseTrim(head!!)
            "extract" -> tail.parseExtract(head!!)
            "date_add", "date_diff" -> tail.parseDateAddOrDateDiff(head!!)
            "date" -> tail.parseDate()
            "time" -> tail.parseTime()
            "nullif" -> tail.parseNullIf(head!!)
            "coalesce" -> tail.parseCoalesce()
            in FUNCTION_NAME_KEYWORDS -> when (tail.head?.type) {
                TokenType.LEFT_PAREN ->
                    tail.tail.parseFunctionCall(head!!)
                else -> err("Unexpected keyword", ErrorCode.PARSE_UNEXPECTED_KEYWORD)
            }
            "exec" -> tail.parseExec()
            else -> err("Unexpected keyword", ErrorCode.PARSE_UNEXPECTED_KEYWORD)
        }
        TokenType.LEFT_PAREN -> {
            val group = tail.parseArgList(
                aliasSupportType = AliasSupportType.NONE,
                mode = ArgListMode.NORMAL_ARG_LIST
            ).deriveExpected(TokenType.RIGHT_PAREN)
            when (group.children.size) {
                0 -> tail.err("Expression group cannot be empty", ErrorCode.PARSE_EXPECTED_EXPRESSION)
                // expression grouping
                1 -> group.children[0].copy(remaining = group.remaining)
                // row value constructor--which aliases to list constructor in PartiQL
                else -> group.copy(type = ParseType.LIST)
            }
        }
        TokenType.LEFT_BRACKET -> when (tail.head?.type) {
            TokenType.RIGHT_BRACKET -> ParseNode(ParseType.LIST, null, emptyList(), tail.tail)
            else -> tail.parseListLiteral()
        }
        TokenType.LEFT_DOUBLE_ANGLE_BRACKET -> when (tail.head?.type) {
            TokenType.RIGHT_DOUBLE_ANGLE_BRACKET -> ParseNode(ParseType.BAG, null, emptyList(), tail.tail)
            else -> tail.parseBagLiteral()
        }
        TokenType.LEFT_CURLY -> when (tail.head?.type) {
            TokenType.RIGHT_CURLY -> ParseNode(ParseType.STRUCT, null, emptyList(), tail.tail)
            else -> tail.parseStructLiteral()
        }
        TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER -> when (tail.head?.type) {
            TokenType.LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
            else -> atomFromHead()
        }
        TokenType.QUESTION_MARK -> ParseNode(ParseType.PARAMETER, head!!, listOf(), tail)
        TokenType.ION_LITERAL, TokenType.LITERAL, TokenType.NULL, TokenType.MISSING,
        TokenType.TRIM_SPECIFICATION -> atomFromHead()
        else -> err("Unexpected term", ErrorCode.PARSE_UNEXPECTED_TERM)
    }.let { parseNode ->
        // for many of the terms here we parse the tail, assuming the head as
        // context, but that loses the metas and other info from that token.
        // the below assumes that the head is in fact representative of the
        // resulting parse node.
        // TODO: validate and/or better guarantee the above assumption
        if (parseNode.token == null) {
            parseNode.copy(token = head)
        } else {
            parseNode
        }
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

        return ParseNode(ParseType.CASE, null, children, rem)
    }

    private fun List<Token>.parseCaseBody(): ParseNode {
        val children = ArrayList<ParseNode>()
        var rem = this
        while (rem.head?.keywordText == "when") {
            val conditionExpr = rem.tail.parseExpression().deriveExpectedKeyword("then")
            rem = conditionExpr.remaining

            val result = rem.parseExpression()
            rem = result.remaining

            children.add(ParseNode(ParseType.WHEN, null, listOf(conditionExpr, result), rem))
        }
        if (children.isEmpty()) {
            err("Expected a WHEN clause in CASE", ErrorCode.PARSE_EXPECTED_WHEN_CLAUSE)
        }
        if (rem.head?.keywordText == "else") {
            val elseExpr = rem.tail.parseExpression()
            rem = elseExpr.remaining

            children.add(ParseNode(ParseType.ELSE, null, listOf(elseExpr), rem))
        }

        return ParseNode(ParseType.ARG_LIST, null, children, rem)
            .deriveExpectedKeyword("end")
    }

    private fun List<Token>.parseTypeFunction(): ParseNode {
        val functionName = head?.keywordText!!
        var rem = tail
        if (rem.head?.type != TokenType.LEFT_PAREN) {
            rem.err("Missing left parenthesis after $functionName", ErrorCode.PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST)
        }
        val valueExpr = rem.tail.parseExpression().deriveExpected(TokenType.AS)
        rem = valueExpr.remaining

        val typeNode = rem.parseType(functionName).deriveExpected(TokenType.RIGHT_PAREN)
        rem = typeNode.remaining

        return ParseNode(ParseType.TYPE_FUNCTION, head, listOf(valueExpr, typeNode), rem)
    }

    private fun List<Token>.parseNullIf(nullIfToken: Token): ParseNode {
        if (head?.type != TokenType.LEFT_PAREN) {
            err("Missing left parenthesis after nullif", ErrorCode.PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR)
        }
        val expr1 = tail.parseExpression().deriveExpected(TokenType.COMMA)
        var rem = expr1.remaining

        val expr2 = rem.parseExpression().deriveExpected(TokenType.RIGHT_PAREN)
        rem = expr2.remaining

        return ParseNode(ParseType.NULLIF, nullIfToken, listOf(expr1, expr2), rem)
    }

    private fun List<Token>.parseCoalesce(): ParseNode {
        if (head?.type != TokenType.LEFT_PAREN) {
            err("Missing left parenthesis after coalesce", ErrorCode.PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR)
        }
        return tail.parseArgList(aliasSupportType = AliasSupportType.NONE, mode = ArgListMode.NORMAL_ARG_LIST)
            .copy(type = ParseType.COALESCE, token = head)
            .deriveExpected(TokenType.RIGHT_PAREN)
    }

    private fun List<Token>.parseType(opName: String): ParseNode {
        val typeName = head?.keywordText
        val typeArity = ALL_TYPE_NAME_ARITY_MAP[typeName]
            ?: (
                head?.customType?.arityRange
                    ?: err("Expected type name", ErrorCode.PARSE_EXPECTED_TYPE_NAME)
                )

        val typeNode = when (tail.head?.type) {
            TokenType.LEFT_PAREN -> tail.tail.parseArgList(
                aliasSupportType = AliasSupportType.NONE,
                mode = ArgListMode.NORMAL_ARG_LIST
            ).copy(
                type = ParseType.TYPE,
                token = head
            ).deriveExpected(TokenType.RIGHT_PAREN)

            else -> ParseNode(ParseType.TYPE, head, emptyList(), tail)
        }
            // Check for the optional "WITH TIME ZONE" specifier for TIME and validate the value of precision.
            // Note that this needs to be checked explicitly as the keywordtext for "TIME WITH TIME ZONE" consists of multiple words.
            .let {
                if (typeName == "time") {
                    // Check for the range of valid values for precision
                    it.children.firstOrNull()?.also { precision ->
                        if (precision.token?.value == null || !precision.token.value.isUnsignedInteger ||
                            precision.token.value.longValue() < 0 || precision.token.value.longValue() > MAX_PRECISION_FOR_TIME
                        ) {
                            precision.token.err(
                                "Expected integer value between 0 and 9 for precision",
                                ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME
                            )
                        }
                    }
                    val (remainingAfterOptionalTimeZone, isTimeZoneSpecified) = it.remaining.checkForOptionalTimeZone()
                    val newToken = if (isTimeZoneSpecified) {
                        it.token!!.copy(value = ion.singleValue(SqlDataType.TIME_WITH_TIME_ZONE.typeName))
                    } else {
                        it.token
                    }
                    it.copy(token = newToken, remaining = remainingAfterOptionalTimeZone)
                } else {
                    it
                }
            }

        if (typeNode.children.size !in typeArity) {
            val pvmap = PropertyValueMap()
            pvmap[Property.CAST_TO] = typeName ?: ""
            pvmap[Property.EXPECTED_ARITY_MIN] = typeArity.first
            pvmap[Property.EXPECTED_ARITY_MAX] = typeArity.last
            tail.err("$opName type argument $typeName must have arity of $typeArity", ErrorCode.PARSE_CAST_ARITY, pvmap)
        }
        for (child in typeNode.children) {
            if (child.type != ParseType.ATOM ||
                child.token?.type != TokenType.LITERAL ||
                child.token.value?.isUnsignedInteger != true
            ) {
                err("Type parameter must be an unsigned integer literal", ErrorCode.PARSE_INVALID_TYPE_PARAM)
            }
        }

        return typeNode
    }

    private fun List<Token>.parseFrom(): ParseNode {
        var rem = this
        val children = ArrayList<ParseNode>()
        val fromList = rem.parseFromSourceList()

        rem = fromList.remaining

        rem.parseOptionalWhere()?.let {
            children.add(it)
            rem = it.remaining
        }

        // TODO support ORDER BY, LIMIT and OFFSET (and full select sub-clauses)

        // TODO determine if DML l-value should be restricted to paths...
        // TODO support the FROM ... SELECT forms
        val operation = rem.parseBaseDmls()
        rem = operation.remaining

        rem.parseOptionalReturning()?.let { it ->
            children.add(it)
            rem = it.remaining
        }

        return ParseNode(ParseType.FROM, null, listOf(operation, fromList) + children, rem)
    }

    private fun List<Token>.parseBaseDmls(): ParseNode {
        var rem = this
        val nodes = ArrayList<ParseNode>()
        while (rem.head?.keywordText in BASE_DML_KEYWORDS) {
            var node = rem.parseBaseDml()
            nodes.add(node)
            rem = node.remaining
        }

        if (nodes.size == 0) {
            err("Expected data manipulation", ErrorCode.PARSE_MISSING_OPERATION)
        }

        if (nodes.size == 1) {
            return nodes[0]
        }

        return ParseNode(ParseType.DML_LIST, null, nodes, rem)
    }

    private fun List<Token>.parseBaseDml(): ParseNode {
        val keywordToken = this.firstOrNull()
        var rem = this
        return when (rem.head?.keywordText) {
            "insert_into" -> {
                val lvalue = rem.tail.parsePathTerm(PathMode.SIMPLE_PATH)
                rem = lvalue.remaining

                if ("value" == rem.head?.keywordText) {
                    val value = rem.tail.parseExpression()
                    rem = value.remaining

                    val position = when (rem.head?.keywordText) {
                        "at" -> rem.tail.parseExpression().also { rem = it.remaining }
                        else -> null
                    }
                    val onConflict = rem.parseOptionalOnConflict()?.also { rem = it.remaining }

                    val returning = rem.parseOptionalReturning()?.also { rem = it.remaining }

                    ParseNode(ParseType.INSERT_VALUE, keywordToken, listOfNotNull(lvalue, value, position, onConflict, returning), rem)
                } else {
                    val values = rem.parseExpression()
                    ParseNode(ParseType.INSERT, keywordToken, listOf(lvalue, values), values.remaining)
                }
            }
            "set" -> rem.tail.parseSetAssignments(ParseType.UPDATE).copy(token = keywordToken)
            "remove" -> {
                val lvalue = rem.tail.parsePathTerm(PathMode.SIMPLE_PATH)
                rem = lvalue.remaining
                ParseNode(ParseType.REMOVE, keywordToken, listOf(lvalue), rem)
            }
            else -> err("Expected data manipulation", ErrorCode.PARSE_MISSING_OPERATION)
        }
    }

    private fun List<Token>.parseConflictAction(token: Token): ParseNode {
        val rem = this
        return ParseNode(ParseType.CONFLICT_ACTION, token, emptyList(), rem.tail)
    }

    // Parse the optional ON CONFLICT clause in 'INSERT VALUE <expr> AT <expr> ON CONFLICT WHERE <expr> <conflict action>'
    private fun List<Token>.parseOptionalOnConflict(): ParseNode? {
        val remaining = this
        return if ("on_conflict" == remaining.head?.keywordText) {
            val rem = remaining.tail
            when (rem.head?.keywordText) {
                "where" -> {
                    val where_rem = rem.tail
                    val onConflictExpression = where_rem.parseExpression()
                    val onConflictRem = onConflictExpression.remaining
                    when (onConflictRem.head?.keywordText) {
                        "do_nothing" -> {
                            val conflictAction = onConflictRem.parseConflictAction(onConflictRem.head!!)
                            var nodes = listOfNotNull(onConflictExpression, conflictAction)
                            ParseNode(ParseType.ON_CONFLICT, null, nodes, conflictAction.remaining)
                        }
                        else -> rem.head.err("invalid ON CONFLICT syntax", ErrorCode.PARSE_EXPECTED_CONFLICT_ACTION)
                    }
                }
                else -> rem.head.err("invalid ON CONFLICT syntax", ErrorCode.PARSE_EXPECTED_WHERE_CLAUSE)
            }
        } else null
    }

    private fun List<Token>.parseSetAssignments(type: ParseType): ParseNode = parseArgList(
        aliasSupportType = AliasSupportType.NONE,
        mode = ArgListMode.SET_CLAUSE_ARG_LIST
    ).run {
        if (children.isEmpty()) {
            remaining.err("Expected assignment for SET", ErrorCode.PARSE_MISSING_SET_ASSIGNMENT)
        }
        copy(type = type)
    }

    private fun List<Token>.parseDelete(name: Token): ParseNode {
        if (head?.keywordText != "from") {
            err("Expected FROM after DELETE", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }

        return tail.parseLegacyDml { ParseNode(ParseType.DELETE, name, emptyList(), this) }
    }

    private fun List<Token>.parseUpdate(): ParseNode = parseLegacyDml {
        parseBaseDmls()
    }

    private fun List<Token>.parseReturning(): ParseNode {
        var rem = this
        val returningElems = listOf(rem.parseReturningElems())
        rem = returningElems.first().remaining
        return ParseNode(type = ParseType.RETURNING, token = null, children = returningElems, remaining = rem)
    }

    private inline fun List<Token>.parseLegacyDml(parseDmlOp: List<Token>.() -> ParseNode): ParseNode {
        var rem = this
        val children = ArrayList<ParseNode>()

        val source = rem.parsePathTerm(PathMode.SIMPLE_PATH).let {
            it.remaining.parseOptionalAsAlias(it).also { asNode ->
                rem = asNode.remaining
            }
        }.let {
            it.remaining.parseOptionalAtAlias(it).also { atNode ->
                rem = atNode.remaining
            }
        }.let {
            it.remaining.parseOptionalByAlias(it).also { byNode ->
                rem = byNode.remaining
            }
        }

        children.add(ParseNode(ParseType.FROM_CLAUSE, null, listOf(source), rem))

        val operation = rem.parseDmlOp().also {
            rem = it.remaining
        }

        rem.parseOptionalWhere()?.let {
            children.add(it)
            rem = it.remaining
        }

        rem.parseOptionalReturning()?.let { it ->
            children.add(it)
            rem = it.remaining
        }

        // generate a FROM-node to normalize the parse tree
        return ParseNode(ParseType.FROM, null, listOf(operation) + children, rem)
    }

    private fun List<Token>.parseOptionalWhere(): ParseNode? {
        var rem = this

        // TODO consolidate this logic with the SELECT logic
        if (rem.head?.keywordText == "where") {
            val expr = rem.tail.parseExpression()
            rem = expr.remaining
            return ParseNode(ParseType.WHERE, null, listOf(expr), rem)
        }

        return null
    }

    private fun List<Token>.parseOptionalReturning(): ParseNode? {
        var rem = this

        if (rem.head?.keywordText == "returning") {
            return rem.tail.parseReturning()
        }

        return null
    }

    private fun List<Token>.parseReturningElems(): ParseNode {
        return parseCommaList {
            var rem = this
            var returningMapping = rem.parseReturningMapping().also { rem = it.remaining }
            var column = rem.parseColumn().also { rem = it.remaining }
            ParseNode(type = ParseType.RETURNING_ELEM, token = null, children = listOf(returningMapping, column), remaining = rem)
        }
    }

    private fun List<Token>.parseReturningMapping(): ParseNode {
        var rem = this
        when (rem.head?.keywordText) {
            "modified_old", "modified_new", "all_old", "all_new" -> {
                return ParseNode(
                    type = ParseType.RETURNING_MAPPING, token = rem.head, children = listOf(),
                    remaining = rem.tail
                )
            }
            else -> rem.err(
                "Expected ( MODIFIED | ALL ) ( NEW | OLD ) in each returning element.",
                ErrorCode.PARSE_EXPECTED_RETURNING_CLAUSE
            )
        }
    }

    private fun List<Token>.parseColumn(): ParseNode {
        return when (this.head?.type) {
            TokenType.STAR -> ParseNode(ParseType.RETURNING_WILDCARD, this.head, listOf(), this.tail)
            else -> {
                var expr = parseExpression().let {
                    when (it.type) {
                        ParseType.PATH -> inspectColumnPathExpression(it)
                        ParseType.ATOM -> it
                        else -> this.err(
                            "Unsupported syntax in RETURNING columns.",
                            ErrorCode.PARSE_UNSUPPORTED_RETURNING_CLAUSE_SYNTAX
                        )
                    }
                }
                expr
            }
        }
    }

    private fun inspectColumnPathExpression(pathNode: ParseNode): ParseNode {
        if (pathNode.children.size > 2) {
            pathNode.children[2].token?.err(
                "More than two paths in RETURNING columns.",
                ErrorCode.PARSE_UNSUPPORTED_RETURNING_CLAUSE_SYNTAX
            )
        }
        return pathNode
    }

    private fun List<Token>.parsePivot(): ParseNode {
        var rem = this
        val value = rem.parseExpression().deriveExpectedKeyword("at")
        rem = value.remaining
        val name = rem.parseExpression()
        rem = name.remaining
        val selectAfterProjection = parseSelectAfterProjection(
            ParseType.PIVOT,
            ParseNode(ParseType.MEMBER, null, listOf(name, value), rem)
        )
        return selectAfterProjection
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

        var type = ParseType.SELECT_LIST
        var projection = when {
            rem.head?.keywordText == "value" -> {
                type = ParseType.SELECT_VALUE
                rem.tail.parseExpression()
            }
            else -> {
                val list = rem.parseSelectList()
                if (list.children.isEmpty()) {
                    rem.err("Cannot have empty SELECT list", ErrorCode.PARSE_EMPTY_SELECT)
                }

                val asterisk = list.children.firstOrNull { it.type == ParseType.PROJECT_ALL && it.children.isEmpty() }
                if (asterisk != null &&
                    list.children.size > 1
                ) {
                    asterisk.token.err(
                        "Other expressions may not be present in the select list when '*' is used without dot notation.",
                        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST
                    )
                }

                list
            }
        }
        if (distinct) {
            projection = ParseNode(ParseType.DISTINCT, null, listOf(projection), projection.remaining)
        }

        val parseSelectAfterProjection = parseSelectAfterProjection(type, projection)
        return parseSelectAfterProjection
    }

    private fun ParseNode.expectEof(statementType: String) {
        if (!remaining.onlyEndOfStatement()) {
            remaining.err("Unexpected tokens after $statementType statement!", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
    }

    private fun List<Token>.parseCreate(): ParseNode = when (head?.keywordText) {
        "table" -> tail.parseCreateTable()
        "index" -> tail.parseCreateIndex()
        else -> head.err("Unexpected token following CREATE", ErrorCode.PARSE_UNEXPECTED_TOKEN)
    }.apply {
        expectEof("CREATE")
    }

    private fun List<Token>.parseDrop(): ParseNode = when (head?.keywordText) {
        "table" -> tail.parseDropTable()
        "index" -> tail.parseDropIndex()
        else -> head.err("Unexpected token following DROP", ErrorCode.PARSE_UNEXPECTED_TOKEN)
    }.apply {
        expectEof("DROP")
    }

    /**
     * This is currently parsing only the most naïve `CREATE TABLE <name>` statement.
     *
     * TODO: provide for definition of table schema.
     */
    private fun List<Token>.parseCreateTable(): ParseNode {
        val identifier = when (head?.type) {
            TokenType.QUOTED_IDENTIFIER, TokenType.IDENTIFIER -> {
                atomFromHead()
            }
            else -> {
                err("Expected identifier!", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        return ParseNode(ParseType.CREATE_TABLE, null, listOf(identifier), identifier.remaining)
    }

    private fun List<Token>.parseDropIndex(): ParseNode {
        var rem = this

        val identifier = when (rem.head?.type) {
            TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER -> {
                atomFromHead()
            }
            else -> {
                rem.err("Expected identifier!", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        rem = rem.tail

        if (rem.head?.keywordText != "on") {
            rem.err("Expected ON", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
        rem = rem.tail

        val target = when (rem.head?.type) {
            TokenType.QUOTED_IDENTIFIER, TokenType.IDENTIFIER -> {
                rem.atomFromHead()
            }
            else -> {
                rem.err("Table target must be an identifier", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        rem = rem.tail

        return ParseNode(ParseType.DROP_INDEX, null, listOf(identifier, target), rem)
    }

    /**
     * This is currently parsing only the most naïve `DROP TABLE <name>` statement.
     */
    private fun List<Token>.parseDropTable(): ParseNode {
        val identifier = when (head?.type) {
            TokenType.QUOTED_IDENTIFIER, TokenType.IDENTIFIER -> {
                atomFromHead()
            }
            else -> {
                err("Expected identifier!", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }

        return ParseNode(ParseType.DROP_TABLE, null, listOf(identifier), identifier.remaining)
    }

    /**
     * Parses a basic `CREATE INDEX ON <name> <path>, ...`
     */
    private fun List<Token>.parseCreateIndex(): ParseNode {
        var rem = this

        // TODO support UNIQUE modifier
        // TODO support naming the index

        if (rem.head?.keywordText != "on") {
            err("Expected ON", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
        rem = rem.tail

        val target = when (rem.head?.type) {
            TokenType.QUOTED_IDENTIFIER, TokenType.IDENTIFIER -> {
                rem.atomFromHead()
            }
            else -> {
                rem.err("Index target must be an identifier", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        rem = target.remaining

        if (rem.head?.type != TokenType.LEFT_PAREN) {
            rem.err("Expected parenthesis for keys", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
        // TODO support full expressions here... only simple paths for now
        val keys = rem.tail.parseArgList(AliasSupportType.NONE, ArgListMode.SIMPLE_PATH_ARG_LIST)
            .deriveExpected(TokenType.RIGHT_PAREN)
        rem = keys.remaining

        // TODO support other syntax options
        return ParseNode(ParseType.CREATE_INDEX, null, listOf(target, keys), rem)
    }

    /**
     * Inspects a path expression to determine if should be treated as a regular [ParseType.PATH] expression or
     * converted to a [ParseType.PROJECT_ALL].
     *
     * Examples of expressions that are converted to [ParseType.PROJECT_ALL] are:
     *
     * ```sql
     *      SELECT * FROM foo
     *      SELECT foo.* FROM foo
     *      SELECT f.* FROM foo as f
     *      SELECT foo.bar.* FROM foo
     *      SELECT f.bar.* FROM foo as f
     * ```
     * Also validates that the expression is valid for select list context.  It does this by making
     * sure that expressions looking like the following do not appear:
     *
     * ```sql
     *      SELECT foo[*] FROM foo
     *      SELECT f.*.bar FROM foo as f
     *      SELECT foo[1].* FROM foo
     *      SELECT foo.*.bar FROM foo
     * ```
     *
     * If no conversion is needed, returns the original `pathNode`.
     * If conversion is needed, clones the original `pathNode`, changing the `type` to `PROJECT_ALL`,
     * removes the trailing `PATH_WILDCARD_UNPIVOT` and returns.
     */
    private fun inspectPathExpression(pathNode: ParseNode): ParseNode {
        fun flattenParseNode(node: ParseNode): List<ParseNode> {
            fun doFlatten(n: ParseNode, l: MutableList<ParseNode>) {
                l.add(n)
                n.children.forEach { doFlatten(it, l) }
            }
            val list = mutableListOf<ParseNode>()
            doFlatten(node, list)
            return list
        }

        val flattened = flattenParseNode(pathNode).drop(2)

        // Is invalid if contains PATH_WILDCARD (i.e. to `[*]`}
        flattened.firstOrNull { it.type == ParseType.PATH_WILDCARD }
            ?.token
            ?.err("Invalid use of * in select list", ErrorCode.PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST)

        // Is invalid if contains PATH_WILDCARD_UNPIVOT (i.e. * as part of a dotted expression) anywhere except at the end.
        // i.e. f.*.b is invalid but f.b.* is not.
        flattened.dropLast(1).firstOrNull { it.type == ParseType.PATH_UNPIVOT }
            ?.token
            ?.err("Invalid use of * in select list", ErrorCode.PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST)

        // If the last path component expression is a *, then the PathType is a wildcard and we need to do one
        // additional check.
        if (flattened.last().type == ParseType.PATH_UNPIVOT) {

            // Is invalid if contains a square bracket anywhere and a wildcard at the end.
            // i.e f[1].* is invalid
            flattened.firstOrNull { it.type == ParseType.PATH_SQB }
                ?.token
                ?.err("Cannot use [] and * together in SELECT list expression", ErrorCode.PARSE_CANNOT_MIX_SQB_AND_WILDCARD_IN_SELECT_LIST)

            val pathPart = pathNode.copy(children = pathNode.children.dropLast(1))

            return ParseNode(
                type = ParseType.PROJECT_ALL,
                token = null,
                children = listOf(if (pathPart.children.size == 1) pathPart.children[0] else pathPart),
                remaining = pathNode.remaining
            )
        }
        return pathNode
    }

    private fun List<Token>.parseSelectList(): ParseNode {
        return parseCommaList {
            if (this.head?.type == TokenType.STAR) {
                ParseNode(ParseType.PROJECT_ALL, this.head, listOf(), this.tail)
            } else if (this.head != null && this.head?.keywordText in RESERVED_KEYWORDS) {
                this.head.err(
                    "Expected identifier or an expression but found unexpected keyword '${this.head?.keywordText ?: ""}' in a select list.",
                    ErrorCode.PARSE_UNEXPECTED_KEYWORD
                )
            } else {
                val expr = parseExpression().let {
                    when (it.type) {
                        ParseType.PATH -> inspectPathExpression(it)
                        else -> it
                    }
                }
                val rem = expr.remaining
                rem.parseOptionalAsAlias(expr)
            }
        }
    }

    private fun parseSelectAfterProjection(selectType: ParseType, projection: ParseNode): ParseNode {
        val children = ArrayList<ParseNode>()
        var rem = projection.remaining
        children.add(projection)

        // TODO support SELECT with no FROM
        if (rem.head?.keywordText != "from") {
            rem.err("Expected FROM after SELECT list", ErrorCode.PARSE_SELECT_MISSING_FROM)
        }

        val fromList = rem.tail.parseFromSourceList(OperatorPrecedenceGroups.SELECT.precedence)

        rem = fromList.remaining
        children.add(fromList)

        fun parseOptionalSingleExpressionClause(type: ParseType) {
            if (rem.head?.keywordText == type.identifier) {
                val expr = rem.tail.parseExpression(OperatorPrecedenceGroups.SELECT.precedence)
                rem = expr.remaining
                children.add(ParseNode(type, null, listOf(expr), rem))
            }
        }

        if (rem.head?.keywordText == "let") {
            val letParseNode = rem.parseLet()
            rem = letParseNode.remaining
            children.add(letParseNode)
        }

        parseOptionalSingleExpressionClause(ParseType.WHERE)

        if (rem.head?.keywordText == "group") {
            val groupToken = rem.head

            rem = rem.tail
            val type = when (rem.head?.keywordText) {
                "partial" -> {
                    rem = rem.tail
                    ParseType.GROUP_PARTIAL
                }
                else -> ParseType.GROUP
            }

            val groupChildren = ArrayList<ParseNode>()

            rem = rem.tailExpectedToken(TokenType.BY)

            val groupKey = rem.parseArgList(
                aliasSupportType = AliasSupportType.AS_ONLY,
                mode = ArgListMode.NORMAL_ARG_LIST,
                precedence = OperatorPrecedenceGroups.SELECT.precedence
            )
            groupKey.children.forEach {
                // TODO support ordinal case
                if (it.token?.type == TokenType.LITERAL) {
                    it.token.err(
                        "Literals (including ordinals) not supported in GROUP BY",
                        ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY
                    )
                }
            }
            groupChildren.add(groupKey)
            rem = groupKey.remaining

            if (rem.head?.keywordText == "group") {
                rem = rem.tail.tailExpectedKeyword("as")

                if (rem.head?.type?.isIdentifier() != true) {
                    rem.err(
                        "Expected identifier for GROUP name",
                        ErrorCode.PARSE_EXPECTED_IDENT_FOR_GROUP_NAME
                    )
                }
                groupChildren.add(rem.atomFromHead())
                rem = rem.tail
            }
            children.add(
                ParseNode(
                    type,
                    groupToken,
                    groupChildren,
                    rem
                )
            )
        }

        parseOptionalSingleExpressionClause(ParseType.HAVING)

        if (rem.head?.keywordText == "order") {
            val orderToken = rem.head
            rem = rem.tail.tailExpectedToken(TokenType.BY)

            val orderByChildren = listOf(rem.parseOrderByArgList())
            rem = orderByChildren.first().remaining

            children.add(
                ParseNode(type = ParseType.ORDER_BY, token = orderToken, children = orderByChildren, remaining = rem)
            )
        }

        parseOptionalSingleExpressionClause(ParseType.LIMIT)

        parseOptionalSingleExpressionClause(ParseType.OFFSET)

        return ParseNode(selectType, null, children, rem)
    }

    private fun List<Token>.parseFunctionCall(name: Token): ParseNode {
        fun parseCallArguments(callName: String, args: List<Token>, callType: ParseType): ParseNode = when (args.head?.type) {
            TokenType.STAR -> err("$callName(*) is not allowed", ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR)
            TokenType.RIGHT_PAREN -> ParseNode(callType, name, emptyList(), tail)
            else -> {
                args.parseArgList(aliasSupportType = AliasSupportType.NONE, mode = ArgListMode.NORMAL_ARG_LIST)
                    .copy(type = callType, token = name)
                    .deriveExpected(TokenType.RIGHT_PAREN)
            }
        }

        val callName = name.text!!
        val memoizedTail by lazy { tail }
        val keywordText = head?.keywordText

        return when (callName) {
            "count" -> {
                when {
                    head?.type == TokenType.RIGHT_PAREN -> {
                        err("Aggregate functions are always unary", ErrorCode.PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL)
                    }

                    // COUNT(*)
                    head?.type == TokenType.STAR -> {
                        ParseNode(ParseType.CALL_AGG_WILDCARD, name, emptyList(), tail).deriveExpected(TokenType.RIGHT_PAREN)
                    }

                    head?.type == TokenType.KEYWORD && keywordText == "distinct" -> {
                        when (memoizedTail.head?.type) {
                            // COUNT(DISTINCT *)
                            TokenType.STAR -> {
                                err("COUNT(DISTINCT *) is not supported", ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR)
                            }

                            // COUNT(DISTINCT expression)
                            else -> {
                                memoizedTail.parseArgList(aliasSupportType = AliasSupportType.NONE, mode = ArgListMode.NORMAL_ARG_LIST)
                                    .copy(type = ParseType.CALL_DISTINCT_AGG, token = name)
                                    .deriveExpected(TokenType.RIGHT_PAREN)
                            }
                        }
                    }

                    head?.type == TokenType.KEYWORD && keywordText == "all" -> {
                        when (memoizedTail.head?.type) {
                            TokenType.STAR -> err("COUNT(ALL *) is not supported", ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR)

                            // COUNT(ALL expression)
                            else -> {
                                memoizedTail.parseArgList(aliasSupportType = AliasSupportType.NONE, mode = ArgListMode.NORMAL_ARG_LIST)
                                    .copy(type = ParseType.CALL_AGG, token = name)
                                    .deriveExpected(TokenType.RIGHT_PAREN)
                            }
                        }
                    }

                    else -> parseArgList(aliasSupportType = AliasSupportType.NONE, mode = ArgListMode.NORMAL_ARG_LIST)
                        .copy(type = ParseType.CALL_AGG, token = name)
                        .deriveExpected(TokenType.RIGHT_PAREN)
                }
            }
            in STANDARD_AGGREGATE_FUNCTIONS -> {

                val call = when {
                    head?.type == TokenType.KEYWORD && head?.keywordText == "distinct" -> {
                        parseCallArguments(callName, tail, ParseType.CALL_DISTINCT_AGG)
                    }
                    head?.type == TokenType.KEYWORD && head?.keywordText == "all" -> {
                        parseCallArguments(callName, tail, ParseType.CALL_AGG)
                    }
                    else -> {
                        parseCallArguments(callName, this, ParseType.CALL_AGG)
                    }
                }

                if (call.children.size != 1) {
                    err(
                        "Aggregate functions are always unary",
                        ErrorCode.PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL
                    )
                }

                call
            }

            // normal function
            else -> parseCallArguments(callName, this, ParseType.CALL)
        }
    }

    private fun List<Token>.parseExec(): ParseNode {
        var rem = this
        if (rem.head?.type == TokenType.EOF) {
            rem.err("No stored procedure provided", ErrorCode.PARSE_NO_STORED_PROCEDURE_PROVIDED)
        }

        rem.forEach {
            if (it.keywordText?.toLowerCase() == "exec") {
                it.err("EXEC call found at unexpected location", ErrorCode.PARSE_UNEXPECTED_TERM)
            }
        }

        val procedureName = rem.head
        rem = rem.tail

        // Stored procedure call has no args
        if (rem.head?.type == TokenType.EOF) {
            return ParseNode(ParseType.EXEC, procedureName, emptyList(), rem)
        } else if (rem.head?.type == TokenType.LEFT_PAREN) {
            rem.err("Unexpected ${TokenType.LEFT_PAREN} found following stored procedure call", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }

        return rem.parseArgList(aliasSupportType = AliasSupportType.NONE, mode = ArgListMode.NORMAL_ARG_LIST)
            .copy(type = ParseType.EXEC, token = procedureName)
    }

    /**
     * Parses substring
     *
     * Syntax is either SUBSTRING(<str> FROM <start position> [FOR <string length>])
     * or SUBSTRING(<str>, <start position> [, <string length>])
     */
    private fun List<Token>.parseSubstring(name: Token): ParseNode {
        var rem = this

        if (rem.head?.type != TokenType.LEFT_PAREN) {
            val pvmap = PropertyValueMap()
            pvmap[Property.EXPECTED_TOKEN_TYPE] = TokenType.LEFT_PAREN
            rem.err(
                "Expected ${TokenType.LEFT_PAREN}",
                ErrorCode.PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL, pvmap
            )
        }

        var stringExpr = tail.parseExpression()
        rem = stringExpr.remaining
        var parseSql92Syntax = false

        stringExpr = when {
            rem.head!!.keywordText == "from" -> {
                parseSql92Syntax = true
                stringExpr.deriveExpectedKeyword("from")
            }
            rem.head!!.type == TokenType.COMMA -> stringExpr.deriveExpected(TokenType.COMMA)
            else -> rem.err(
                "Expected ${TokenType.KEYWORD} 'from' OR ${TokenType.COMMA}",
                ErrorCode.PARSE_EXPECTED_ARGUMENT_DELIMITER
            )
        }

        val (positionExpr: ParseNode, expectedToken: Token) = stringExpr.remaining.parseExpression()
            .deriveExpected(if (parseSql92Syntax) TokenType.FOR else TokenType.COMMA, TokenType.RIGHT_PAREN)

        if (expectedToken.type == TokenType.RIGHT_PAREN) {
            return ParseNode(
                ParseType.CALL,
                name,
                listOf(stringExpr, positionExpr),
                positionExpr.remaining
            )
        }

        rem = positionExpr.remaining
        val lengthExpr = rem.parseExpression().deriveExpected(TokenType.RIGHT_PAREN)
        return ParseNode(
            ParseType.CALL,
            name,
            listOf(stringExpr, positionExpr, lengthExpr),
            lengthExpr.remaining
        )
    }

    /**
     * Parses trim
     *
     * Syntax is TRIM([[ specification ] [to trim characters] FROM] <trim source>).
     */
    private fun List<Token>.parseTrim(name: Token): ParseNode {
        if (head?.type != TokenType.LEFT_PAREN) err(
            "Expected ${TokenType.LEFT_PAREN}",
            ErrorCode.PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL
        )

        var rem = tail
        val arguments = mutableListOf<ParseNode>()

        fun parseArgument(block: (ParseNode) -> ParseNode = { it }): List<Token> {
            val node = block(rem.parseExpression())
            arguments.add(node)

            return node.remaining
        }

        val maybeTrimSpec = rem.head
        val hasSpecification = when {
            maybeTrimSpec?.type == TokenType.IDENTIFIER &&
                TRIM_SPECIFICATION_KEYWORDS.contains(maybeTrimSpec.text?.toLowerCase()) -> {
                arguments.add(
                    ParseNode(
                        ParseType.ATOM, maybeTrimSpec.copy(type = TokenType.TRIM_SPECIFICATION),
                        listOf(), rem.tail
                    )
                )
                rem = rem.tail

                true
            }
            else -> false
        }

        if (hasSpecification) { // trim(spec [toRemove] from target)
            rem = when (rem.head?.keywordText) {
                "from" -> rem.tail
                else -> parseArgument { it.deriveExpectedKeyword("from") }
            }

            rem = parseArgument()
        } else {
            if (rem.head?.keywordText == "from") { // trim(from target)
                rem = rem.tail // skips from

                rem = parseArgument()
            } else { // trim([toRemove from] target)
                rem = parseArgument()

                if (rem.head?.keywordText == "from") {
                    rem = rem.tail // skips from

                    rem = parseArgument()
                }
            }
        }

        if (rem.head?.type != TokenType.RIGHT_PAREN) {
            rem.err("Expected ${TokenType.RIGHT_PAREN}", ErrorCode.PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL)
        }

        return ParseNode(ParseType.CALL, name, arguments, rem.tail)
    }

    private fun List<Token>.parseDateTimePart(): ParseNode {
        val maybeDateTimePart = this.head
        return when {
            maybeDateTimePart?.type == TokenType.IDENTIFIER && DATE_TIME_PART_KEYWORDS.contains(maybeDateTimePart.text?.toLowerCase()) -> {
                ParseNode(ParseType.ATOM, maybeDateTimePart.copy(type = TokenType.DATETIME_PART), listOf(), this.tail)
            }
            else -> maybeDateTimePart.err("Expected one of: $DATE_TIME_PART_KEYWORDS", ErrorCode.PARSE_EXPECTED_DATE_TIME_PART)
        }
    }

    /**
     * Parses extract function call.
     *
     * Syntax is EXTRACT(<date_time_part> FROM <timestamp>).
     */
    private fun List<Token>.parseExtract(name: Token): ParseNode {
        if (head?.type != TokenType.LEFT_PAREN) err(
            "Expected ${TokenType.LEFT_PAREN}",
            ErrorCode.PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL
        )
        val dateTimePart = this.tail.parseDateTimePart().deriveExpectedKeyword("from")
        val rem = dateTimePart.remaining
        val dateTimeType = rem.parseExpression().deriveExpected(TokenType.RIGHT_PAREN)

        return ParseNode(ParseType.CALL, name, listOf(dateTimePart, dateTimeType), dateTimeType.remaining)
    }

    /**
     * Parses a date string and validates that the date string is a string and of the format YYYY-MM-DD
     */
    private fun List<Token>.parseDate(): ParseNode {
        val dateStringToken = head
        if (dateStringToken?.value == null || dateStringToken.type != TokenType.LITERAL || !dateStringToken.value.isText) {
            err(
                "Expected date string followed by the keyword DATE, found ${head?.value?.type}",
                ErrorCode.PARSE_UNEXPECTED_TOKEN
            )
        }

        val dateString = dateStringToken.value.stringValue()

        // validate that the date string follows the format YYYY-MM-DD
        // Filter out the extended dates which can be specified with the '+' or '-' symbol.
        // '+99999-03-10' for example is allowed by LocalDate.parse and should be filtered out.
        if (!DATE_PATTERN_REGEX.matches(dateString!!)) {
            err("Expected DATE string to be of the format yyyy-MM-dd", ErrorCode.PARSE_INVALID_DATE_STRING)
        }
        try {
            LocalDate.parse(dateString, ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            err(e.localizedMessage, ErrorCode.PARSE_INVALID_DATE_STRING)
        }

        return ParseNode(ParseType.DATE, head, listOf(), tail)
    }

    /**
     * Parses the optional precision specified with TIME type.
     * The precision states the precision of the second's value in the time unit.
     * If the precision is specified, the function returns the [ParseNode] with the token as precision value.
     * Otherwise, the function returns the [ParseNode] with the token as null.
     * It also verifies that the specified precision is an unsigned integer.
     */
    private fun List<Token>.parseOptionalPrecision(): ParseNode =
        // If the optional precision is present
        if (head?.type == TokenType.LEFT_PAREN) {
            var rem = tail
            // Expected precision token to be unsigned integer between 0 and 9 inclusive
            if (rem.head == null || rem.head!!.type != TokenType.LITERAL || !rem.head!!.value!!.isUnsignedInteger ||
                rem.head!!.value!!.longValue() < 0 || rem.head!!.value!!.longValue() > MAX_PRECISION_FOR_TIME
            ) {
                rem.head.err("Expected integer value between 0 and 9 for precision", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME)
            }
            val precision = rem.head
            rem = rem.tail
            if (rem.head?.type != TokenType.RIGHT_PAREN) {
                rem.head.errExpectedTokenType(TokenType.RIGHT_PAREN)
            }
            ParseNode(ParseType.PRECISION, precision, listOf(), rem.tail)
        } else {
            ParseNode(ParseType.PRECISION, null, listOf(), this)
        }

    /**
     * Checks for the optional time zone indicator which is specified with "WITH TIME ZONE"
     */
    private fun List<Token>.checkForOptionalTimeZone(): Pair<List<Token>, Boolean> {
        // If the keyword is specified for time zone, it must be a series of keywords - "with time zone"
        if (head?.type == TokenType.KEYWORD) {
            val rem =
                tailExpectedKeyword("with")
                    .tailExpectedKeyword("time")
                    .tailExpectedKeyword("zone")
            return Pair(rem, true)
        }
        return Pair(this, false)
    }

    /**
     * Parses a time string and verifies that the time string is a string and is specified in the valid ISO 8601 format.
     * Allows for optional precision and time zone to be specified with the time.
     * The different valid usages are as follows:
     *
     *         1. TIME 'HH:MM:SS[.ddd....][+|-HH:MM]'
     *
     *         2. TIME (<p>) 'HH:MM:SS[.ddd....][+|-HH:MM]'
     *
     *         3. TIME WITH TIME ZONE 'HH:MM:SS[.ddd....][+|-HH:MM]'
     *
     *         4. TIME (<p>) WITH TIME ZONE 'HH:MM:SS[.ddd....][+|-HH:MM]'
     *
     * where p is the precision for the second's value in the time.
     * The valid range for the time zone offset is [-18:00 to +18:00]
     * If the time zone offset is not specified when used with "TIME WITH TIME ZONE", the system default time zone is picked.
     * This behaviour is consistent with most other database languages like SQL92, PostgreSQL etc.
     * It also makes more sense as the user using the keywords "TIME WITH TIME ZONE .." would expect the
     * local time zone offset to be used by default.
     */
    private fun List<Token>.parseTime(): ParseNode {

        var rem = this

        // Parses the time string with or without the time zone offset.
        fun tryTimeParsing(time: String?, formatter: DateTimeFormatter, parse: (String?, DateTimeFormatter) -> Temporal) {
            try {
                parse(time, formatter)
            } catch (e: DateTimeParseException) {
                rem.head.err(e.localizedMessage, ErrorCode.PARSE_INVALID_TIME_STRING)
            }
        }

        // 1. Parse for optional precision and store the precision value
        val precision = rem.parseOptionalPrecision()
        rem = precision.remaining

        // 2. Check for optional "with time zone" tokens and store the boolean
        val (remainingAfterOptionalTimeZone, withTimeZone) = rem.checkForOptionalTimeZone()
        rem = remainingAfterOptionalTimeZone

        val timeStringToken = rem.head
        if (timeStringToken?.value == null || timeStringToken.type != TokenType.LITERAL || !timeStringToken.value.isText) {
            rem.head.err(
                "Expected time string followed by the keyword TIME OR TIME WITH TIME ZONE, found ${rem.head?.value?.type}",
                ErrorCode.PARSE_UNEXPECTED_TOKEN
            )
        }

        // 3. Parse the time string as local time 'hh:mm:ss.dddd...' or local time with offset 'hh:mm:ss.dddd...[+|-]hh:mm'
        //      - If the time zone is true and the local offset is missing, consider local offset from the system settings.
        val timeString = timeStringToken.value.stringValue()?.replace(" ", "")
        if (!genericTimeRegex.matches(timeString!!)) {
            rem.head.err(
                "Invalid format for time string. Expected format is \"TIME [(p)] [WITH TIME ZONE] HH:MM:SS[.ddddd...][+|-HH:MM]\"",
                ErrorCode.PARSE_INVALID_TIME_STRING
            )
        }
        // For "TIME WITH TIME ZONE", if the time zone is not explicitly specified, we still consider it as valid.
        // We will add the default time zone to it later in the evaluation phase.
        if (!withTimeZone || timeWithoutTimeZoneRegex.matches(timeString)) {
            tryTimeParsing(timeString, ISO_TIME, LocalTime::parse)
        } else {
            tryTimeParsing(timeString, ISO_TIME, OffsetTime::parse)
        }

        // Extract the precision from the time string representation if the precision is not specified.
        // For e.g., TIME '23:12:12.12300' should have precision of 5.
        // The source span here is just the filler value and does not reflect the actual source location of the precision
        // as it does not exists in case the precision is unspecified.
        val precisionOfValue = precision.token
            ?: Token(
                TokenType.LITERAL,
                ion.newInt(getPrecisionFromTimeString(timeString)),
                sourceText = timeString,
                timeStringToken.span
            )

        return ParseNode(
            if (withTimeZone) ParseType.TIME_WITH_TIME_ZONE else ParseType.TIME,
            rem.head!!.copy(value = ion.newString(timeString)),
            listOf(precision.copy(token = precisionOfValue)),
            rem.tail
        )
    }

    /**
     * Parses a function call that has the syntax of `date_add` and `date_diff`.
     *
     * Syntax is <func>(<date_time_part>, <timestamp>, <timestamp>) where <func>
     * is the value of [name].
     */
    private fun List<Token>.parseDateAddOrDateDiff(name: Token): ParseNode {
        if (head?.type != TokenType.LEFT_PAREN) err(
            "Expected ${TokenType.LEFT_PAREN}",
            ErrorCode.PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL
        )
        val dateTimePart = this.tail.parseDateTimePart().deriveExpected(TokenType.COMMA)

        val timestamp1 = dateTimePart.remaining.parseExpression().deriveExpected(TokenType.COMMA)
        val timestamp2 = timestamp1.remaining.parseExpression().deriveExpected(TokenType.RIGHT_PAREN)

        return ParseNode(ParseType.CALL, name, listOf(dateTimePart, timestamp1, timestamp2), timestamp2.remaining)
    }

    private fun List<Token>.parseLet(): ParseNode {
        val letClauses = ArrayList<ParseNode>()
        var rem = this.tail
        var child = rem.parseExpression()
        rem = child.remaining

        if (rem.head?.type != TokenType.AS) {
            rem.head.err(
                "Expected ${TokenType.AS} following ${ParseType.LET} expr",
                ErrorCode.PARSE_EXPECTED_AS_FOR_LET
            )
        }

        rem = rem.tail

        if (rem.head?.type?.isIdentifier() != true) {
            rem.head.err(
                "Expected identifier for ${TokenType.AS}-alias",
                ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS
            )
        }

        var name = rem.head
        rem = rem.tail
        letClauses.add(ParseNode(ParseType.AS_ALIAS, name, listOf(child), rem))

        while (rem.head?.type == TokenType.COMMA) {
            rem = rem.tail
            child = rem.parseExpression()
            rem = child.remaining
            if (rem.head?.type != TokenType.AS) {
                rem.head.err("Expected ${TokenType.AS} following ${ParseType.LET} expr", ErrorCode.PARSE_EXPECTED_AS_FOR_LET)
            }

            rem = rem.tail

            if (rem.head?.type?.isIdentifier() != true) {
                rem.head.err("Expected identifier for ${TokenType.AS}-alias", ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS)
            }

            name = rem.head

            rem = rem.tail
            letClauses.add(ParseNode(ParseType.AS_ALIAS, name, listOf(child), rem))
        }
        return ParseNode(ParseType.LET, null, letClauses, rem)
    }

    private fun List<Token>.parseListLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = AliasSupportType.NONE,
            mode = ArgListMode.NORMAL_ARG_LIST
        ).copy(
            type = ParseType.LIST
        ).deriveExpected(TokenType.RIGHT_BRACKET)

    private fun List<Token>.parseBagLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = AliasSupportType.NONE,
            mode = ArgListMode.NORMAL_ARG_LIST
        ).copy(
            type = ParseType.BAG
        ).deriveExpected(TokenType.RIGHT_DOUBLE_ANGLE_BRACKET)

    private fun List<Token>.parseStructLiteral(): ParseNode =
        parseArgList(
            aliasSupportType = AliasSupportType.NONE,
            mode = ArgListMode.STRUCT_LITERAL_ARG_LIST
        ).copy(
            type = ParseType.STRUCT
        ).deriveExpected(TokenType.RIGHT_CURLY)

    private fun List<Token>.parseTableValues(): ParseNode =
        parseCommaList {
            var rem = this
            if (rem.head?.type != TokenType.LEFT_PAREN) {
                err(
                    "Expected ${TokenType.LEFT_PAREN} for row value constructor",
                    ErrorCode.PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR
                )
            }
            rem = rem.tail
            rem.parseArgList(
                aliasSupportType = AliasSupportType.NONE,
                mode = ArgListMode.NORMAL_ARG_LIST
            ).copy(
                type = ParseType.LIST
            ).deriveExpected(TokenType.RIGHT_PAREN)
        }

    private val parseCommaDelim: List<Token>.() -> ParseNode? = {
        when (head?.type) {
            TokenType.COMMA -> atomFromHead()
            else -> null
        }
    }

    private val parseJoinDelim: List<Token>.() -> ParseNode? = {
        when (head?.type) {
            TokenType.COMMA -> atomFromHead(ParseType.INNER_JOIN)
            TokenType.KEYWORD -> when (head?.keywordText) {
                "join", "cross_join", "inner_join" -> atomFromHead(ParseType.INNER_JOIN)
                "left_join", "left_cross_join" -> atomFromHead(ParseType.LEFT_JOIN)
                "right_join", "right_cross_join" -> atomFromHead(ParseType.RIGHT_JOIN)
                "outer_join", "outer_cross_join" -> atomFromHead(ParseType.OUTER_JOIN)
                else -> null
            }
            else -> null
        }
    }

    private fun List<Token>.parseOrderByArgList(): ParseNode {
        return parseDelimitedList(parseCommaDelim) {
            var rem = this

            var child = rem.parseExpression()
            var children = listOf(child)
            rem = child.remaining

            when (rem.head?.type) {
                TokenType.ASC, TokenType.DESC -> {
                    children = children + listOf(
                        ParseNode(
                            type = ParseType.ORDERING_SPEC,
                            token = rem.head,
                            children = listOf(),
                            remaining = rem.tail
                        )
                    )
                    rem = rem.tail
                }
                else -> { /* intentionally blank. */ }
            }
            when (rem.head?.type) {
                TokenType.NULLS -> {
                    rem = rem.tail
                    when (rem.head?.type) {
                        TokenType.FIRST, TokenType.LAST -> {
                            children = children + listOf(
                                ParseNode(
                                    type = ParseType.NULLS_SPEC,
                                    token = rem.head,
                                    children = listOf(),
                                    remaining = rem.tail
                                )
                            )
                        }
                        else -> rem.head.err("Expected FIRST OR LAST after NULLS", ErrorCode.PARSE_UNEXPECTED_TOKEN)
                    }
                    rem = rem.tail
                }
                else -> { /* intentionally left blank. */ }
            }.let { }
            ParseNode(type = ParseType.SORT_SPEC, token = null, children = children, remaining = rem)
        }
    }

    private fun List<Token>.parseFromSource(precedence: Int = -1, parseRemaining: Boolean = true): ParseNode {
        var rem = this
        var child = when (rem.head?.keywordText) {
            "unpivot" -> {
                val actualChild = rem.tail.parseExpression(precedence)
                ParseNode(
                    ParseType.UNPIVOT,
                    rem.head,
                    listOf(actualChild),
                    actualChild.remaining
                )
            }
            else -> {
                val isSubqueryOrLiteral = rem.tail.head?.type == TokenType.LITERAL || rem.tail.head?.keywordText == "select"
                if (rem.head?.type == TokenType.LEFT_PAREN && !isSubqueryOrLiteral) {
                    // Starts with a left paren and is not a subquery or literal, so parse as a from source
                    rem = rem.tail
                    rem.parseFromSource(precedence).deriveExpected(TokenType.RIGHT_PAREN)
                } else {
                    rem.parseExpression(precedence)
                }
            }
        }
        rem = child.remaining

        child = rem.parseOptionalMatchClause(child).also {
            rem = it.remaining
        }

        child = rem.parseOptionalAsAlias(child).also {
            rem = it.remaining
        }

        child = rem.parseOptionalAtAlias(child).also {
            rem = it.remaining
        }

        child = rem.parseOptionalByAlias(child).also {
            rem = it.remaining
        }

        var left = child

        var delim = rem.parseJoinDelim()
        if (parseRemaining) {
            while (delim?.type?.isJoin == true) {
                val isCrossJoin = delim.token?.keywordText?.contains("cross") ?: false
                val hasOnClause = delim.token?.type == TokenType.KEYWORD && !isCrossJoin
                var children: List<ParseNode>
                var joinToken: Token? = delim.token

                rem = rem.tail

                if (hasOnClause) {
                    // Explicit join
                    if (rem.head?.type == TokenType.LEFT_PAREN) {
                        // Starts with a left paren. Could indicate subquery/literal or indicate higher precedence
                        val isSubqueryOrLiteral = rem.tail.head?.type == TokenType.LITERAL || rem.tail.head?.keywordText == "select"
                        val parenClause = rem.parseFromSource(precedence, parseRemaining = true)
                        rem = parenClause.remaining

                        // check for an ON-clause
                        if (rem.head?.keywordText != "on") {
                            rem.err("Expected 'ON'", ErrorCode.PARSE_MALFORMED_JOIN)
                        }

                        val onClause = rem.tail.parseExpression(precedence)

                        rem = onClause.remaining
                        if (!isSubqueryOrLiteral) {
                            children = listOf(parenClause, left, onClause)
                        } else {
                            children = listOf(left, parenClause, onClause)
                        }
                    } else {
                        // Rest is just the right side of the clause
                        val rightRef = rem.parseFromSource(precedence, parseRemaining = false)
                        rem = rightRef.remaining

                        // check for an ON-clause
                        if (rem.head?.keywordText != "on") {
                            rem.err("Expected 'ON'", ErrorCode.PARSE_MALFORMED_JOIN)
                        }

                        val onClause = rem.tail.parseExpression(precedence)

                        rem = onClause.remaining

                        children = listOf(left, rightRef, onClause)
                    }
                } else {
                    // For implicit joins
                    val rightRef = rem.parseFromSource(precedence, parseRemaining = false)
                    rem = rightRef.remaining
                    children = listOf(left, rightRef)
                    if (delim.token?.type == TokenType.COMMA) {
                        joinToken = delim.token?.copy(
                            type = TokenType.KEYWORD,
                            value = ion.newSymbol("cross_join")
                        )
                    }
                }
                left = ParseNode(ParseType.FROM_SOURCE_JOIN, joinToken, children, rem)
                delim = rem.parseJoinDelim()
            }
            return left
        }
        return child
    }

    private fun List<Token>.parseFromSourceList(precedence: Int = -1): ParseNode {
        val child = this.parseFromSource(precedence)
        return ParseNode(ParseType.FROM_CLAUSE, null, listOf(child), child.remaining)
    }

    private fun List<Token>.parseArgList(
        aliasSupportType: AliasSupportType,
        mode: ArgListMode,
        precedence: Int = -1
    ): ParseNode {
        val parseDelim = parseCommaDelim

        return parseDelimitedList(parseDelim) { _ ->
            var rem = this
            var child = when (mode) {
                ArgListMode.STRUCT_LITERAL_ARG_LIST -> {
                    val field = rem.parseExpression(precedence).deriveExpected(TokenType.COLON)
                    rem = field.remaining
                    val value = rem.parseExpression(precedence)
                    ParseNode(ParseType.MEMBER, null, listOf(field, value), value.remaining)
                }
                ArgListMode.SIMPLE_PATH_ARG_LIST -> rem.parsePathTerm(PathMode.SIMPLE_PATH)
                ArgListMode.SET_CLAUSE_ARG_LIST -> {
                    val lvalue = rem.parsePathTerm(PathMode.SIMPLE_PATH)
                    rem = lvalue.remaining
                    val equalsOperator = rem.head
                    if (rem.head?.keywordText != "=") {
                        rem.err("Expected '='", ErrorCode.PARSE_MISSING_SET_ASSIGNMENT)
                    }
                    rem = rem.tail
                    val rvalue = rem.parseExpression(precedence)
                    ParseNode(ParseType.ASSIGNMENT, equalsOperator, listOf(lvalue, rvalue), rvalue.remaining)
                }
                ArgListMode.NORMAL_ARG_LIST -> rem.parseExpression(precedence)
            }
            rem = child.remaining

            if (aliasSupportType.supportsAs) {
                child = rem.parseOptionalAsAlias(child).also {
                    rem = it.remaining
                }
            }

            if (aliasSupportType.supportsAt) {
                child = rem.parseOptionalAtAlias(child).also {
                    rem = it.remaining
                }
            }

            if (aliasSupportType.supportsBy) {
                child = rem.parseOptionalByAlias(child).also {
                    rem = it.remaining
                }
            }
            child
        }
    }

    /**
     * Parse any token(s) which may denote an alias, taking the form of: <\[AS\] IDENTIFIER>.
     * [child] specifies a [ParseNode] that will become the child of the returned [ParseNode].
     * [keywordTokenType] specifies the [TokenType] of the keyword (e.g. AS, AT or BY)
     * [keywordIsOptional] specifies whether or not the keyword is optional (e.g. as in the case of `AS`)
     * [parseNodeType] specifies the type of the returned [ParseNode].
     */
    private fun List<Token>.parseOptionalAlias(
        child: ParseNode,
        keywordTokenType: TokenType,
        keywordIsOptional: Boolean,
        parseNodeType: ParseType
    ): ParseNode {
        var rem = this
        return when {
            rem.head?.type == keywordTokenType -> {
                rem = rem.tail
                val name = rem.head
                if (rem.head?.type?.isIdentifier() != true) {
                    rem.head.err(
                        "Expected identifier for $keywordTokenType-alias",
                        ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS
                    )
                }
                rem = rem.tail
                ParseNode(parseNodeType, name, listOf(child), rem)
            }
            keywordIsOptional && rem.head?.type?.isIdentifier() ?: false -> {
                ParseNode(parseNodeType, rem.head, listOf(child), rem.tail)
            } else -> {
                child
            }
        }
    }

    private fun List<Token>.parseOptionalAsAlias(child: ParseNode) =
        parseOptionalAlias(child = child, keywordTokenType = TokenType.AS, keywordIsOptional = true, parseNodeType = ParseType.AS_ALIAS)

    private fun List<Token>.parseOptionalAtAlias(child: ParseNode) =
        parseOptionalAlias(child = child, keywordTokenType = TokenType.AT, keywordIsOptional = false, parseNodeType = ParseType.AT_ALIAS)

    private fun List<Token>.parseOptionalByAlias(child: ParseNode) =
        parseOptionalAlias(child = child, keywordTokenType = TokenType.BY, keywordIsOptional = false, parseNodeType = ParseType.BY_ALIAS)

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
    private inline fun List<Token>.parseDelimitedList(
        parseDelim: List<Token>.() -> ParseNode?,
        parseItem: List<Token>.(delim: ParseNode?) -> ParseNode
    ): ParseNode {
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
        return ParseNode(ParseType.ARG_LIST, null, items, rem)
    }

    private fun ParseNode.throwTopLevelParserError(): Nothing =
        token?.err("Keyword ${token.text} only expected at the top level in the query", ErrorCode.PARSE_UNEXPECTED_TERM)
            ?: throw ParserException(
                "Keyword ${token?.text} only expected at the top level in the query",
                ErrorCode.PARSE_UNEXPECTED_TERM, PropertyValueMap()
            )

    private fun List<Token>.parseOptionalMatchClause(child: ParseNode): ParseNode {
        var rem = this
        return when (rem.head?.keywordText) {
            "match" -> {
                rem = rem.tail

                // `maybeNested` is a heuristic as to whether the whole match is surrounded by parentheses
                //     e.g., `SELECT ... FROM g MATCH ( () -> () )`
                val maybeNested = if (rem.head?.type == TokenType.LEFT_PAREN) {
                    val nextNextIsRParen = rem.tail.head?.type == TokenType.RIGHT_PAREN
                    var looksLikeNode = false
                    for (t in rem.tail) {
                        if (t.type == TokenType.LEFT_PAREN) {
                            break // another left paren means this is likely parens around the whole match
                        } else if (t.type in listOf(TokenType.COLON, TokenType.RIGHT_PAREN)) {
                            looksLikeNode = true
                            break
                        }
                    }

                    !nextNextIsRParen && !looksLikeNode
                } else {
                    false
                }

                if (maybeNested) {
                    try {
                        rem.tail.parseMatch(child).deriveExpected(TokenType.RIGHT_PAREN)
                    } catch (e: ParserException) {
                        rem.parseMatch(child)
                    }
                } else {
                    rem.parseMatch(child)
                }
            }
            else -> {
                child
            }
        }
    }

    private fun List<Token>.parseMatch(expr: ParseNode): ParseNode {
        var rem = this

        fun consume(type: TokenType): Boolean {
            if (rem.head?.type == type) {
                rem = rem.tail
                return true
            }
            return false
        }

        fun consumeInt(): ParseNode? {
            if (rem.head?.type == TokenType.LITERAL &&
                rem.head!!.value?.isUnsignedInteger == true
            ) {
                val int = rem.atomFromHead()
                rem = rem.tail
                return int
            }
            return null
        }

        fun consumeKW(keyword: String): Boolean {
            return when (rem.head?.type!!) {
                TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER, TokenType.KEYWORD -> {
                    if (rem.head!!.sourceText.toUpperCase() == keyword) {
                        rem = rem.tail
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        fun parseSelector(): ParseNode? {
            var selector: Token? = null
            var selectorK: ParseNode? = null
            val start = rem

            fun span(count: Int): SourceSpan {
                val startSpan = start.head!!.span
                var last = startSpan
                var len = 0L
                for (next in start.tail.subList(0, count - 1)) {
                    if (next.span.line == last.line) {
                        len += (next.span.column - last.column)
                    } else {
                        len += last.length
                    }
                    last = next.span
                }
                len += last.length

                return SourceSpan(startSpan.line, startSpan.column, len)
            }

            if (consumeKW("ANY")) {
                if (consumeKW("SHORTEST")) {
                    selector =
                        Token(TokenType.OPERATOR, ion.newSymbol("ANY_SHORTEST"), "ANY_SHORTEST", span(2))
                } else {
                    val k = consumeInt()
                    if (k != null) {
                        selector = Token(TokenType.OPERATOR, ion.newSymbol("ANY_K"), "ANY_K", span(2))
                        selectorK = k
                    } else {
                        selector = Token(TokenType.OPERATOR, ion.newSymbol("ANY"), "ANY", span(1))
                    }
                }
            } else if (consumeKW("ALL")) {
                if (consumeKW("SHORTEST")) {
                    selector =
                        Token(TokenType.OPERATOR, ion.newSymbol("ALL_SHORTEST"), "ALL_SHORTEST", span(2))
                } else {
                    rem.head.err(
                        "Expected 'SHORTEST' after 'ALL'",
                        ErrorCode.PARSE_EXPECTED_KEYWORD_FOR_MATCH
                    )
                }
            } else if (consumeKW("SHORTEST")) {
                val k = consumeInt()
                if (k != null) {
                    selector = if (consumeKW("GROUP")) {
                        Token(
                            TokenType.OPERATOR,
                            ion.newSymbol("SHORTEST_K_GROUP"),
                            "SHORTEST_K_GROUP",
                            span(3)
                        )
                    } else {
                        Token(TokenType.OPERATOR, ion.newSymbol("SHORTEST_K"), "SHORTEST_K", span(2))
                    }
                    selectorK = k
                } else {
                    rem.head.err(
                        "Expected a number after 'SHORTEST'",
                        ErrorCode.PARSE_EXPECTED_KEYWORD_FOR_MATCH
                    )
                }
            }

            return if (selector != null) {
                ParseNode(ParseType.MATCH_EXPR_SELECTOR, selector, listOfNotNull(selectorK), rem)
            } else {
                null
            }
        }

        val selector = parseSelector()
        rem = selector?.remaining ?: rem

        val matches = ArrayList<ParseNode>()
        do {
            val pattern = rem.parseMatchPattern()
            matches.add(pattern)
            rem = pattern.remaining
        } while (consume(TokenType.COMMA))

        return ParseNode(ParseType.MATCH, this.head, listOfNotNull(expr, selector) + matches, rem)
    }

    // left/right/undirected edge directions essentially form a 3-bit flag
    // represent here the 7 (non-zero) 3-bit combinations to their abbreviation for lookup
    val matchEdgeAbbreviationMap = Array(2) { Array(2) { Array(2) { "" } } }.also {
        it[0][0][1] = "~"
        it[0][1][0] = "->"
        it[0][1][1] = "~>"
        it[1][0][0] = "<-"
        it[1][0][1] = "<~"
        it[1][1][0] = "<->"
        it[1][1][1] = "-"
    }

    data class EdgeType(val left: Boolean, val right: Boolean, val undirected: Boolean) {
        // Just union all the left/right/undirected flags
        fun union(other: EdgeType): EdgeType {
            val left = this.left.or(other.left)
            val right = this.right.or(other.right)
            val undirected = this.undirected.or(other.undirected)
            val union = EdgeType(left = left, right = right, undirected = undirected)
            return union
        }

        // Combine leading and trailing edge detection.
        // If leading thinks right and trailing thinks left
        //    then left + right + undirected
        // else
        //    union(leading, trailing)
        fun combine(other: EdgeType): EdgeType {
            return if (this == EdgeType(left = false, right = true, undirected = false) &&
                other == EdgeType(left = true, right = false, undirected = false)
            ) {
                EdgeType(left = true, right = true, undirected = true)
            } else {
                this.union(other)
            }
        }
    }

    fun EdgeType.abbreviation(): String {
        return matchEdgeAbbreviationMap[if (left) 1 else 0][if (right) 1 else 0][if (undirected) 1 else 0]
    }

    val matchAbbreviations = HashMap<String, EdgeType>().also {
        for (lIdx in 0..1) {
            for (rIdx in 0..1) {
                for (unIdx in 0..1) {
                    val abbreviation = matchEdgeAbbreviationMap[lIdx][rIdx][unIdx]
                    if (abbreviation.isNotEmpty()) {
                        it[abbreviation] = EdgeType(left = (lIdx > 0), right = (rIdx > 0), undirected = (unIdx > 0))
                    }
                }
            }
        }
    }

    val matchRestrictorKWs = listOf("TRAIL", "ACYCLIC", "SIMPLE")

    private fun List<Token>.parseMatchPattern(): ParseNode {
        var rem = this

        fun parseName(): ParseNode? {
            return when (rem.head?.type!!) {
                TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER, TokenType.KEYWORD -> {
                    val name = rem.atomFromHead()
                    ParseNode(ParseType.MATCH_EXPR_NAME, null, listOf(name), name.remaining)
                }
                else -> null
            }
        }

        fun parseLabel(): ParseNode? {
            return when (rem.head?.type) {
                TokenType.COLON -> {
                    rem = rem.tail
                    when (rem.head?.type!!) {
                        TokenType.IDENTIFIER, TokenType.QUOTED_IDENTIFIER, TokenType.KEYWORD -> {
                            val name = rem.atomFromHead()
                            ParseNode(ParseType.MATCH_EXPR_LABEL, null, listOf(name), name.remaining)
                        }
                        else -> {
                            rem.head.err(
                                "Expected identifier for",
                                ErrorCode.PARSE_EXPECTED_IDENT_FOR_MATCH
                            )
                        }
                    }
                }
                else -> null
            }
        }

        // 'consume' a single token matching the specified type and optionally matching a specified keyword
        fun consume(type: TokenType, keywordText: String? = null): Boolean {
            if (rem.head?.type == type) {
                if (keywordText == null || rem.head?.keywordText == keywordText) {
                    rem = rem.tail
                    return true
                }
            }
            return false
        }

        // `consume` a single token matching the specified type or throw an error if not possible
        fun expect(type: TokenType, keywordText: String? = null, errorCode: ErrorCode, errorMsg: String) {
            if (!consume(type, keywordText)) {
                rem.head.err(
                    "Expected ${type.name} for $errorMsg", errorCode
                )
            }
        }

        fun parseNode(): ParseNode {
            expect(TokenType.LEFT_PAREN, null, ErrorCode.PARSE_EXPECTED_LEFT_PAREN_FOR_MATCH_NODE, "match node")

            val name = parseName()
            rem = name?.remaining ?: rem

            val label = parseLabel()
            rem = label?.remaining ?: rem

            val predicate = if (rem.head?.keywordText == "where") {
                rem.tail.parseExpression()
            } else {
                null
            }
            rem = predicate?.remaining ?: rem

            expect(TokenType.RIGHT_PAREN, null, ErrorCode.PARSE_EXPECTED_RIGHT_PAREN_FOR_MATCH_NODE, "match node")

            return ParseNode(ParseType.MATCH_EXPR_NODE, null, listOfNotNull(name, label, predicate), rem)
        }

        fun errorEdgeParse(): Nothing {
            rem.head.err("Expected edge pattern for match", ErrorCode.PARSE_EXPECTED_EDGE_PATTERN_MATCH_EDGE)
        }

        fun parseLeftEdgePattern(): EdgeType {
            val direction = if (rem.head?.type == TokenType.OPERATOR) {
                when (rem.head!!.keywordText) {
                    "<" -> {
                        rem = rem.tail
                        if (rem.head?.type == TokenType.OPERATOR) {
                            when (rem.head!!.keywordText) {
                                "-" -> EdgeType(left = true, right = false, undirected = false)
                                "~" -> EdgeType(left = true, right = false, undirected = true)
                                else -> errorEdgeParse()
                            }
                        } else {
                            errorEdgeParse()
                        }
                    }
                    "-" -> EdgeType(left = false, right = true, undirected = false)
                    "~" -> EdgeType(left = false, right = false, undirected = true)
                    else -> errorEdgeParse()
                }
            } else {
                errorEdgeParse()
            }
            rem = rem.tail

            return direction
        }

        fun parseRightEdgePattern(): EdgeType {
            val direction = if (rem.head?.type == TokenType.OPERATOR) {
                when (rem.head!!.keywordText) {
                    "-" -> {
                        if (rem.tail.head?.type == TokenType.OPERATOR && rem.tail.head?.keywordText == ">") {
                            rem = rem.tail
                            EdgeType(left = false, right = true, undirected = false)
                        } else {
                            EdgeType(left = true, right = false, undirected = false)
                        }
                    }
                    "~" -> {
                        if (rem.tail.head?.type == TokenType.OPERATOR && rem.tail.head?.keywordText == ">") {
                            rem = rem.tail
                            EdgeType(left = false, right = true, undirected = true)
                        } else {
                            EdgeType(left = false, right = false, undirected = true)
                        }
                    }
                    else -> errorEdgeParse()
                }
            } else {
                errorEdgeParse()
            }
            rem = rem.tail

            return direction
        }

        // Parses an edge pattern containing a spec as defined by
        //
        // | Orientation               | Edge pattern | Abbreviation |
        // |---------------------------+--------------+--------------|
        // | Pointing left             | <−[ spec ]−  | <−           |
        // | Undirected                | ~[ spec ]~   | ~            |
        // | Pointing right            | −[ spec ]−>  | −>           |
        // | Left or undirected        | <~[ spec ]~  | <~           |
        // | Undirected or right       | ~[ spec ]~>  | ~>           |
        // | Left or right             | <−[ spec ]−> | <−>          |
        // | Left, undirected or right | −[ spec ]−   | −            |
        //
        // Fig. 5. Table of edge patterns:
        // https://arxiv.org/abs/2112.06217
        fun parseEdgeWithSpec(): ParseNode {
            val dir1 = parseLeftEdgePattern()

            expect(TokenType.LEFT_BRACKET, null, ErrorCode.PARSE_EXPECTED_LEFT_BRACKET_FOR_MATCH_EDGE, "match edge")

            val name = parseName()
            rem = name?.remaining ?: rem

            val label = parseLabel()
            rem = label?.remaining ?: rem

            val predicate = if (rem.head?.keywordText == "where") {
                rem.tail.parseExpression()
            } else {
                null
            }
            rem = predicate?.remaining ?: rem

            expect(TokenType.RIGHT_BRACKET, null, ErrorCode.PARSE_EXPECTED_RIGHT_BRACKET_FOR_MATCH_EDGE, "match edge")

            val dir2 = parseRightEdgePattern()

            val dir = dir1.combine(dir2)

            val directionToken =
                Token(TokenType.OPERATOR, ion.newSymbol(dir.abbreviation()), dir.abbreviation(), SourceSpan(0, 0, 0))
            val direction = ParseNode(ParseType.MATCH_EXPR_EDGE_DIRECTION, directionToken, emptyList(), rem)

            return ParseNode(ParseType.MATCH_EXPR_EDGE, null, listOfNotNull(direction, name, label, predicate), rem)
        }

        // Parses an abbreviated edge pattern (i.e, no label, no variable, no predicate) as defined by
        //
        // | Orientation               | Edge pattern | Abbreviation |
        // |---------------------------+--------------+--------------|
        // | Pointing left             | <−[ spec ]−  | <−           |
        // | Undirected                | ~[ spec ]~   | ~            |
        // | Pointing right            | −[ spec ]−>  | −>           |
        // | Left or undirected        | <~[ spec ]~  | <~           |
        // | Undirected or right       | ~[ spec ]~>  | ~>           |
        // | Left or right             | <−[ spec ]−> | <−>          |
        // | Left, undirected or right | −[ spec ]−   | −            |
        //
        // Fig. 5. Table of edge patterns:
        // https://arxiv.org/abs/2112.06217
        fun parseEdgeAbbreviated(): ParseNode {
            var candidates: Map<String, EdgeType> = matchAbbreviations
            do {
                if (rem.head?.type == TokenType.OPERATOR && rem.head!!.keywordText in listOf("<", "-", ">", "~")) {
                    val char = rem.head!!.keywordText!!
                    rem = rem.tail
                    candidates = candidates.filterKeys { it.startsWith(char) }.mapKeys { it.key.removePrefix(char) }
                    if (candidates.size == 1) {
                        val edge = candidates.values.first()
                        val directionToken =
                            Token(
                                TokenType.OPERATOR,
                                ion.newSymbol(edge.abbreviation()),
                                edge.abbreviation(),
                                SourceSpan(0, 0, 0)
                            )
                        val direction = ParseNode(ParseType.MATCH_EXPR_EDGE_DIRECTION, directionToken, emptyList(), rem)
                        return ParseNode(ParseType.MATCH_EXPR_EDGE, null, listOf(direction), rem)
                    }
                } else if (candidates.contains("")) {
                    val edge = candidates[""]!!
                    val directionToken =
                        Token(
                            TokenType.OPERATOR,
                            ion.newSymbol(edge.abbreviation()),
                            edge.abbreviation(),
                            SourceSpan(0, 0, 0)
                        )
                    val direction = ParseNode(ParseType.MATCH_EXPR_EDGE_DIRECTION, directionToken, emptyList(), rem)
                    return ParseNode(ParseType.MATCH_EXPR_EDGE, null, listOf(direction), rem)
                } else {
                    errorEdgeParse()
                }
            } while (candidates.isNotEmpty())
            errorEdgeParse()
        }

        fun parseQuantifier(): List<ParseNode> {
            return when (rem.head?.type) {
                TokenType.STAR -> {
                    val q = listOf(ParseNode(ParseType.MATCH_EXPR_QUANTIFIER, rem.head, listOf(), rem.tail))
                    rem = rem.tail
                    q
                }
                TokenType.OPERATOR -> {
                    when (rem.head!!.keywordText) {
                        "+" -> {
                            val q = listOf(ParseNode(ParseType.MATCH_EXPR_QUANTIFIER, rem.head, listOf(), rem.tail))
                            rem = rem.tail
                            q
                        }
                        else -> emptyList()
                    }
                }
                TokenType.LEFT_CURLY -> {
                    rem = rem.tail
                    if (rem.head?.type == TokenType.LITERAL) {
                        val lower = rem.atomFromHead(ParseType.MATCH_EXPR_QUANTIFIER)
                        rem = rem.tail

                        expect(TokenType.COMMA, null, ErrorCode.PARSE_EXPECTED_EDGE_PATTERN_MATCH_EDGE, "quantifier")

                        val upper = if (rem.head?.type == TokenType.LITERAL) {
                            val upper = rem.atomFromHead(ParseType.MATCH_EXPR_QUANTIFIER)
                            rem = rem.tail
                            upper
                        } else {
                            null
                        }

                        expect(
                            TokenType.RIGHT_CURLY,
                            null,
                            ErrorCode.PARSE_EXPECTED_EDGE_PATTERN_MATCH_EDGE,
                            "quantifier"
                        )

                        listOfNotNull(lower, upper)
                    } else {
                        errorEdgeParse()
                    }
                }
                else -> {
                    emptyList()
                }
            }
        }

        fun parseEdge(): ParseNode {
            val preRem = rem
            val edge = try {
                parseEdgeWithSpec()
            } catch (e: ParserException) {
                rem = preRem
                parseEdgeAbbreviated()
            }

            val quantifer = parseQuantifier()
            return edge.copy(children = edge.children + quantifer, remaining = rem)
        }

        fun parseRestrictor(): ParseNode? {
            return when (rem.head?.type!!) {
                TokenType.IDENTIFIER -> {
                    if (rem.head!!.sourceText.toUpperCase() in matchRestrictorKWs) {
                        val name = rem.atomFromHead()
                        rem = name.remaining
                        ParseNode(ParseType.MATCH_EXPR_RESTRICTOR, null, listOf(name), name.remaining)
                    } else {
                        null
                    }
                }
                else -> null
            }
        }

        fun parsePathVariable(): ParseNode? {
            val name = parseName()
            return if (name != null) {
                rem = name.remaining
                expect(
                    TokenType.OPERATOR,
                    "=",
                    ErrorCode.PARSE_EXPECTED_EQUALS_FOR_MATCH_PATH_VARIABLE,
                    "path variable"
                )
                name.copy(remaining = rem)
            } else {
                null
            }
        }

        fun parseParenthesizedPattern(): ParseNode? {
            fun parse(expectedClose: TokenType): Pair<ParseNode, ParseNode?>? {
                // look ahead 1, parse `()` as empty node and `[]` as empty edge, not an empty pattern
                if (rem.tail.head?.type == expectedClose) {
                    return null
                }

                rem = rem.tail
                val pattern = rem.parseMatchPattern()
                rem = pattern.remaining

                val predicate = if (rem.head?.keywordText == "where") {
                    rem.tail.parseExpression()
                } else {
                    null
                }
                rem = predicate?.remaining ?: rem

                expect(
                    expectedClose,
                    null,
                    ErrorCode.PARSE_EXPECTED_PARENTHESIZED_PATTERN,
                    "parenthesized pattern"
                )

                return Pair(pattern, predicate)
            }

            val preParseRem = rem
            val parenthesized = try {
                when (rem.head?.type) {
                    TokenType.LEFT_BRACKET -> parse(TokenType.RIGHT_BRACKET)
                    TokenType.LEFT_PAREN -> parse(TokenType.RIGHT_PAREN)
                    else -> null
                }
            } catch (e: ParserException) {
                rem = preParseRem
                null
            }

            return if (parenthesized != null) {
                val subPattern = parenthesized.first
                val predicate = parenthesized.second
                val quantifer = parseQuantifier()
                subPattern.copy(children = subPattern.children + quantifer + listOfNotNull(predicate), remaining = rem)
            } else {
                null
            }
        }

        fun parsePatternPart(): ParseNode? {
            val parenthesized = try {
                parseParenthesizedPattern()
            } catch (e: ParserException) {
                null
            }

            return parenthesized ?: try {
                parseNode()
            } catch (e: ParserException) {
                try {
                    parseEdge()
                } catch (e: ParserException) {
                    null
                }
            }
        }

        val patternElements = ArrayList<ParseNode?>()

        val restrictor = parseRestrictor()
        patternElements.add(restrictor)

        val pathVariable = parsePathVariable()
        patternElements.add(pathVariable)

        do {
            val part = parsePatternPart()
            patternElements.add(part)
        } while (part != null)

        return ParseNode(ParseType.MATCH_EXPR, null, patternElements.filterNotNull(), rem)
    }

    /**
     * Validates tree to make sure that the top level tokens are not found below the top level.
     * Top level tokens are the tokens or keywords which are valid to be used only at the top level in the query.
     * i.e. these tokens cannot be used with a mix of other commands. Hence if more than one top level tokens are found
     * in the query then it is invalid.
     * [level] is the current traversal level in the parse tree.
     * If [topLevelTokenSeen] is true, it means it has been encountered at least once before while traversing the parse tree.
     * If [dmlListTokenSeen] is true, it means it has been encountered at least once before while traversing the parse tree.
     */
    private fun validateTopLevelNodes(
        node: ParseNode,
        level: Int,
        topLevelTokenSeen: Boolean,
        dmlListTokenSeen: Boolean
    ) {
        checkThreadInterrupted()
        val isTopLevelType = when (node.type.isDml) {
            // DML_LIST token type allows multiple DML keywords to be used in the same statement.
            // Hence, DML keyword tokens are not treated as top level tokens if present with the DML_LIST token type
            true -> !dmlListTokenSeen && node.type.isTopLevelType
            else -> node.type.isTopLevelType
        }
        if (topLevelTokenSeen && isTopLevelType) {
            node.throwTopLevelParserError()
        }

        if (isTopLevelType && level > 0) {
            // Note that for DML operations, top level parse node may be of type 'FROM' and nested within a `DML_LIST`
            // Hence the check level > 1
            if (node.type.isDml) {
                if (level > 1) {
                    node.throwTopLevelParserError()
                }
            } else {
                node.throwTopLevelParserError()
            }
        }
        node.children.map {
            validateTopLevelNodes(
                node = it,
                level = level + 1,
                topLevelTokenSeen = topLevelTokenSeen || isTopLevelType,
                dmlListTokenSeen = dmlListTokenSeen || node.type == SqlParser.ParseType.DML_LIST
            )
        }
    }

    /** Entry point into the parser. */
    @Deprecated("`ExprNode` is deprecated. Please use `parseAstStatement` instead. ")
    @Suppress("DEPRECATION")
    override fun parseExprNode(source: String): org.partiql.lang.ast.ExprNode {
        return parseAstStatement(source).toExprNode(ion)
    }

    /**
     * Parse given source node as a PartiqlAst.Statement
     */
    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        val tokens = SqlLexer(ion).tokenize(source)
        val node = tokens.parseExpression()
        val rem = node.remaining
        if (!rem.onlyEndOfStatement()) {
            when (rem.head?.type) {
                TokenType.SEMICOLON -> rem.tail.err(
                    "Unexpected token after semicolon. (Only one query is allowed.)",
                    ErrorCode.PARSE_UNEXPECTED_TOKEN
                )
                else -> rem.err("Unexpected token after expression", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }

        validateTopLevelNodes(node = node, level = 0, topLevelTokenSeen = false, dmlListTokenSeen = false)

        return node.toAstStatement()
    }

    override fun parse(source: String): IonSexp =
        @Suppress("DEPRECATION")
        org.partiql.lang.ast.AstSerializer.serialize(
            parseExprNode(source),
            org.partiql.lang.ast.AstVersion.V0, ion
        )
}

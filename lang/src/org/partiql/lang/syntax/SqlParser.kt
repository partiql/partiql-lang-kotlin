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

import com.amazon.ion.*
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.ErrorCode.*
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.Property.*
import org.partiql.lang.errors.*
import org.partiql.lang.syntax.SqlParser.AliasSupportType.*
import org.partiql.lang.syntax.SqlParser.ArgListMode.*
import org.partiql.lang.syntax.SqlParser.ParseType.*
import org.partiql.lang.syntax.TokenType.*
import org.partiql.lang.syntax.TokenType.KEYWORD
import org.partiql.lang.util.*

/**
 * Parses a list of tokens as infix query expression into a prefix s-expression
 * as the abstract syntax tree.
 */
class SqlParser(private val ion: IonSystem) : Parser {

    private val trueValue: IonBool = ion.newBool(true)

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

    internal enum class ParseType(val isJoin: Boolean = false) {
        ATOM,
        CASE_SENSITIVE_ATOM,
        CASE_INSENSITIVE_ATOM,
        PROJECT_ALL,           // Wildcard, i.e. the * in `SELECT * FROM f` and a.b.c.* in `SELECT a.b.c.* FROM f`
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
        GROUP,
        GROUP_PARTIAL,
        HAVING,
        LIMIT,
        PIVOT,
        UNPIVOT,
        CALL,
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
        CAST,
        TYPE,
        CASE,
        WHEN,
        ELSE,
        BAG,
        INSERT,
        INSERT_VALUE,
        REMOVE,
        SET,
        UPDATE,
        DELETE,
        ASSIGNMENT,
        FROM,
        FROM_CLAUSE,
        FROM_SOURCE_JOIN,
        CREATE_TABLE,
        DROP_TABLE,
        DROP_INDEX,
        CREATE_INDEX,
        PARAMETER;

        val identifier = name.toLowerCase()
    }

    internal data class ParseNode(val type: ParseType,
                                  val token: Token?,
                                  val children: List<ParseNode>,
                                  val remaining: List<Token>) {

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
                pvmap[EXPECTED_TOKEN_TYPE_1_OF_2] = expectedType1
                pvmap[EXPECTED_TOKEN_TYPE_2_OF_2] = expectedType2
                this.remaining.err("Expected $type", PARSE_EXPECTED_2_TOKEN_TYPES, pvmap)
            } else {
                Pair(copy(remaining = this.remaining.tail), this.remaining.head!!)
            }

        fun deriveExpectedKeyword(keyword: String): ParseNode = derive { tailExpectedKeyword(keyword) }

        val isNumericLiteral = type == ATOM && when (token?.type) {
            LITERAL -> token.value?.isNumeric ?: false
            else -> false
        }

        fun numberValue(): Number = token?.value?.numberValue()
            ?: unsupported("Could not interpret token as number", PARSE_EXPECTED_NUMBER)

        fun unsupported(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap = PropertyValueMap()): Nothing =
            remaining.err(message, errorCode, errorContext)

        fun errMalformedParseTree(message: String): Nothing {
            val context = PropertyValueMap()
            token?.position?.let {
                context[Property.LINE_NUMBER] = it.line
                context[Property.COLUMN_NUMBER] = it.column
            }
            throw ParserException(message, ErrorCode.PARSE_MALFORMED_PARSE_TREE, context)
        }
    }

    private fun Token.toSourceLocation() = SourceLocationMeta(position.line, position.column)

    private fun Token?.toSourceLocationMetaContainer(): MetaContainer =
        if(this == null) {
            metaContainerOf()
        } else {
            metaContainerOf(this.toSourceLocation())
        }

    private fun ParseNode.toSymbolicName(): SymbolicName {
        if(token == null) {
            errMalformedParseTree("Expected ParseNode to have a token")
        }
        when (token.type) {
            LITERAL, IDENTIFIER, QUOTED_IDENTIFIER -> {
                val tokenText = token.text ?: errMalformedParseTree("Expected ParseNode.token to have text")
                return SymbolicName(tokenText, token.toSourceLocationMetaContainer())
            }
            else                                 -> {
                errMalformedParseTree("TokenType.${token.type} cannot be converted to a SymbolicName")
            }
        }
     }

    //***************************************
    // toExprNode
    //***************************************
    private fun ParseNode.toExprNode(): ExprNode {
        val metas = token.toSourceLocationMetaContainer()
        return when (type) {
            ATOM -> when (token?.type) {
                LITERAL, NULL, TRIM_SPECIFICATION, DATE_PART -> {
                    Literal(token.value!!, metas)
                }
                MISSING                                      -> {
                    LiteralMissing(metas)
                }
                QUOTED_IDENTIFIER                            -> {
                    VariableReference(
                        token.text!!,
                        CaseSensitivity.SENSITIVE,
                        metas = metas)
                }
                IDENTIFIER                                   -> {
                    VariableReference(
                        token.text!!,
                        CaseSensitivity.INSENSITIVE,
                        metas = metas)
                }
                else                                         -> {
                    errMalformedParseTree("Unsupported atom token type ${token?.type}")
                }
            }
            LIST -> {
                Seq(SeqType.LIST, children.map { it.toExprNode() }, metas)
            }
            BAG -> {
                Seq(SeqType.BAG, children.map { it.toExprNode() }, metas)
            }
            STRUCT -> {
                val fields = children.map {
                    if (it.type != MEMBER) {
                        errMalformedParseTree("Expected MEMBER node as direct descendant of a STRUCT node but instead found ${it.type}")
                    }
                    if (it.children.size != 2) {
                        errMalformedParseTree("Expected MEMBER node to have 2 children but found ${it.children.size}")
                    }
                    val keyExpr = it.children[0].toExprNode()
                    val valueExpr = it.children[1].toExprNode()
                    StructField(keyExpr, valueExpr)
            }
                Struct(fields, metas)
            }
            UNARY, BINARY, TERNARY -> {
                when(token!!.text) {
                    "is" -> {
                        Typed(
                            TypedOp.IS,
                            children[0].toExprNode(),
                            children[1].toDataType(),
                            metas)
                    }
                    "is_not" -> {
                         NAry(
                             NAryOp.NOT,
                             listOf(Typed(TypedOp.IS, children[0].toExprNode(), children[1].toDataType(), metas)),
                             metas.add(LegacyLogicalNotMeta.instance))
                    }
                    else -> {

                        val (opName, wrapInNot) = when (token.text) {
                            "not_between" -> Pair("between", true)
                            "not_like" -> Pair("like", true)
                            "not_in" -> Pair("in", true)
                            else -> Pair(token.text!!, false)
                        }

                        when (opName) {
                            "@"  -> {
                                val childNode = children[0]
                                val childToken = childNode.token ?: errMalformedParseTree("@ node does not have a token")
                                when(childToken.type) {
                                    QUOTED_IDENTIFIER                            -> {
                                        VariableReference(
                                            childNode.token.text!!,
                                            CaseSensitivity.SENSITIVE,
                                            ScopeQualifier.LEXICAL,
                                            childToken.toSourceLocationMetaContainer())
                                    }
                                    IDENTIFIER                                   -> {
                                        VariableReference(
                                            childNode.token.text!!,
                                            CaseSensitivity.INSENSITIVE,
                                            ScopeQualifier.LEXICAL,
                                            childToken.toSourceLocationMetaContainer())
                                    }
                                    else                                         -> {
                                        errMalformedParseTree("Unexpected child node token type of @ operator node ${childToken}")
                                    }
                                }
                            }
                            else -> {
                                val op = NAryOp.forSymbol(opName) ?: errMalformedParseTree("Unsupported operator: $opName")

                                val exprNode = NAry(op, children.map { it.toExprNode() }, metas)
                                if (!wrapInNot) {
                                    exprNode
                                } else {
                                    NAry(
                                        NAryOp.NOT,
                                        listOf(exprNode),
                                        metas.add(LegacyLogicalNotMeta.instance))
                                }
                            }
                        }
                    }
                }

            }
            CAST -> {
                val funcExpr = children[0].toExprNode()
                val dataType = children[1].toDataType()
                Typed(TypedOp.CAST, funcExpr, dataType, metas)
            }
            CALL -> {
                val funcName = token?.text!!.toLowerCase()

                when (funcName) {
                    "sexp", "list", "bag" -> {
                        // special case--list/sexp/bag "functions" are intrinsic to the literal form
                        val seqType = SeqType.values().firstOrNull { it.typeName == funcName }
                            ?: errMalformedParseTree("Cannot construct Seq node for functional call")
                        Seq(seqType,  children.map { it.toExprNode()}, metas)
                    }
                    else -> {
                        // Note:  we are forcing all function name lookups to be case insensitive here...
                        // This seems like the right thing to do because that is consistent with the
                        // previous behavior.
                        val funcExpr =
                            VariableReference(
                                funcName,
                                CaseSensitivity.INSENSITIVE,
                                metas = metaContainerOf())
                        NAry(NAryOp.CALL, listOf(funcExpr) + children.map { it.toExprNode() }, metas)
                    }
                }
            }
            CALL_AGG -> {
                val funcExpr =
                    VariableReference(
                        token?.text!!.toLowerCase(),
                        CaseSensitivity.INSENSITIVE,
                        metas = metaContainerOf())

                CallAgg(funcExpr, SetQuantifier.ALL, children.first().toExprNode(), metas)
            }
            CALL_DISTINCT_AGG -> {
                val funcExpr =
                    VariableReference(
                        token?.text!!.toLowerCase(),
                        CaseSensitivity.INSENSITIVE,
                        metas = metaContainerOf())

                CallAgg(funcExpr, SetQuantifier.DISTINCT, children.first().toExprNode(), metas)
            }
            CALL_AGG_WILDCARD -> {
                if(token!!.type != KEYWORD || token.keywordText != "count") {
                    errMalformedParseTree("only COUNT can be used with a wildcard")
                }
                val countStar = createCountStar(ion, metas)
                countStar
            }
            PATH -> {
                val rootExpr = children[0].toExprNode()
                val pathComponents = children.drop(1).map {
                    when(it.type) {
                        PATH_DOT -> {
                            if(it.children.size != 1) {
                                errMalformedParseTree("Unexpected number of child elements in PATH_DOT ParseNode")
                            }
                            val atomParseNode = it.children.first()
                            val atomMetas = atomParseNode.token.toSourceLocationMetaContainer()
                            when(atomParseNode.type) {
                                CASE_SENSITIVE_ATOM, CASE_INSENSITIVE_ATOM -> {
                                    val sensitivity = if(atomParseNode.type  == CASE_SENSITIVE_ATOM)
                                        CaseSensitivity.SENSITIVE
                                    else {
                                        CaseSensitivity.INSENSITIVE
                                    }
                                    PathComponentExpr(
                                        Literal(
                                            ion.newString(atomParseNode.token?.text!!),
                                            atomMetas),
                                        sensitivity)
                                }
                                PATH_UNPIVOT -> {
                                    PathComponentUnpivot(atomMetas)
                                }
                                else -> errMalformedParseTree("Unsupported child path node of PATH_DOT")
                            }
                        }
                        PATH_SQB -> {
                            if(it.children.size != 1) {
                                errMalformedParseTree("Unexpected number of child elements in PATH_SQB ParseNode")
                            }
                            val child = it.children.first()
                            val childMetas = child.token.toSourceLocationMetaContainer()
                            if(child.type == PATH_WILDCARD) {
                                PathComponentWildcard(childMetas)
                            } else {
                                PathComponentExpr(child.toExprNode(), CaseSensitivity.SENSITIVE)
                            }
                        }
                        else -> {
                            errMalformedParseTree("Unsupported path component: ${it.type}")
                        }
                    }//.copy(token.toSourceLocationMetaContainer())
                }
                Path(rootExpr, pathComponents, metas)
            }
            PARAMETER -> {
                Parameter(token!!.value!!.asIonInt().intValue(), metas)
            }
            CASE -> {
                when (children.size) {
                    // Searched CASE
                    1 -> {
                        val branches = ArrayList<SearchedCaseWhen>()
                        var elseExpr: ExprNode? = null
                        children[0].children.forEach {
                            when(it.type) {
                                WHEN -> branches.add(
                                    SearchedCaseWhen(
                                        it.children[0].toExprNode(),
                                        it.children[1].toExprNode()))

                                ELSE -> elseExpr = it.children[0].toExprNode()
                                else -> errMalformedParseTree("CASE clause must be WHEN or ELSE")
                            }
                        }

                        SearchedCase(branches, elseExpr, metas)
                    }
                    // Simple CASE
                    2 -> {
                        val valueExpr = children[0].toExprNode()
                        val branches = ArrayList<SimpleCaseWhen>()
                        var elseExpr: ExprNode? = null
                        children[1].children.forEach {
                            when(it.type) {
                                WHEN -> branches.add(
                                    SimpleCaseWhen(
                                        it.children[0].toExprNode(),
                                        it.children[1].toExprNode()))

                                ELSE -> elseExpr = it.children[0].toExprNode()
                                else -> errMalformedParseTree("CASE clause must be WHEN or ELSE")
                            }
                        }

                        SimpleCase(valueExpr, branches, elseExpr, metas)
                    }
                    else -> errMalformedParseTree("CASE must be searched or simple")
                }
            }
            FROM -> {
                // The first child is the operation, the second child is the from list,
                // each child following is an optional clause (e.g. ORDER BY)

                val operation = children[0].toExprNode()
                val fromSource = children[1].also {
                    if (it.type != FROM_CLAUSE) {
                        errMalformedParseTree("Invalid second child of FROM")
                    }

                    if (it.children.size != 1) {
                        errMalformedParseTree("Invalid FROM clause children length")
                    }
                }.children[0].toFromSource()

                val where = children.firstOrNull { it.type == WHERE }?.let { it.children[0].toExprNode() }

                when (operation) {
                    is DataManipulation -> {
                        operation.copy(from = fromSource, where = where, metas = metas)
                    }
                    else -> {
                        errMalformedParseTree("Unsupported operation for FROM expression")
                    }
                }
            }
            INSERT -> {
                DataManipulation(InsertOp(children[0].toExprNode(), children[1].toExprNode()), metas = metas)
            }
            INSERT_VALUE -> {
                val position = children.getOrNull(2)?.toExprNode()
                DataManipulation(InsertValueOp(children[0].toExprNode(), children[1].toExprNode(), position), metas = metas)
            }
            SET, UPDATE -> {
                val assignments =
                    children
                        .map { Assignment(it.children[0].toExprNode(), it.children[1].toExprNode()) }
                        .toList()
                DataManipulation(AssignmentOp(assignments), metas = metas)
            }
            REMOVE -> {
                DataManipulation(RemoveOp(children[0].toExprNode()), metas = metas)
            }
            DELETE -> {
                DataManipulation(DeleteOp(), metas = metas)
            }
            SELECT_LIST, SELECT_VALUE, PIVOT -> {
                // The first child of a SELECT_LIST parse node and be either DISTINCT or ARG_LIST.
                // If it is ARG_LIST, the children of that node are the select items and the SetQuantifier is ALL
                // If it is DISTINCT, the SetQuantifier is DISTINCT and there should be one child node, an ARG_LIST
                // containing the select items.

                // The second child of a SELECT_LIST is always an ARG_LIST containing the from clause.

                // GROUP BY, GROUP PARTIAL BY, WHERE, HAVING and limit parse nodes each have distinct ParseNodeTypes
                // and if present, exist in children, starting at the third position.

                var setQuantifier = SetQuantifier.ALL
                var selectList = children[0]
                val fromList = children[1]

                // We will remove items from this collection as we consume them.
                // If any unconsumed children remain, we've missed something and should throw an exception.
                val unconsumedChildren = children.drop(2).toMutableList()

                // If the query parsed was a `SELECT DISTINCT ...`, children[0] is of type DISTINCT and its
                // children are the actual select list.
                if(selectList.type == DISTINCT) {
                    selectList = selectList.children[0]
                    setQuantifier = SetQuantifier.DISTINCT
                }

                val projection = when(type) {
                    SELECT_LIST -> {
                        val selectListItems = selectList.children.map { it.toSelectListItem() }
                        SelectProjectionList(selectListItems)
                    }
                    SELECT_VALUE -> {
                        SelectProjectionValue(selectList.toExprNode())
                    }
                    PIVOT -> {
                        val member = children[0]
                        val asExpr = member.children[0].toExprNode()
                        val atExpr = member.children[1].toExprNode()
                        SelectProjectionPivot(asExpr, atExpr)
                    }
                    else -> {
                        throw IllegalStateException("This can never happen!")
                    }
                }

                if (fromList.type != FROM_CLAUSE) {
                    errMalformedParseTree("Invalid second child of SELECT_LIST")
                }

                if (fromList.children.size != 1) {
                    errMalformedParseTree("Invalid FROM clause children length")
                }

                val fromSource = fromList.children[0].toFromSource()

                val fromLet = unconsumedChildren.firstOrNull { it.type == LET }?.let {
                    unconsumedChildren.remove(it)
                    it.toLetSource()
                }

                val whereExpr = unconsumedChildren.firstOrNull { it.type == WHERE }?.let {
                    unconsumedChildren.remove(it)
                    it.children[0].toExprNode()
                }

                val groupBy = unconsumedChildren.firstOrNull { it.type == GROUP || it.type == GROUP_PARTIAL }?.let {
                    unconsumedChildren.remove(it)
                    val groupingStrategy = when(it.type) {
                        GROUP -> GroupingStrategy.FULL
                        else -> GroupingStrategy.PARTIAL
                    }

                    val groupAsName = if(it.children.size > 1) {
                        it.children[1].toSymbolicName()
                    } else {
                        null
                    }

                    GroupBy(
                        groupingStrategy,
                        it.children[0].children.map {
                            val (alias, groupByItemNode) = it.unwrapAsAlias()
                            GroupByItem(
                                groupByItemNode.toExprNode(),
                                alias)
                        },
                        groupAsName)
                }

                val havingExpr = unconsumedChildren.firstOrNull { it.type == HAVING }?.let {
                    unconsumedChildren.remove(it)
                    it.children[0].toExprNode()
                }

                val limitExpr = unconsumedChildren.firstOrNull { it.type == LIMIT }?.let {
                    unconsumedChildren.remove(it)
                    it.children[0].toExprNode()
                }

                if(!unconsumedChildren.isEmpty()) {
                    errMalformedParseTree("Unprocessed query components remaining")
                }

                Select(
                    setQuantifier = setQuantifier,
                    projection = projection,
                    from = fromSource,
                    fromLet = fromLet,
                    where = whereExpr,
                    groupBy = groupBy,
                    having = havingExpr,
                    limit = limitExpr,
                    metas = metas)
            }
            CREATE_TABLE -> {
                val name = children.first().token!!.text!!
                CreateTable(name, metas = metas)
            }
            DROP_TABLE -> {
                val name = children.first().token!!.text!!
                DropTable(name, metas = metas)
            }
            CREATE_INDEX -> {
                val tableName = children[0].token!!.text!!
                val keys = children[1].children.map { it.toExprNode() }
                CreateIndex(tableName, keys, metas = metas)
            }
            DROP_INDEX -> {
                val identifier = children[0].toExprNode() as VariableReference
                val tableName = children[1].token!!.text!!
                DropIndex(tableName, identifier, metas = metas)
            }
            else -> unsupported("Unsupported syntax for $type", PARSE_UNSUPPORTED_SYNTAX)
        }
    }

    private data class AsAlias(val name: SymbolicName?, val node: ParseNode)

    /**
     * Unwraps select list items that have been wrapped in an annotating node containing the `AS <alias>`,
     * if present.
     */
    private fun ParseNode.unwrapAsAlias(): AsAlias =
        if(type == AS_ALIAS) {
            AsAlias(SymbolicName(token!!.text!!, token.toSourceLocationMetaContainer()), children[0])
        } else {
            AsAlias(null, this)
        }

    private fun ParseNode.toSelectListItem(): SelectListItem {
        val metas = token.toSourceLocationMetaContainer()
        return when (type) {
            PROJECT_ALL -> {
                if (children.isEmpty()) {
                    SelectListItemStar(metas)
                }
                else {
                    val expr = children[0].toExprNode()
                    SelectListItemProjectAll(expr)
                }

            }
            else        -> {
                val (asAliasSymbol, parseNode) = unwrapAsAlias()
                SelectListItemExpr(parseNode.toExprNode(), asAliasSymbol)
            }
        }
    }

    private fun ParseNode.unwrapAliases(
        variables: LetVariables = LetVariables()
    ): Pair<LetVariables, ParseNode> {

        val metas = token.toSourceLocationMetaContainer()
        return when (type) {
            AS_ALIAS -> {
                if(variables.asName != null) error("Invalid parse tree: AS_ALIAS encountered more than once in FROM source")
                children[0].unwrapAliases(variables.copy(asName = SymbolicName(token!!.text!!, metas)))
            }
            AT_ALIAS -> {
                if(variables.atName != null) error("Invalid parse tree: AT_ALIAS encountered more than once in FROM source")
                children[0].unwrapAliases(variables.copy(atName = SymbolicName(token!!.text!!, metas)))
            }
            BY_ALIAS -> {
                if(variables.byName != null) error("Invalid parse tree: BY_ALIAS encountered more than once in FROM source")
                children[0].unwrapAliases(variables.copy(byName = SymbolicName(token!!.text!!, metas)))
            }
            else -> {
                return Pair(variables, this)
            }
        }
    }

    private fun ParseNode.toFromSource(): FromSource {
        val head = this
        if (head.type == FROM_SOURCE_JOIN) {
            val isCrossJoin = head.token?.keywordText?.contains("cross") ?: false
            if (!isCrossJoin && head.children.size != 3) {
                head.errMalformedParseTree("Incorrect number of clauses provided to JOIN")
            }

            val joinTokenType = head.token?.keywordText
            val joinOp = when (joinTokenType) {
                "inner_join", "join", "cross_join" -> JoinOp.INNER
                "left_join", "left_cross_join" -> JoinOp.LEFT
                "right_join", "right_cross_join" -> JoinOp.RIGHT
                "outer_join", "outer_cross_join" -> JoinOp.OUTER
                else -> {
                    head.errMalformedParseTree("Unsupported syntax for ${head.type}")
                }
            }

            val condition = when {
                isCrossJoin -> Literal(trueValue, metaContainerOf())
                else -> head.children[2].toExprNode()
            }

            return FromSourceJoin(joinOp,
                    head.children[0].toFromSource(),
                    head.children[1].unwrapAliasesAndUnpivot(),
                    condition,
                    token.toSourceLocationMetaContainer().let {
                    when {
                            isCrossJoin -> it.add(IsImplictJoinMeta.instance)
                            else        -> it
                        }
                    }
            )
        }
        return head.unwrapAliasesAndUnpivot()
    }

    private fun ParseNode.toLetSource(): LetSource {
        val letBindings = this.children.map { it.toLetBinding() }
        return LetSource(letBindings)
    }

    private fun ParseNode.toLetBinding(): LetBinding {
        val (asAliasSymbol, parseNode) = unwrapAsAlias()
        if (asAliasSymbol == null) {
            this.errMalformedParseTree("Unsupported syntax for ${this.type}")
        }
        else {
            return LetBinding(parseNode.toExprNode(), asAliasSymbol)
        }
    }

    private fun ParseNode.unwrapAliasesAndUnpivot(): FromSource {
        val (aliases, unwrappedParseNode) = unwrapAliases()
        return when(unwrappedParseNode.type) {
            UNPIVOT -> {
                val expr = unwrappedParseNode.children[0].toExprNode()
                FromSourceUnpivot(
                    expr,
                    aliases,
                    unwrappedParseNode.token.toSourceLocationMetaContainer())
            }
            else    -> {
                FromSourceExpr(unwrappedParseNode.toExprNode(), aliases)
            }
        }
    }

    private fun ParseNode.toDataType(): DataType {
        if(type != TYPE) {
            errMalformedParseTree("Expected ParseType.TYPE instead of $type")
        }
        val sqlDataType = SqlDataType.forTypeName(token!!.keywordText!!)
        if(sqlDataType == null) {
            errMalformedParseTree("Invalid DataType: ${token.keywordText!!}")
        }

        return DataType(
            sqlDataType,
            children.mapNotNull { it.token?.value?.longValue() },
            metas = token.toSourceLocationMetaContainer())
    }

    /**********************************************************************************************
     * Parse logic below this line.
     **********************************************************************************************/

    // keywords that IN (<expr>) evaluate more like grouping than a singleton in value list
    private val IN_OP_NORMAL_EVAL_KEYWORDS = setOf("select", "values")

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
        while (rem.isNotEmpty() && precedence < headPrecedence()) {
            val op = rem.head!!
            if (!op.isBinaryOperator && op.keywordText !in SPECIAL_INFIX_OPERATORS) {
                // unrecognized operator
                break
            }

            fun parseRightExpr() = if (rem.size < 3) {
                rem.err(
                    "Missing right-hand side expression of infix operator",
                    PARSE_EXPECTED_EXPRESSION
                )
            } else {
                rem.tail.parseExpression(
                    precedence = op.infixPrecedence
                )
            }

            val right = when (op.keywordText) {
                // IS/IS NOT requires a type
                "is", "is_not" -> rem.tail.parseType()
                // IN has context sensitive parsing rules around parenthesis
                "in", "not_in" -> when {
                    rem.tail.head?.type == LEFT_PAREN
                            && rem.tail.tail.head?.keywordText !in IN_OP_NORMAL_EVAL_KEYWORDS ->
                        rem.tail.tail.parseArgList(
                            aliasSupportType = NONE,
                            mode = NORMAL_ARG_LIST
                        ).deriveExpected(RIGHT_PAREN).copy(LIST)
                    else -> parseRightExpr()
                }
                else -> parseRightExpr()
            }
            rem = right.remaining

            expr = when {
                op.isBinaryOperator -> ParseNode(BINARY, op, listOf(expr, right), rem)
                else -> when (op.keywordText) {
                    "between", "not_between" -> {
                        val rest = rem.tailExpectedKeyword("and")
                        if (rest.onlyEndOfStatement()) {
                            rem.head.err("Expected expression after AND", PARSE_EXPECTED_EXPRESSION)
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
                        when {
                            rem.head?.keywordText == "escape" -> {
                                val rest = rem.tailExpectedKeyword("escape")
                                if (rest.onlyEndOfStatement()) {
                                    rem.head.err("Expected expression after ESCAPE", PARSE_EXPECTED_EXPRESSION)
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
                    else -> rem.err("Unknown infix operator", PARSE_UNKNOWN_OPERATOR)
                }
            }
        }
        return expr
    }

    private fun List<Token>.parseUnaryTerm(): ParseNode =
        when (head?.isUnaryOperator) {
            true -> {
                val op = head!!

                val term = tail.parseUnaryTerm()
                var expr: ParseNode? = null

                // constant fold unary plus/minus into constant literals
                when (op.keywordText) {
                    "+" -> when {
                        term.isNumericLiteral -> {
                            // unary plus is a NO-OP
                            expr = term
                        }
                    }
                    "-" -> when {
                        term.isNumericLiteral -> {
                            val num = -term.numberValue()
                            expr = ParseNode(ATOM,
                                             term.token!!.copy(value = num.ionValue(ion)),
                                             emptyList(),
                                             term.remaining)
                        }
                    }
                    "not" -> {
                        val children = tail.parseExpression(op.prefixPrecedence)
                        expr = ParseNode(UNARY, op, listOf(children), children.remaining)
                    }
                }

                expr ?: ParseNode(UNARY, op, listOf(term), term.remaining)
            }
            else -> parsePathTerm()
        }


    private fun List<Token>.parsePathTerm(pathMode: PathMode = PathMode.FULL_PATH): ParseNode {
        val term = when (pathMode) {
            PathMode.FULL_PATH -> parseTerm()
            PathMode.SIMPLE_PATH -> when (head?.type) {
                QUOTED_IDENTIFIER, IDENTIFIER -> atomFromHead()
                else -> err("Expected identifier for simple path", PARSE_INVALID_PATH_COMPONENT)
            }
        }
        val path = ArrayList<ParseNode>(listOf(term))
        var rem = term.remaining

        var hasPath = true
        while (hasPath) {
            when (rem.head?.type) {
                DOT -> {
                    val dotToken = rem.head!!
                    // consume first dot
                    rem = rem.tail
                    val pathPart = when (rem.head?.type) {
                        IDENTIFIER       -> {
                            val litToken = Token(LITERAL, ion.newString(rem.head?.text!!), rem.head!!.position)
                            ParseNode(CASE_INSENSITIVE_ATOM, litToken, emptyList(), rem.tail)
                        }
                        QUOTED_IDENTIFIER -> {
                            val litToken = Token(LITERAL, ion.newString(rem.head?.text!!), rem.head!!.position)
                            ParseNode(CASE_SENSITIVE_ATOM, litToken, emptyList(), rem.tail)
                        }
                        STAR              -> {
                            if (pathMode != PathMode.FULL_PATH) {
                                rem.err("Invalid path dot component for simple path", PARSE_INVALID_PATH_COMPONENT)
                            }
                            ParseNode(PATH_UNPIVOT, rem.head, emptyList(), rem.tail)
                        }
                        else              -> {
                            rem.err("Invalid path dot component", PARSE_INVALID_PATH_COMPONENT)
                        }
                    }
                    path.add(ParseNode(PATH_DOT, dotToken, listOf(pathPart), rem))
                    rem = rem.tail
                }
                LEFT_BRACKET -> {
                    val leftBracketToken = rem.head!!
                    rem = rem.tail
                    val expr = when (rem.head?.type) {
                        STAR -> ParseNode(PATH_WILDCARD, rem.head, emptyList(), rem.tail)
                        else -> rem.parseExpression()
                    }.deriveExpected(RIGHT_BRACKET)
                    if (pathMode == PathMode.SIMPLE_PATH && expr.type != ATOM && expr.token?.type != LITERAL) {
                        rem.err("Invalid path component for simple path", PARSE_INVALID_PATH_COMPONENT)
                    }

                    path.add(ParseNode(PATH_SQB, leftBracketToken, listOf(expr), rem.tail))
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
                IDENTIFIER, QUOTED_IDENTIFIER -> ParseNode(
                    UNARY,
                    head,
                    listOf(tail.atomFromHead()),
                    tail.tail
                )
                else -> err("Identifier must follow @-operator", PARSE_MISSING_IDENT_AFTER_AT)
            }
            else -> err("Unexpected operator", PARSE_UNEXPECTED_OPERATOR)
        }

        KEYWORD -> when (head?.keywordText) {
            in BASE_DML_KEYWORDS -> parseBaseDml()
            "update" -> tail.parseUpdate()
            "delete" -> tail.parseDelete()
            "case" -> when (tail.head?.keywordText) {
                "when" -> tail.parseCase(isSimple = false)
                else -> tail.parseCase(isSimple = true)
            }
            "cast" -> tail.parseCast()
            "select" -> tail.parseSelect()
            "create" -> tail.parseCreate()
            "drop" -> tail.parseDrop()
            "pivot" -> tail.parsePivot()
            "from" -> tail.parseFrom()
            // table value constructor--which aliases to bag constructor in PartiQL with very
            // specific syntax
            "values" -> tail.parseTableValues().copy(type = BAG)
            "substring" -> tail.parseSubstring(head!!)
            "trim" -> tail.parseTrim(head!!)
            "extract" -> tail.parseExtract(head!!)
            "date_add", "date_diff" -> tail.parseDateAddOrDateDiff(head!!)
            in FUNCTION_NAME_KEYWORDS -> when (tail.head?.type) {
                LEFT_PAREN ->
                    tail.tail.parseFunctionCall(head!!)
                else -> err("Unexpected keyword", PARSE_UNEXPECTED_KEYWORD)
            }
            else -> err("Unexpected keyword", PARSE_UNEXPECTED_KEYWORD)
        }
        LEFT_PAREN -> {
            val group = tail.parseArgList(
                aliasSupportType = NONE,
                mode = NORMAL_ARG_LIST
            ).deriveExpected(RIGHT_PAREN)
            when (group.children.size) {
                0 -> tail.err("Expression group cannot be empty", PARSE_EXPECTED_EXPRESSION)
            // expression grouping
                1 -> group.children[0].copy(remaining = group.remaining)
            // row value constructor--which aliases to list constructor in PartiQL
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
        IDENTIFIER, QUOTED_IDENTIFIER -> when (tail.head?.type) {
            LEFT_PAREN -> tail.tail.parseFunctionCall(head!!)
            else -> atomFromHead()
        }
        QUESTION_MARK -> ParseNode(PARAMETER, head!!, listOf(), tail)
        LITERAL, NULL, MISSING, TRIM_SPECIFICATION -> atomFromHead()
        else -> err("Unexpected term", PARSE_UNEXPECTED_TERM)
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
            err("Expected a WHEN clause in CASE", PARSE_EXPECTED_WHEN_CLAUSE)
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
            err("Missing left parenthesis after CAST", PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST)
        }
        val valueExpr = tail.parseExpression().deriveExpected(AS)
        var rem = valueExpr.remaining

        val typeNode = rem.parseType().deriveExpected(RIGHT_PAREN)
        rem = typeNode.remaining

        return ParseNode(CAST, head, listOf(valueExpr, typeNode), rem)
    }

    private fun List<Token>.parseType(): ParseNode {
        val typeName = head?.keywordText
        val typeArity = TYPE_NAME_ARITY_MAP[typeName] ?: err("Expected type name", PARSE_EXPECTED_TYPE_NAME)

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
            val pvmap = PropertyValueMap()
            pvmap[CAST_TO] = typeName?: ""
            pvmap[EXPECTED_ARITY_MIN] = typeArity.first
            pvmap[EXPECTED_ARITY_MAX] = typeArity.last
            tail.err("CAST for $typeName must have arity of $typeArity", PARSE_CAST_ARITY, pvmap)
        }
        for (child in typeNode.children) {
            if (child.type != ATOM
                || child.token?.type != LITERAL
                || child.token.value?.isUnsignedInteger != true) {
                err("Type parameter must be an unsigned integer literal", PARSE_INVALID_TYPE_PARAM)
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

        // TODO support ORDER BY and LIMIT (and full select sub-clauses)

        // TODO determine if DML l-value should be restricted to paths...
        // TODO support the FROM ... SELECT forms
        val operation = rem.parseBaseDml()
        rem = operation.remaining

        return ParseNode(FROM, null, listOf(operation, fromList) + children, rem)
    }

    private fun List<Token>.parseBaseDml(): ParseNode {
        var rem = this
        return when (rem.head?.keywordText) {
            "insert_into" -> {
                val lvalue = rem.tail.parsePathTerm(PathMode.SIMPLE_PATH)
                rem = lvalue.remaining

                if ("value" == rem.head?.keywordText) {
                    val value = rem.tail.parseExpression()
                    rem = value.remaining

                    if ("at" == rem.head?.keywordText) {
                        val position = rem.tail.parseExpression()
                        ParseNode(INSERT_VALUE, null, listOf(lvalue, value, position), position.remaining)
                    } else {
                        ParseNode(INSERT_VALUE, null, listOf(lvalue, value), value.remaining)
                    }
                } else {
                    val values = rem.parseExpression()
                    ParseNode(INSERT, null, listOf(lvalue, values), values.remaining)
                }
            }
            "set" -> rem.tail.parseSetAssignments(UPDATE)
            "remove" -> {
                val lvalue = rem.tail.parsePathTerm(PathMode.SIMPLE_PATH)
                rem = lvalue.remaining
                ParseNode(REMOVE, null, listOf(lvalue), rem)
            }
            else -> err("Expected data manipulation", PARSE_MISSING_OPERATION)
        }
    }

    private fun List<Token>.parseSetAssignments(type: ParseType): ParseNode = parseArgList(
        aliasSupportType = NONE,
        mode = SET_CLAUSE_ARG_LIST
    ).run {
        if (children.isEmpty()) {
            remaining.err("Expected assignment for SET", PARSE_MISSING_SET_ASSIGNMENT)
        }
        copy(type = type)
    }

    private fun List<Token>.parseDelete(): ParseNode {
        if (head?.keywordText != "from") {
            err("Expected FROM after DELETE", PARSE_UNEXPECTED_TOKEN)
        }

        return tail.parseLegacyDml { ParseNode(DELETE, null, emptyList(), this) }
    }

    private fun List<Token>.parseUpdate(): ParseNode = parseLegacyDml {
        parseBaseDml()
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

        children.add(ParseNode(FROM_CLAUSE, null, listOf(source), rem))

        val operation = rem.parseDmlOp().also {
            rem = it.remaining
        }

        rem.parseOptionalWhere()?.let {
            children.add(it)
            rem = it.remaining
        }

        // generate a FROM-node to normalize the parse tree
        return ParseNode(FROM, null, listOf(operation) + children, rem)
    }

    private fun List<Token>.parseOptionalWhere(): ParseNode? {
        var rem = this

        // TODO consolidate this logic with the SELECT logic
        if (rem.head?.keywordText == "where") {
            val expr = rem.tail.parseExpression()
            rem = expr.remaining
            return ParseNode(WHERE, null, listOf(expr), rem)
        }

        return null
    }

    private fun List<Token>.parsePivot(): ParseNode {
        var rem = this
        val value = rem.parseExpression().deriveExpectedKeyword("at")
        rem = value.remaining
        val name = rem.parseExpression()
        rem = name.remaining
        val selectAfterProjection = parseSelectAfterProjection(PIVOT,ParseNode(MEMBER, null, listOf(name, value), rem))
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

        var type = SELECT_LIST
        var projection = when {
            rem.head?.keywordText == "value" -> {
                type = SELECT_VALUE
                rem.tail.parseExpression()
            }
            else -> {
                val list = rem.parseSelectList()
                if (list.children.isEmpty()) {
                    rem.err("Cannot have empty SELECT list", PARSE_EMPTY_SELECT)
                }

                val asterisk = list.children.firstOrNull { it.type == ParseType.PROJECT_ALL && it.children.isEmpty() }
                if(asterisk != null
                   && list.children.size > 1) {
                    asterisk.token.err(
                        "Other expressions may not be present in the select list when '*' is used without dot notation.",
                        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST)
                }

                list
            }
        }
        if (distinct) {
            projection = ParseNode(DISTINCT, null, listOf(projection), projection.remaining)
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
            QUOTED_IDENTIFIER, IDENTIFIER -> {
                atomFromHead()
            }
            else -> {
                err("Expected identifier!", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        return ParseNode(CREATE_TABLE, null, listOf(identifier), identifier.remaining)
    }

    private fun List<Token>.parseDropIndex(): ParseNode {
        var rem = this

        val identifier = when (rem.head?.type) {
            IDENTIFIER, QUOTED_IDENTIFIER -> {
                atomFromHead()
            }
            else -> {
                rem.err("Expected identifier!", PARSE_UNEXPECTED_TOKEN)
            }
        }
        rem = rem.tail

        if (rem.head?.keywordText != "on") {
            rem.err("Expected ON", PARSE_UNEXPECTED_TOKEN)
        }
        rem = rem.tail

        val target = when (rem.head?.type) {
            QUOTED_IDENTIFIER, IDENTIFIER -> {
                rem.atomFromHead()
            }
            else -> {
                rem.err("Table target must be an identifier", PARSE_UNEXPECTED_TOKEN)
            }
        }
        rem = rem.tail

        return ParseNode(DROP_INDEX, null, listOf(identifier, target), rem)
    }

    /**
     * This is currently parsing only the most naïve `DROP TABLE <name>` statement.
     */
    private fun List<Token>.parseDropTable(): ParseNode {
        val identifier = when (head?.type) {
            QUOTED_IDENTIFIER, IDENTIFIER -> {
                atomFromHead()
            }
            else -> {
                err("Expected identifier!", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }

        return ParseNode(DROP_TABLE, null, listOf(identifier), identifier.remaining)
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
            QUOTED_IDENTIFIER, IDENTIFIER -> {
                rem.atomFromHead()
            }
            else -> {
                rem.err("Index target must be an identifier", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        rem = target.remaining

        if (rem.head?.type != LEFT_PAREN) {
            rem.err("Expected parenthesis for keys", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
        // TODO support full expressions here... only simple paths for now
        val keys = rem.tail.parseArgList(NONE, SIMPLE_PATH_ARG_LIST).deriveExpected(RIGHT_PAREN)
        rem = keys.remaining

        // TODO support other syntax options
        return ParseNode(CREATE_INDEX, null, listOf(target, keys), rem)
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
                n.children.forEach { doFlatten(it,l ) }
            }
            val list = mutableListOf<ParseNode>()
            doFlatten(node, list)
            return list
        }

        val flattened = flattenParseNode(pathNode).drop(2)

        //Is invalid if contains PATH_WILDCARD (i.e. to `[*]`}
        flattened.firstOrNull { it.type == PATH_WILDCARD }
            ?.token
            ?.err("Invalid use of * in select list", ErrorCode.PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST)

        //Is invalid if contains PATH_WILDCARD_UNPIVOT (i.e. * as part of a dotted expression) anywhere except at the end.
        //i.e. f.*.b is invalid but f.b.* is not.
        flattened.dropLast(1).firstOrNull { it.type == PATH_UNPIVOT }
            ?.token
            ?.err("Invalid use of * in select list", ErrorCode.PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST)

        //If the last path component expression is a *, then the PathType is a wildcard and we need to do one
        //additional check.
        if(flattened.last().type == ParseType.PATH_UNPIVOT) {

            //Is invalid if contains a square bracket anywhere and a wildcard at the end.
            //i.e f[1].* is invalid
            flattened.firstOrNull { it.type == PATH_SQB }
                ?.token
                ?.err("Cannot use [] and * together in SELECT list expression", ErrorCode.PARSE_CANNOT_MIX_SQB_AND_WILDCARD_IN_SELECT_LIST)

            val pathPart = pathNode.copy(children = pathNode.children.dropLast(1))

            return ParseNode(
                type = PROJECT_ALL,
                token = null,
                children = listOf(if (pathPart.children.size == 1) pathPart.children[0] else pathPart),
                remaining = pathNode.remaining)
        }
        return pathNode
    }


    private fun List<Token>.parseSelectList(): ParseNode {
        return parseCommaList {
            if (this.head?.type == STAR) {
                ParseNode(PROJECT_ALL, this.head, listOf(), this.tail)
            }
            else {
                val expr = parseExpression().let {
                    when (it.type) {
                        PATH -> inspectPathExpression(it)
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
            rem.err("Expected FROM after SELECT list", PARSE_SELECT_MISSING_FROM)
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

            rem = rem.tailExpectedToken(BY)

            val groupKey = rem.parseArgList(
                aliasSupportType = AS_ONLY,
                mode = NORMAL_ARG_LIST,
                precedence = OperatorPrecedenceGroups.SELECT.precedence
            )
            groupKey.children.forEach {
                // TODO support ordinal case
                if (it.token?.type == LITERAL) {
                    it.token.err("Literals (including ordinals) not supported in GROUP BY", PARSE_UNSUPPORTED_LITERALS_GROUPBY)
                }
            }
            groupChildren.add(groupKey)
            rem = groupKey.remaining

            if (rem.head?.keywordText == "group") {
                rem = rem.tail.tailExpectedKeyword("as")

                if (rem.head?.type?.isIdentifier() != true) {
                    rem.err("Expected identifier for GROUP name", PARSE_EXPECTED_IDENT_FOR_GROUP_NAME)
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
        fun parseCallArguments(callName: String, args: List<Token>, callType: ParseType): ParseNode = when(args.head?.type) {
            STAR -> err("$callName(*) is not allowed", PARSE_UNSUPPORTED_CALL_WITH_STAR)
            RIGHT_PAREN -> ParseNode(callType, name, emptyList(), tail)
            else -> {
                args.parseArgList(aliasSupportType = NONE, mode = NORMAL_ARG_LIST)
                    .copy(type = callType, token = name)
                    .deriveExpected(RIGHT_PAREN)
            }
        }

        val callName = name.text!!
        val memoizedTail by lazy { tail }
        val keywordText = head?.keywordText

        return when (callName) {
            "count" -> {
                when {
                    head?.type == RIGHT_PAREN -> {
                        err("Aggregate functions are always unary", PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL)
                    }

                    // COUNT(*)
                    head?.type == STAR -> {
                        ParseNode(CALL_AGG_WILDCARD, name, emptyList(), tail).deriveExpected(RIGHT_PAREN)
                    }

                    head?.type == KEYWORD && keywordText == "distinct" -> {
                        when(memoizedTail.head?.type) {
                            // COUNT(DISTINCT *)
                            STAR -> {
                                err("COUNT(DISTINCT *) is not supported", PARSE_UNSUPPORTED_CALL_WITH_STAR)
                            }

                            // COUNT(DISTINCT expression)
                            else -> {
                                memoizedTail.parseArgList(aliasSupportType = NONE, mode = NORMAL_ARG_LIST)
                                    .copy(type = CALL_DISTINCT_AGG, token = name)
                                    .deriveExpected(RIGHT_PAREN)
                            }
                        }
                    }

                    head?.type == KEYWORD && keywordText == "all" -> {
                        when(memoizedTail.head?.type) {
                            STAR -> err("COUNT(ALL *) is not supported", PARSE_UNSUPPORTED_CALL_WITH_STAR)

                            // COUNT(ALL expression)
                            else -> {
                                memoizedTail.parseArgList(aliasSupportType = NONE, mode = NORMAL_ARG_LIST)
                                    .copy(type = CALL_AGG, token = name)
                                    .deriveExpected(RIGHT_PAREN)
                            }
                        }
                    }

                    else -> parseArgList(aliasSupportType = NONE, mode = NORMAL_ARG_LIST)
                        .copy(type = CALL_AGG, token = name)
                        .deriveExpected(RIGHT_PAREN)
                }
            }
            in STANDARD_AGGREGATE_FUNCTIONS -> {

                val call = when {
                    head?.type == KEYWORD && head?.keywordText == "distinct" -> {
                        parseCallArguments(callName, tail, CALL_DISTINCT_AGG)
                    }
                    head?.type == KEYWORD && head?.keywordText == "all" -> {
                        parseCallArguments(callName, tail, CALL_AGG)
                    }
                    else -> {
                        parseCallArguments(callName, this, CALL_AGG)
                    }
                }

                if (call.children.size != 1) {
                    err("Aggregate functions are always unary", PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL)
                }

                call
            }

            // normal function
            else -> parseCallArguments(callName, this, CALL)
        }
    }

    /**
     * Parses substring
     *
     * Syntax is either SUBSTRING(<str> FROM <start position> [FOR <string length>])
     * or SUBSTRING(<str>, <start position> [, <string length>])
     */
    private fun List<Token>.parseSubstring(name: Token): ParseNode {
        var rem = this

        if (rem.head?.type != LEFT_PAREN) {
            val pvmap = PropertyValueMap()
            pvmap[EXPECTED_TOKEN_TYPE] = LEFT_PAREN
            rem.err("Expected $LEFT_PAREN", PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL, pvmap)
        }

        var stringExpr = tail.parseExpression()
        rem = stringExpr.remaining
        var parseSql92Syntax = false

        stringExpr = when {
            rem.head!!.keywordText == "from" -> {
                parseSql92Syntax = true
                stringExpr.deriveExpectedKeyword("from")
            }
            rem.head!!.type == COMMA -> stringExpr.deriveExpected(COMMA)
            else -> rem.err("Expected $KEYWORD 'from' OR $COMMA", PARSE_EXPECTED_ARGUMENT_DELIMITER)
        }

        val (positionExpr: ParseNode, expectedToken: Token) = stringExpr.remaining.parseExpression()
                .deriveExpected(if(parseSql92Syntax) FOR else COMMA, RIGHT_PAREN)

        if (expectedToken.type == RIGHT_PAREN) {
            return ParseNode(
                ParseType.CALL,
                name,
                listOf(stringExpr, positionExpr),
                positionExpr.remaining
            )
        }

        rem = positionExpr.remaining
        val lengthExpr = rem.parseExpression().deriveExpected(RIGHT_PAREN)
        return ParseNode(ParseType.CALL,
                name,
                listOf(stringExpr, positionExpr, lengthExpr),
                lengthExpr.remaining)

    }

    /**
     * Parses trim
     *
     * Syntax is TRIM([[ specification ] [to trim characters] FROM] <trim source>).
     */
    private fun List<Token>.parseTrim(name: Token): ParseNode {
        if (head?.type != LEFT_PAREN) err("Expected $LEFT_PAREN", PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL)

        var rem = tail
        val arguments = mutableListOf<ParseNode>()

        fun parseArgument(block: (ParseNode) -> ParseNode = { it }): List<Token> {
            val node = block(rem.parseExpression())
            arguments.add(node)

            return node.remaining
        }

        val hasSpecification = when(rem.head?.type) {
            TRIM_SPECIFICATION -> {
                rem = parseArgument()

                true
            }
            else -> false
        }

        if (hasSpecification) { // trim(spec [toRemove] from target)
            rem = when (rem.head?.keywordText) {
                "from" -> rem.tail
                else   -> parseArgument { it.deriveExpectedKeyword("from") }
            }

            rem = parseArgument()
        }
        else {
            if(rem.head?.keywordText == "from") { // trim(from target)
                rem = rem.tail // skips from

                rem = parseArgument()
            }
            else { // trim([toRemove from] target)
                rem = parseArgument()

                if(rem.head?.keywordText == "from") {
                    rem = rem.tail // skips from

                    rem = parseArgument()
                }
            }
        }

        if(rem.head?.type != RIGHT_PAREN) {
            rem.err("Expected $RIGHT_PAREN", PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL)
        }

        return ParseNode(ParseType.CALL, name, arguments, rem.tail)
    }

    private fun List<Token>.parseDatePart(): ParseNode {
        val maybeDatePart = this.head
        return when  {
            maybeDatePart?.type == IDENTIFIER && DATE_PART_KEYWORDS.contains(maybeDatePart.text?.toLowerCase()) -> {
                ParseNode(ATOM, maybeDatePart.copy(type = DATE_PART), listOf(), this.tail)
            }
            else -> maybeDatePart.err("Expected one of: $DATE_PART_KEYWORDS", PARSE_EXPECTED_DATE_PART)
        }
    }


    /**
     * Parses extract function call.
     *
     * Syntax is EXTRACT(<date_part> FROM <timestamp>).
     */
    private fun List<Token>.parseExtract(name: Token): ParseNode {
        if (head?.type != LEFT_PAREN) err("Expected $LEFT_PAREN",
                                          PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL)

        val datePart = this.tail.parseDatePart().deriveExpectedKeyword("from")
        val rem = datePart.remaining
        val timestamp = rem.parseExpression().deriveExpected(RIGHT_PAREN)

        return ParseNode(CALL, name, listOf(datePart, timestamp), timestamp.remaining)
    }

    /**
     * Parses a function call that has the syntax of `date_add` and `date_diff`.
     *
     * Syntax is <func>(<date_part>, <timestamp>, <timestamp>) where <func>
     * is the value of [name].
     */
    private fun List<Token>.parseDateAddOrDateDiff(name: Token): ParseNode {
        if (head?.type != LEFT_PAREN) err("Expected $LEFT_PAREN",
                                          PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL)

        val datePart = this.tail.parseDatePart().deriveExpected(COMMA)

        val timestamp1 = datePart.remaining.parseExpression().deriveExpected(COMMA)
        val timestamp2 = timestamp1.remaining.parseExpression().deriveExpected(RIGHT_PAREN)

        return ParseNode(CALL, name, listOf(datePart, timestamp1, timestamp2), timestamp2.remaining)
    }

    private fun List<Token>.parseLet(): ParseNode {
        val letClauses = ArrayList<ParseNode>()
        var rem = this.tail
        var child = rem.parseExpression()
        rem = child.remaining

        if (rem.head?.type != AS) {
            rem.head.err("Expected $AS following $LET expr", PARSE_EXPECTED_AS_FOR_LET)
        }

        rem = rem.tail

        if (rem.head?.type?.isIdentifier() != true) {
            rem.head.err("Expected identifier for $AS-alias", PARSE_EXPECTED_IDENT_FOR_ALIAS)
        }

        var name = rem.head
        rem = rem.tail
        letClauses.add(ParseNode(AS_ALIAS, name, listOf(child), rem))

        while (rem.head?.type == COMMA) {
            rem = rem.tail
            child = rem.parseExpression()
            rem = child.remaining
            if (rem.head?.type != AS) {
                rem.head.err("Expected $AS following $LET expr", PARSE_EXPECTED_AS_FOR_LET)
            }

            rem = rem.tail

            if (rem.head?.type?.isIdentifier() != true) {
                rem.head.err("Expected identifier for $AS-alias", PARSE_EXPECTED_IDENT_FOR_ALIAS)
            }

            name = rem.head

            rem = rem.tail
            letClauses.add(ParseNode(AS_ALIAS, name, listOf(child), rem))
        }
        return ParseNode(LET, null, letClauses, rem)
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
                err("Expected $LEFT_PAREN for row value constructor", PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR)
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
                "join", "cross_join", "inner_join" -> atomFromHead(INNER_JOIN)
                "left_join", "left_cross_join" -> atomFromHead(LEFT_JOIN)
                "right_join", "right_cross_join" -> atomFromHead(RIGHT_JOIN)
                "outer_join", "outer_cross_join" -> atomFromHead(OUTER_JOIN)
                else -> null
            }
            else -> null
        }
    }

    private fun List<Token>.parseFromSource(precedence: Int = -1, parseRemaining: Boolean = true): ParseNode {
        var rem = this
        var child = when (rem.head?.keywordText) {
            "unpivot" -> {
                val actualChild = rem.tail.parseExpression(precedence)
                ParseNode(
                        UNPIVOT,
                        rem.head,
                        listOf(actualChild),
                        actualChild.remaining
                )
            }
            else -> {
                val isSubqueryOrLiteral = rem.tail.head?.type == LITERAL || rem.tail.head?.keywordText == "select"
                if (rem.head?.type == LEFT_PAREN && !isSubqueryOrLiteral) {
                    // Starts with a left paren and is not a subquery or literal, so parse as a from source
                    rem = rem.tail
                    rem.parseFromSource(precedence).deriveExpected(RIGHT_PAREN)
                }
                else {
                    rem.parseExpression(precedence)
                }
            }
        }
        rem = child.remaining

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
                val hasOnClause = delim.token?.type == KEYWORD && !isCrossJoin
                var children : List<ParseNode>
                var joinToken : Token? = delim.token

                rem = rem.tail

                if (hasOnClause) {
                    // Explicit join
                    if (rem.head?.type == LEFT_PAREN) {
                        // Starts with a left paren. Could indicate subquery/literal or indicate higher precedence
                        val isSubqueryOrLiteral = rem.tail.head?.type == LITERAL || rem.tail.head?.keywordText == "select"
                        val parenClause = rem.parseFromSource(precedence, parseRemaining = true)
                        rem = parenClause.remaining

                        // check for an ON-clause
                        if (rem.head?.keywordText != "on") {
                            rem.err("Expected 'ON'", PARSE_MALFORMED_JOIN)
                        }

                        val onClause = rem.tail.parseExpression(precedence)

                        rem = onClause.remaining
                        if (!isSubqueryOrLiteral) {
                            children = listOf(parenClause, left, onClause)
                        }

                        else {
                            children = listOf(left, parenClause, onClause)
                        }
                    }

                    else {
                        // Rest is just the right side of the clause
                        val rightRef = rem.parseFromSource(precedence, parseRemaining = false)
                        rem = rightRef.remaining

                        // check for an ON-clause
                        if (rem.head?.keywordText != "on") {
                            rem.err("Expected 'ON'", PARSE_MALFORMED_JOIN)
                        }

                        val onClause = rem.tail.parseExpression(precedence)

                        rem = onClause.remaining

                        children = listOf(left, rightRef, onClause)
                    }
                }

                else {
                    // For implicit joins
                    val rightRef = rem.parseFromSource(precedence, parseRemaining = false)
                    rem = rightRef.remaining
                    children = listOf(left, rightRef)
                    if (delim.token?.type == COMMA) {
                        joinToken = delim.token?.copy(
                            type = KEYWORD,
                            value = ion.newSymbol("cross_join")
                        )
                    }
                }
                left = ParseNode(FROM_SOURCE_JOIN, joinToken, children, rem)
                delim = rem.parseJoinDelim()
            }
            return left
        }
        return child
    }


    private fun List<Token>.parseFromSourceList(precedence: Int = -1): ParseNode {
        val child = this.parseFromSource(precedence)
        return ParseNode(FROM_CLAUSE, null, listOf(child), child.remaining)
    }

    private fun List<Token>.parseArgList(
            aliasSupportType: AliasSupportType,
            mode: ArgListMode,
            precedence: Int = -1
    ): ParseNode {
        val parseDelim = parseCommaDelim

        return parseDelimitedList(parseDelim) { delim ->
            var rem = this
            var child = when (mode) {
                STRUCT_LITERAL_ARG_LIST -> {
                    val field = rem.parseExpression(precedence).deriveExpected(COLON)
                    rem = field.remaining
                    val value = rem.parseExpression(precedence)
                    ParseNode(MEMBER, null, listOf(field, value), value.remaining)
                }
                SIMPLE_PATH_ARG_LIST -> rem.parsePathTerm(PathMode.SIMPLE_PATH)
                SET_CLAUSE_ARG_LIST -> {
                    val lvalue = rem.parsePathTerm(PathMode.SIMPLE_PATH)
                    rem = lvalue.remaining
                    if (rem.head?.keywordText != "=") {
                        rem.err("Expected '='", PARSE_MISSING_SET_ASSIGNMENT)
                    }
                    rem = rem.tail
                    val rvalue = rem.parseExpression(precedence)
                    ParseNode(ASSIGNMENT, null, listOf(lvalue, rvalue), rvalue.remaining)
                }
                NORMAL_ARG_LIST -> rem.parseExpression(precedence)
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
                    rem.head.err("Expected identifier for $keywordTokenType-alias", PARSE_EXPECTED_IDENT_FOR_ALIAS)
                }
                rem = rem.tail
                ParseNode(parseNodeType, name, listOf(child), rem)
            }
            keywordIsOptional && rem.head?.type?.isIdentifier() ?: false -> {
                ParseNode(parseNodeType, rem.head, listOf(child), rem.tail)
            } else                                  -> {
                child
            }
        }
    }

    private fun List<Token>.parseOptionalAsAlias(child: ParseNode) =
        parseOptionalAlias(child = child, keywordTokenType = AS, keywordIsOptional = true, parseNodeType = AS_ALIAS)

    private fun List<Token>.parseOptionalAtAlias(child: ParseNode) =
        parseOptionalAlias(child = child, keywordTokenType = AT, keywordIsOptional = false, parseNodeType = AT_ALIAS)

    private fun List<Token>.parseOptionalByAlias(child: ParseNode) =
        parseOptionalAlias(child = child, keywordTokenType = BY, keywordIsOptional = false, parseNodeType = BY_ALIAS)

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
    override fun parseExprNode(source: String): ExprNode {
        val node = SqlLexer(ion).tokenize(source).parseExpression()
        val rem = node.remaining
        if (!rem.onlyEndOfStatement()) {
            when(rem.head?.type ) {
                SEMICOLON -> rem.tail.err("Unexpected token after semicolon. (Only one query is allowed.)",
                                          PARSE_UNEXPECTED_TOKEN)
                else      -> rem.err("Unexpected token after expression", PARSE_UNEXPECTED_TOKEN)
            }
        }
        return node.toExprNode()
    }

    /**
     * Parse given source node as a PartiqlAst.Statement
     */
    override fun parseAstStatement(source: String): PartiqlAst.Statement = parseExprNode(source).toAstStatement()

    override fun parse(source: String): IonSexp =
        AstSerializer.serialize(parseExprNode(source), AstVersion.V0, ion)
}

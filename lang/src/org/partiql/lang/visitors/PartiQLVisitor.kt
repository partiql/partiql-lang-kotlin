/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.visitors

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.toIonElement
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.time.MAX_PRECISION_FOR_TIME
import org.partiql.lang.generated.PartiQLBaseVisitor
import org.partiql.lang.generated.PartiQLParser
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.getPrecisionFromTimeString
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.runtime.asPrimitive
import java.math.BigInteger
import java.time.LocalTime
import java.time.OffsetTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Extends ANTLR's generated [PartiQLBaseVisitor] to visit an ANTLR ParseTree and convert it into a PartiQL AST. This
 * class uses the [PartiqlAst.PartiqlAstNode] to represent all nodes within the new AST.
 */
class PartiQLVisitor(val ion: IonSystem, val customTypes: List<CustomType> = listOf()) :
    PartiQLBaseVisitor<PartiqlAst.PartiqlAstNode>() {

    private val CUSTOM_KEYWORDS = customTypes.map { it.name.toLowerCase() }

    private val CUSTOM_TYPE_ALIASES =
        customTypes.map { customType ->
            customType.aliases.map { alias ->
                Pair(alias.toLowerCase(), customType.name.toLowerCase())
            }
        }.flatten().toMap()

    override fun visitSelectFromWhere(ctx: PartiQLParser.SelectFromWhereContext): PartiqlAst.Expr.Select {
        val projection = visit(ctx.selectClause()) as PartiqlAst.Projection
        val strategy = getSetQuantifierStrategy(ctx.selectClause())
        val from = visit(ctx.fromClause()) as PartiqlAst.FromSource
        val order = if (ctx.orderByClause() != null) visit(ctx.orderByClause()) as PartiqlAst.OrderBy else null
        val group = if (ctx.groupClause() != null) visit(ctx.groupClause()) as PartiqlAst.GroupBy else null
        val limit = if (ctx.limitClause() != null) visit(ctx.limitClause()) as PartiqlAst.Expr else null
        val offset = if (ctx.offsetByClause() != null) visit(ctx.offsetByClause()) as PartiqlAst.Expr else null
        val where = if (ctx.whereClause() != null) visit(ctx.whereClause()) as PartiqlAst.Expr else null
        val having = if (ctx.havingClause() != null) visit(ctx.havingClause()) as PartiqlAst.Expr else null
        val let = if (ctx.letClause() != null) visit(ctx.letClause()) as PartiqlAst.Let else null
        return PartiqlAst.BUILDER().select(
            project = projection,
            from = from,
            setq = strategy,
            order = order,
            group = group,
            limit = limit,
            offset = offset,
            where = where,
            having = having,
            fromLet = let
        )
    }

    override fun visitSelectAll(ctx: PartiQLParser.SelectAllContext): PartiqlAst.Projection.ProjectStar =
        PartiqlAst.BUILDER().projectStar()

    override fun visitSelectItems(ctx: PartiQLParser.SelectItemsContext): PartiqlAst.Projection.ProjectList =
        visitProjectionItems(ctx.projectionItems())

    override fun visitProjectionItems(ctx: PartiQLParser.ProjectionItemsContext): PartiqlAst.Projection.ProjectList {
        val projections = ctx.projectionItem().map { projection -> visit(projection) as PartiqlAst.ProjectItem }
        return PartiqlAst.BUILDER().projectList(projections)
    }

    override fun visitProjectionItem(ctx: PartiQLParser.ProjectionItemContext): PartiqlAst.ProjectItem.ProjectExpr {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        val alias = if (ctx.symbolPrimitive() != null) ctx.symbolPrimitive().getString() else null
        return PartiqlAst.BUILDER().projectExpr(expr, asAlias = alias)
    }

    override fun visitExprTermTuple(ctx: PartiQLParser.ExprTermTupleContext): PartiqlAst.PartiqlAstNode {
        val pairs = ctx.exprPair().map { pair -> visitExprPair(pair) }
        return PartiqlAst.BUILDER().struct(pairs)
    }

    override fun visitExprPair(ctx: PartiQLParser.ExprPairContext): PartiqlAst.ExprPair {
        val lhs = visitExprQuery(ctx.lhs)
        val rhs = visitExprQuery(ctx.rhs)
        return PartiqlAst.BUILDER().exprPair(lhs, rhs)
    }

    override fun visitLimitClause(ctx: PartiQLParser.LimitClauseContext): PartiqlAst.Expr =
        visitExprQuery(ctx.exprQuery())

    override fun visitExprQuery(ctx: PartiQLParser.ExprQueryContext): PartiqlAst.Expr =
        visit(ctx.booleanExpr()) as PartiqlAst.Expr

    override fun visitOffsetByClause(ctx: PartiQLParser.OffsetByClauseContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.exprQuery())

    override fun visitWhereClause(ctx: PartiQLParser.WhereClauseContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.exprQuery())

    override fun visitHavingClause(ctx: PartiQLParser.HavingClauseContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.exprQuery())

    override fun visitLetClause(ctx: PartiQLParser.LetClauseContext): PartiqlAst.Let {
        val letBindings = ctx.letBindings().letBinding().map { binding -> visit(binding) as PartiqlAst.LetBinding }
        return PartiqlAst.BUILDER().let(letBindings)
    }

    override fun visitLetBinding(ctx: PartiQLParser.LetBindingContext): PartiqlAst.LetBinding {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        val name = ctx.symbolPrimitive().getString()
        return PartiqlAst.BUILDER().letBinding(expr, name)
    }

    override fun visitOrderBy(ctx: PartiQLParser.OrderByContext): PartiqlAst.OrderBy {
        val sortSpecs = ctx.orderSortSpec().map { spec -> visit(spec) as PartiqlAst.SortSpec }
        return PartiqlAst.BUILDER().orderBy(sortSpecs)
    }

    override fun visitOrderBySortSpec(ctx: PartiQLParser.OrderBySortSpecContext): PartiqlAst.SortSpec {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        val order =
            if (ctx.bySpec() != null) visit(ctx.bySpec()) as PartiqlAst.OrderingSpec else PartiqlAst.BUILDER().asc()
        val nullSpec = when {
            ctx.byNullSpec() != null -> visit(ctx.byNullSpec()) as PartiqlAst.NullsSpec
            order == PartiqlAst.BUILDER().desc() -> PartiqlAst.BUILDER().nullsFirst()
            else -> PartiqlAst.BUILDER().nullsLast()
        }
        return PartiqlAst.BUILDER().sortSpec(expr, orderingSpec = order, nullsSpec = nullSpec)
    }

    override fun visitNullSpecFirst(ctx: PartiQLParser.NullSpecFirstContext): PartiqlAst.NullsSpec.NullsFirst =
        PartiqlAst.BUILDER().nullsFirst()

    override fun visitNullSpecLast(ctx: PartiQLParser.NullSpecLastContext): PartiqlAst.NullsSpec.NullsLast =
        PartiqlAst.BUILDER().nullsLast()

    override fun visitOrderByAsc(ctx: PartiQLParser.OrderByAscContext): PartiqlAst.OrderingSpec.Asc =
        PartiqlAst.BUILDER().asc()

    override fun visitOrderByDesc(ctx: PartiQLParser.OrderByDescContext): PartiqlAst.OrderingSpec.Desc =
        PartiqlAst.BUILDER().desc()

    override fun visitGroupClause(ctx: PartiQLParser.GroupClauseContext): PartiqlAst.GroupBy {
        val strategy =
            if (ctx.PARTIAL() != null) PartiqlAst.BUILDER().groupPartial() else PartiqlAst.BUILDER().groupFull()
        val keys = ctx.groupKey().map { key -> visit(key) as PartiqlAst.GroupKey }
        val keyList = PartiqlAst.BUILDER().groupKeyList(keys)
        val alias = if (ctx.groupAlias() != null) ctx.groupAlias().symbolPrimitive().getString() else null
        return PartiqlAst.BUILDER().groupBy(strategy, keyList = keyList, groupAsAlias = alias)
    }

    override fun visitGroupKeyAliasNone(ctx: PartiQLParser.GroupKeyAliasNoneContext): PartiqlAst.GroupKey {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().groupKey(expr)
    }

    override fun visitGroupKeyAlias(ctx: PartiQLParser.GroupKeyAliasContext): PartiqlAst.GroupKey {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        val alias = if (ctx.symbolPrimitive() != null) ctx.symbolPrimitive().getString() else null
        return PartiqlAst.BUILDER().groupKey(expr, asAlias = alias)
    }

    override fun visitFromClause(ctx: PartiQLParser.FromClauseContext): PartiqlAst.FromSource {
        var toJoin = visit(ctx.tableReference(0)) as PartiqlAst.FromSource
        return if (ctx.tableReference().size > 1) {
            for (index in 1 until ctx.tableReference().size) {
                val rhs = visit(ctx.tableReference(index)) as PartiqlAst.FromSource
                toJoin = PartiqlAst.BUILDER().join(PartiqlAst.JoinType.Inner(), toJoin, rhs)
            }
            toJoin
        } else toJoin
    }

    override fun visitExprTermWrappedQuery(ctx: PartiQLParser.ExprTermWrappedQueryContext): PartiqlAst.Expr {
        return visit(ctx.query()) as PartiqlAst.Expr
    }

    override fun visitTopQuery(ctx: PartiQLParser.TopQueryContext): PartiqlAst.Statement.Query {
        val queryExpr = visitQuery(ctx.query())
        return PartiqlAst.BUILDER().query(queryExpr)
    }

    override fun visitQuery(ctx: PartiQLParser.QueryContext): PartiqlAst.Expr {
        return visit(ctx.querySet()) as PartiqlAst.Expr
    }

    override fun visitQuerySetSingleQuery(ctx: PartiQLParser.QuerySetSingleQueryContext): PartiqlAst.PartiqlAstNode = visit(ctx.singleQuery())

    override fun visitQuerySetIntersect(ctx: PartiQLParser.QuerySetIntersectContext): PartiqlAst.Expr.Intersect {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        val quantifier = if (ctx.ALL() != null) PartiqlAst.BUILDER().all() else PartiqlAst.BUILDER().distinct()
        return PartiqlAst.BUILDER().intersect(quantifier, listOf(lhs, rhs))
    }

    override fun visitQuerySetExcept(ctx: PartiQLParser.QuerySetExceptContext): PartiqlAst.Expr.Except {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        val quantifier = if (ctx.ALL() != null) PartiqlAst.BUILDER().all() else PartiqlAst.BUILDER().distinct()
        return PartiqlAst.BUILDER().except(quantifier, listOf(lhs, rhs))
    }

    override fun visitQuerySetUnion(ctx: PartiQLParser.QuerySetUnionContext): PartiqlAst.Expr.Union {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        val quantifier = if (ctx.ALL() != null) PartiqlAst.BUILDER().all() else PartiqlAst.BUILDER().distinct()
        return PartiqlAst.BUILDER().union(quantifier, listOf(lhs, rhs))
    }

    override fun visitQuerySfw(ctx: PartiQLParser.QuerySfwContext): PartiqlAst.Expr.Select {
        return visit(ctx.sfwQuery()) as PartiqlAst.Expr.Select
    }

    override fun visitTableBaseRefClauses(ctx: PartiQLParser.TableBaseRefClausesContext): PartiqlAst.FromSource.Scan {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        return PartiqlAst.FromSource.Scan(expr, asAlias = null, byAlias = null, atAlias = null)
    }

    override fun visitTableRefWrappedJoin(ctx: PartiQLParser.TableRefWrappedJoinContext): PartiqlAst.FromSource {
        return visit(ctx.tableJoined()) as PartiqlAst.FromSource
    }

    // TODO: Ask Josh about this
    override fun visitTableRefCrossJoin(ctx: PartiQLParser.TableRefCrossJoinContext): PartiqlAst.FromSource {
        val lhs = visit(ctx.tableReference()) as PartiqlAst.FromSource
        val joinType = if (ctx.joinType() != null) visitJoinType(ctx.joinType()) else PartiqlAst.JoinType.Inner()
        val rhs = visit(ctx.joinRhs()) as PartiqlAst.FromSource
        return when (ctx.joinRhs()) {
            is PartiQLParser.JoinRhsTableJoinedContext -> PartiqlAst.BUILDER().join(joinType, rhs, lhs)
            else -> PartiqlAst.BUILDER().join(joinType, lhs, rhs)
        }
    }

    // TODO: Ask Josh about this
    override fun visitTableCrossJoin(ctx: PartiQLParser.TableCrossJoinContext): PartiqlAst.FromSource {
        val lhs = visit(ctx.tableReference()) as PartiqlAst.FromSource
        val joinType = if (ctx.joinType() != null) visitJoinType(ctx.joinType()) else PartiqlAst.JoinType.Inner()
        val rhs = visit(ctx.joinRhs()) as PartiqlAst.FromSource
        return when (ctx.joinRhs()) {
            is PartiQLParser.JoinRhsTableJoinedContext -> PartiqlAst.BUILDER().join(joinType, rhs, lhs)
            else -> PartiqlAst.BUILDER().join(joinType, lhs, rhs)
        }
    }

    override fun visitTableBaseRefSymbol(ctx: PartiQLParser.TableBaseRefSymbolContext): PartiqlAst.FromSource {
        val expr = visitExprQuery(ctx.exprQuery())
        val name = ctx.symbolPrimitive().getString()
        return PartiqlAst.BUILDER().scan(expr, name)
    }

    override fun visitJoinRhsNonJoin(ctx: PartiQLParser.JoinRhsNonJoinContext): PartiqlAst.PartiqlAstNode {
        return visit(ctx.tableNonJoin()) as PartiqlAst.FromSource
    }

    override fun visitTableJoinedCrossJoin(ctx: PartiQLParser.TableJoinedCrossJoinContext): PartiqlAst.FromSource {
        return visit(ctx.tableCrossJoin()) as PartiqlAst.FromSource
    }

    // Note: Same as QualifiedRefJoin
    // Note: We have a weird conditional where we changes the LHS and RHS if the RHS is a nested join
    override fun visitTableRefJoin(ctx: PartiQLParser.TableRefJoinContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.tableReference()) as PartiqlAst.FromSource
        val joinType = visitJoinType(ctx.joinType())
        val rhs = visit(ctx.joinRhs()) as PartiqlAst.FromSource
        val predicate = if (ctx.joinSpec() != null) visit(ctx.joinSpec()) as PartiqlAst.Expr else null
        return when (ctx.joinRhs()) {
            is PartiQLParser.JoinRhsTableJoinedContext -> PartiqlAst.BUILDER().join(joinType, rhs, lhs, predicate)
            else -> PartiqlAst.BUILDER().join(joinType, lhs, rhs, predicate)
        }
    }

    // Note: Same as TableRefJoin
    // Note: We have a weird conditional where we changes the LHS and RHS if the RHS is a nested join
    override fun visitQualifiedRefJoin(ctx: PartiQLParser.QualifiedRefJoinContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.tableReference()) as PartiqlAst.FromSource
        val joinType = visitJoinType(ctx.joinType())
        val rhs = visit(ctx.joinRhs()) as PartiqlAst.FromSource
        val predicate = if (ctx.joinSpec() != null) visit(ctx.joinSpec()) as PartiqlAst.Expr else null
        return when (ctx.joinRhs()) {
            is PartiQLParser.JoinRhsTableJoinedContext -> PartiqlAst.BUILDER().join(joinType, rhs, lhs, predicate)
            else -> PartiqlAst.BUILDER().join(joinType, lhs, rhs, predicate)
        }
    }

    override fun visitJoinSpecOn(ctx: PartiQLParser.JoinSpecOnContext): PartiqlAst.Expr =
        visit(ctx.exprQuery()) as PartiqlAst.Expr

    override fun visitJoinType(ctx: PartiQLParser.JoinTypeContext): PartiqlAst.JoinType {
        return when {
            ctx.LEFT() != null -> PartiqlAst.JoinType.Left()
            ctx.RIGHT() != null -> PartiqlAst.JoinType.Right()
            ctx.INNER() != null -> PartiqlAst.JoinType.Inner()
            ctx.FULL() != null -> PartiqlAst.JoinType.Full()
            ctx.OUTER() != null -> PartiqlAst.JoinType.Full()
            else -> PartiqlAst.JoinType.Inner()
        }
    }

    override fun visitJoinRhsTableJoined(ctx: PartiQLParser.JoinRhsTableJoinedContext): PartiqlAst.PartiqlAstNode {
        return visit(ctx.tableJoined())
    }

    override fun visitNestedTableJoined(ctx: PartiQLParser.NestedTableJoinedContext): PartiqlAst.PartiqlAstNode {
        return visit(ctx.tableJoined())
    }

    override fun visitExprTermBag(ctx: PartiQLParser.ExprTermBagContext): PartiqlAst.Expr.Bag {
        val exprList = ctx.exprQuery().map { exprQuery -> visit(exprQuery) as PartiqlAst.Expr }
        return PartiqlAst.Expr.Bag(exprList)
    }

    override fun visitVarRefExprIdentQuoted(ctx: PartiQLParser.VarRefExprIdentQuotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER()
            .id(ctx.toRawString(), PartiqlAst.CaseSensitivity.CaseSensitive(), PartiqlAst.ScopeQualifier.Unqualified())

    override fun visitVarRefExprIdentAtQuoted(ctx: PartiQLParser.VarRefExprIdentAtQuotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER()
            .id(ctx.toRawString(), PartiqlAst.CaseSensitivity.CaseSensitive(), PartiqlAst.ScopeQualifier.LocalsFirst())

    override fun visitVarRefExprIdentAtUnquoted(ctx: PartiQLParser.VarRefExprIdentAtUnquotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(
            ctx.toRawString(),
            PartiqlAst.CaseSensitivity.CaseInsensitive(),
            PartiqlAst.ScopeQualifier.LocalsFirst()
        )

    override fun visitVarRefExprIdentUnquoted(ctx: PartiQLParser.VarRefExprIdentUnquotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(
            ctx.toRawString(),
            PartiqlAst.CaseSensitivity.CaseInsensitive(),
            PartiqlAst.ScopeQualifier.Unqualified()
        )

    override fun visitExprTermVarRefExpr(ctx: PartiQLParser.ExprTermVarRefExprContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.varRefExpr())

    /**
     * EXPRESSIONS
     */

    override fun visitExprQueryOr(ctx: PartiQLParser.ExprQueryOrContext) = getBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitExprQueryAnd(ctx: PartiQLParser.ExprQueryAndContext) = getBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitMathOp00(ctx: PartiQLParser.MathOp00Context): PartiqlAst.PartiqlAstNode = getBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitMathOp01(ctx: PartiQLParser.MathOp01Context): PartiqlAst.PartiqlAstNode = getBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitMathOp02(ctx: PartiQLParser.MathOp02Context): PartiqlAst.PartiqlAstNode = getBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)

    private fun getBinaryOperation(lhs: ParserRuleContext?, rhs: ParserRuleContext?, op: Token?, parent: ParserRuleContext? = null): PartiqlAst.PartiqlAstNode {
        if (parent != null) return visit(parent) as PartiqlAst.Expr
        val lhs = visit(lhs) as PartiqlAst.Expr
        val rhs = visit(rhs) as PartiqlAst.Expr
        return when (op!!.type) {
            PartiQLParser.AND -> PartiqlAst.BUILDER().and(listOf(lhs, rhs))
            PartiQLParser.OR -> PartiqlAst.BUILDER().or(listOf(lhs, rhs))
            PartiQLParser.ASTERISK -> PartiqlAst.BUILDER().times(listOf(lhs, rhs))
            PartiQLParser.SLASH_FORWARD -> PartiqlAst.BUILDER().divide(listOf(lhs, rhs))
            PartiQLParser.PLUS -> PartiqlAst.BUILDER().plus(listOf(lhs, rhs))
            PartiQLParser.MINUS -> PartiqlAst.BUILDER().minus(listOf(lhs, rhs))
            PartiQLParser.PERCENT -> PartiqlAst.BUILDER().modulo(listOf(lhs, rhs))
            PartiQLParser.CONCAT -> PartiqlAst.BUILDER().concat(listOf(lhs, rhs))
            else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unknown operation")
        }
    }

    override fun visitValueExpr(ctx: PartiQLParser.ValueExprContext): PartiqlAst.PartiqlAstNode {
        if (ctx.parent != null) return visit(ctx.parent) as PartiqlAst.Expr
        return when (ctx.sign.type) {
            PartiQLParser.PLUS -> PartiqlAst.BUILDER().pos(visit(ctx.rhs) as PartiqlAst.Expr)
            PartiQLParser.MINUS -> PartiqlAst.BUILDER().neg(visit(ctx.rhs) as PartiqlAst.Expr)
            else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unsupported sign.")
        }
    }

    override fun visitPredicateComparison(ctx: PartiQLParser.PredicateComparisonContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return when {
            ctx.ANGLE_LEFT() != null -> PartiqlAst.BUILDER().lt(listOf(lhs, rhs))
            ctx.LT_EQ() != null -> PartiqlAst.BUILDER().lte(listOf(lhs, rhs))
            ctx.ANGLE_RIGHT() != null -> PartiqlAst.BUILDER().gt(listOf(lhs, rhs))
            ctx.GT_EQ() != null -> PartiqlAst.BUILDER().gte(listOf(lhs, rhs))
            ctx.NEQ() != null -> PartiqlAst.BUILDER().ne(listOf(lhs, rhs))
            ctx.EQ() != null -> PartiqlAst.BUILDER().eq(listOf(lhs, rhs))
            else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unknown operator.")
        }
    }

    // TODO: Implement
    override fun visitExprQueryNot(ctx: PartiQLParser.ExprQueryNotContext): PartiqlAst.PartiqlAstNode {
        if (ctx.parent != null) return visit(ctx.parent) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().not(rhs)
    }

    override fun visitPredicateIn(ctx: PartiQLParser.PredicateInContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        val isType = PartiqlAst.BUILDER().inCollection(listOf(lhs, rhs))
        return if (ctx.NOT() == null) isType
        else PartiqlAst.BUILDER().not(isType)
    }

    override fun visitPredicateIs(ctx: PartiQLParser.PredicateIsContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.type()) as PartiqlAst.Type
        val isType = PartiqlAst.BUILDER().isType(lhs, rhs)
        return if (ctx.NOT() == null) isType
        else PartiqlAst.BUILDER().not(isType)
    }

    // TODO: Helper method
    override fun visitPredicateBetween(ctx: PartiQLParser.PredicateBetweenContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val lower = visit(ctx.lower) as PartiqlAst.Expr
        val upper = visit(ctx.upper) as PartiqlAst.Expr
        val between = PartiqlAst.BUILDER().between(lhs, lower, upper)
        return if (ctx.NOT() == null) between
        else PartiqlAst.BUILDER().not(between)
    }

    // TODO: Helper method
    override fun visitPredicateLike(ctx: PartiQLParser.PredicateLikeContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        val escape = if (ctx.escape == null) null else visit(ctx.escape) as PartiqlAst.Expr
        var like: PartiqlAst.Expr = PartiqlAst.BUILDER().like(lhs, rhs, escape)
        if (ctx.NOT() != null) like = PartiqlAst.BUILDER().not(like)
        return like
    }

    override fun visitLiteralNull(ctx: PartiQLParser.LiteralNullContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newNull().toIonElement())

    override fun visitLiteralMissing(ctx: PartiQLParser.LiteralMissingContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().missing()

    override fun visitLiteralTrue(ctx: PartiQLParser.LiteralTrueContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newBool(true).toIonElement())

    override fun visitLiteralFalse(ctx: PartiQLParser.LiteralFalseContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newBool(false).toIonElement())

    override fun visitLiteralIon(ctx: PartiQLParser.LiteralIonContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.singleValue(ctx.ION_CLOSURE().text.toIonString()).toIonElement())

    override fun visitLiteralString(ctx: PartiQLParser.LiteralStringContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newString(ctx.LITERAL_STRING().text.toPartiQLString()).toIonElement())

    override fun visitLiteralInteger(ctx: PartiQLParser.LiteralIntegerContext): PartiqlAst.Expr.Lit =
        PartiqlAst.Expr.Lit(ion.newInt(BigInteger(ctx.LITERAL_INTEGER().text, 10)).toIonElement())

    override fun visitLiteralDate(ctx: PartiQLParser.LiteralDateContext): PartiqlAst.PartiqlAstNode {
        val dateString = ctx.LITERAL_STRING().text.toPartiQLString()
        val (year, month, day) = dateString.split("-")
        return PartiqlAst.BUILDER().date(year.toLong(), month.toLong(), day.toLong())
    }

    override fun visitLiteralTime(ctx: PartiQLParser.LiteralTimeContext): PartiqlAst.PartiqlAstNode {
        val timeString = ctx.LITERAL_STRING().text.toPartiQLString()
        val precision = when (ctx.LITERAL_INTEGER()) {
            null -> try {
                getPrecisionFromTimeString(timeString).toLong()
            } catch (e: EvaluationException) {
                throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException(
                    "Unable to parse precision.",
                    e
                )
            }
            else -> ctx.LITERAL_INTEGER().text.toInteger().toLong()
        }
        if (precision < 0 || precision > MAX_PRECISION_FOR_TIME) {
            throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Precision out of bounds")
        }
        var time: LocalTime
        try {
            time = LocalTime.parse(timeString, DateTimeFormatter.ISO_TIME)
        } catch (e: DateTimeParseException) {
            throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unable to parse time", e)
        }
        return PartiqlAst.BUILDER().litTime(
            PartiqlAst.BUILDER().timeValue(
                time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                precision, false, null
            )
        )
    }

    override fun visitLiteralTimeZone(ctx: PartiQLParser.LiteralTimeZoneContext): PartiqlAst.PartiqlAstNode {
        val timeString = ctx.LITERAL_STRING().text.toPartiQLString()
        val precision = when (ctx.LITERAL_INTEGER()) {
            null -> try {
                getPrecisionFromTimeString(timeString).toLong()
            } catch (e: EvaluationException) {
                throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException(
                    "Unable to parse precision.",
                    e
                )
            }
            else -> ctx.LITERAL_INTEGER().text.toInteger().toLong()
        }
        if (precision < 0 || precision > MAX_PRECISION_FOR_TIME) {
            throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Precision out of bounds")
        }
        try {
            var time: OffsetTime
            time = OffsetTime.parse(timeString)
            return PartiqlAst.BUILDER().litTime(
                PartiqlAst.BUILDER().timeValue(
                    time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                    precision, true, (time.offset.totalSeconds / 60).toLong()
                )
            )
        } catch (e: DateTimeParseException) {
            var time: LocalTime
            try {
                time = LocalTime.parse(timeString)
            } catch (e: DateTimeParseException) {
                throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unable to parse time", e)
            }
            return PartiqlAst.BUILDER().litTime(
                PartiqlAst.BUILDER().timeValue(
                    time.hour.toLong(), time.minute.toLong(), time.second.toLong(),
                    time.nano.toLong(), precision, true, null
                )
            )
        }
    }

    override fun visitCast(ctx: PartiQLParser.CastContext): PartiqlAst.Expr.Cast {
        val expr = visitExprQuery(ctx.exprQuery())
        val type = visit(ctx.type()) as PartiqlAst.Type
        return PartiqlAst.BUILDER().cast(expr, type)
    }

    override fun visitCanCast(ctx: PartiQLParser.CanCastContext): PartiqlAst.Expr.CanCast {
        val expr = visitExprQuery(ctx.exprQuery())
        val type = visit(ctx.type()) as PartiqlAst.Type
        return PartiqlAst.BUILDER().canCast(expr, type)
    }

    override fun visitCanLosslessCast(ctx: PartiQLParser.CanLosslessCastContext): PartiqlAst.Expr.CanLosslessCast {
        val expr = visitExprQuery(ctx.exprQuery())
        val type = visit(ctx.type()) as PartiqlAst.Type
        return PartiqlAst.BUILDER().canLosslessCast(expr, type)
    }

    override fun visitTypeAtomic(ctx: PartiQLParser.TypeAtomicContext): PartiqlAst.Type {
        return when {
            ctx.NULL() != null -> PartiqlAst.Type.NullType()
            ctx.BOOL() != null || ctx.BOOLEAN() != null -> PartiqlAst.Type.BooleanType()
            ctx.SMALLINT() != null -> PartiqlAst.Type.SmallintType()
            ctx.INT2() != null || ctx.INTEGER2() != null -> PartiqlAst.Type.SmallintType()
            ctx.INT() != null || ctx.INTEGER() != null -> PartiqlAst.Type.IntegerType()
            ctx.INT4() != null || ctx.INTEGER4() != null -> PartiqlAst.Type.Integer4Type()
            ctx.INT8() != null || ctx.INTEGER8() != null -> PartiqlAst.Type.Integer8Type()
            ctx.BIGINT() != null -> PartiqlAst.Type.Integer8Type()
            ctx.REAL() != null -> PartiqlAst.Type.RealType()
            ctx.DOUBLE() != null -> PartiqlAst.Type.DoublePrecisionType()
            ctx.TIMESTAMP() != null -> PartiqlAst.Type.TimestampType()
            ctx.MISSING() != null -> PartiqlAst.Type.MissingType()
            ctx.STRING() != null -> PartiqlAst.Type.StringType()
            ctx.SYMBOL() != null -> PartiqlAst.Type.SymbolType()
            ctx.BLOB() != null -> PartiqlAst.Type.BlobType()
            ctx.CLOB() != null -> PartiqlAst.Type.ClobType()
            ctx.DATE() != null -> PartiqlAst.Type.DateType()
            ctx.STRUCT() != null -> PartiqlAst.Type.StructType()
            ctx.TUPLE() != null -> PartiqlAst.Type.TupleType()
            ctx.LIST() != null -> PartiqlAst.Type.SexpType()
            ctx.BAG() != null -> PartiqlAst.Type.BagType()
            ctx.ANY() != null -> PartiqlAst.Type.AnyType()
            else -> PartiqlAst.Type.AnyType()
        }
    }

    override fun visitTypeVarChar(ctx: PartiQLParser.TypeVarCharContext): PartiqlAst.Type.CharacterVaryingType {
        val length = if (ctx.length != null) ctx.length.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.CharacterVaryingType(length)
    }

    override fun visitTypeChar(ctx: PartiQLParser.TypeCharContext): PartiqlAst.Type.CharacterType {
        val length = if (ctx.length != null) ctx.length.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.CharacterType(length)
    }

    override fun visitTypeFloat(ctx: PartiQLParser.TypeFloatContext): PartiqlAst.Type.FloatType {
        val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.FloatType(precision)
    }

    override fun visitTypeDecimal(ctx: PartiQLParser.TypeDecimalContext): PartiqlAst.Type {
        val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong().asPrimitive() else null
        val scale = if (ctx.scale != null) ctx.scale.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.DecimalType(precision, scale)
    }

    override fun visitTypeNumeric(ctx: PartiQLParser.TypeNumericContext): PartiqlAst.Type.NumericType {
        val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong().asPrimitive() else null
        val scale = if (ctx.scale != null) ctx.scale.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.NumericType(precision, scale)
    }

    override fun visitTypeTime(ctx: PartiQLParser.TypeTimeContext): PartiqlAst.Type.TimeType {
        val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.TimeType(precision)
    }

    override fun visitTypeTimeZone(ctx: PartiQLParser.TypeTimeZoneContext): PartiqlAst.Type.TimeWithTimeZoneType {
        val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong().asPrimitive() else null
        return PartiqlAst.Type.TimeWithTimeZoneType(precision)
    }

    // TODO: Determine if should throw error on else
    override fun visitTypeCustom(ctx: PartiQLParser.TypeCustomContext): PartiqlAst.Type {
        val customName: String = when (val name = ctx.symbolPrimitive().getString().toLowerCase()) {
            in CUSTOM_KEYWORDS -> name
            in CUSTOM_TYPE_ALIASES.keys -> CUSTOM_TYPE_ALIASES.getOrDefault(name, name)
            else -> name
        }
        return PartiqlAst.Type.CustomType(SymbolPrimitive(customName, mapOf()))
    }

    // TODO: Catch exception for exponent too large
    override fun visitLiteralDecimal(ctx: PartiQLParser.LiteralDecimalContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newDecimal(bigDecimalOf(ctx.LITERAL_DECIMAL().text)).toIonElement())

    // TODO: should the function base allow f(), "f"(), @"f"(), and @f()?
    override fun visitFunctionCall(ctx: PartiQLParser.FunctionCallContext): PartiqlAst.PartiqlAstNode {
        val name = ctx.symbolPrimitive().getString()
        val args = ctx.functionCallArg().map { arg -> visit(arg) as PartiqlAst.Expr }
        return PartiqlAst.BUILDER().call(name, args)
    }

    override fun visitFunctionCallArg(ctx: PartiQLParser.FunctionCallArgContext): PartiqlAst.Expr = visitExprQuery(ctx.exprQuery())

    /**
     *
     * HELPER METHODS
     *
     */

    private fun PartiQLParser.VarRefExprIdentAtUnquotedContext.toRawString() =
        this.IDENTIFIER_AT_UNQUOTED().text.removePrefix("@")

    private fun PartiQLParser.VarRefExprIdentAtQuotedContext.toRawString() =
        this.IDENTIFIER_AT_QUOTED().text.removePrefix("@").toPartiQLIdentifier()

    private fun PartiQLParser.VarRefExprIdentQuotedContext.toRawString() =
        this.IDENTIFIER_QUOTED().text.toPartiQLIdentifier()

    private fun PartiQLParser.VarRefExprIdentUnquotedContext.toRawString() = this.IDENTIFIER().text

    private fun String.toPartiQLString(): String = this.trim('\'').replace("''", "'")
    private fun String.toPartiQLIdentifier(): String = this.trim('"').replace("\"\"", "\"")
    private fun String.toIonString(): String = this.trim('`')

    private fun getStrategy(strategy: PartiQLParser.SetQuantifierStrategyContext?): PartiqlAst.SetQuantifier? {
        return when {
            strategy == null -> null
            strategy.text.toUpperCase() == "DISTINCT" -> PartiqlAst.SetQuantifier.Distinct()
            else -> PartiqlAst.SetQuantifier.All()
        }
    }

    private fun getSetQuantifierStrategy(ctx: PartiQLParser.SelectClauseContext): PartiqlAst.SetQuantifier? {
        return when (ctx) {
            is PartiQLParser.SelectAllContext -> getStrategy(ctx.setQuantifierStrategy())
            is PartiQLParser.SelectItemsContext -> getStrategy(ctx.setQuantifierStrategy())
            is PartiQLParser.SelectValueContext -> getStrategy(ctx.setQuantifierStrategy())
            is PartiQLParser.SelectPivotContext -> null
            else -> null
        }
    }

    private fun PartiQLParser.SymbolPrimitiveContext.getString(): String {
        return when (this) {
            is PartiQLParser.SymbolIdentifierQuotedContext -> this.IDENTIFIER_QUOTED().text.toPartiQLIdentifier()
            is PartiQLParser.SymbolIdentifierUnquotedContext -> this.IDENTIFIER().text
            is PartiQLParser.SymbolIdentifierAtQuotedContext -> this.IDENTIFIER_AT_QUOTED().text.removePrefix("@").toPartiQLIdentifier()
            is PartiQLParser.SymbolIdentifierAtUnquotedContext -> this.IDENTIFIER_AT_UNQUOTED().text.removePrefix("@")
            else -> "UNKNOWN"
        }
    }

    private fun String.toInteger() = BigInteger(this, 10)
}

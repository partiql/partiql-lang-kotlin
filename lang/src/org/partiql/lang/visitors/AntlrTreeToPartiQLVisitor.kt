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
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.generated.PartiQLBaseVisitor
import org.partiql.lang.generated.PartiQLParser
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.getPrecisionFromTimeString
import java.math.BigInteger
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AntlrTreeToPartiQLVisitor(val ion: IonSystem) : PartiQLBaseVisitor<PartiqlAst.PartiqlAstNode>() {

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

    /**
     *
     * SIMPLE CLAUSES
     *
     */

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
        return visitQuerySet(ctx.querySet())
    }

    // TODO: Do other scenarios
    override fun visitQuerySet(ctx: PartiQLParser.QuerySetContext): PartiqlAst.Expr {
        return visit(ctx.singleQuery()) as PartiqlAst.Expr
    }

    override fun visitQuerySfw(ctx: PartiQLParser.QuerySfwContext): PartiqlAst.Expr.Select {
        return visit(ctx.sfwQuery()) as PartiqlAst.Expr.Select
    }

    override fun visitTableBaseRefClauses(ctx: PartiQLParser.TableBaseRefClausesContext): PartiqlAst.FromSource.Scan {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        return PartiqlAst.FromSource.Scan(expr, asAlias = null, byAlias = null, atAlias = null)
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

    override fun visitExprQueryAnd(ctx: PartiQLParser.ExprQueryAndContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().and(listOf(lhs, rhs))
    }

    override fun visitExprQueryDivide(ctx: PartiQLParser.ExprQueryDivideContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().divide(listOf(lhs, rhs))
    }

    override fun visitExprQueryMultiply(ctx: PartiQLParser.ExprQueryMultiplyContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().times(listOf(lhs, rhs))
    }

    override fun visitExprQueryPlus(ctx: PartiQLParser.ExprQueryPlusContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().plus(listOf(lhs, rhs))
    }

    override fun visitExprQueryModulo(ctx: PartiQLParser.ExprQueryModuloContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().modulo(listOf(lhs, rhs))
    }

    override fun visitExprQueryOr(ctx: PartiQLParser.ExprQueryOrContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().or(listOf(lhs, rhs))
    }

    override fun visitExprQueryMinus(ctx: PartiQLParser.ExprQueryMinusContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().minus(listOf(lhs, rhs))
    }

    override fun visitExprQueryPositive(ctx: PartiQLParser.ExprQueryPositiveContext): PartiqlAst.PartiqlAstNode {
        return PartiqlAst.BUILDER().pos(visit(ctx.rhs) as PartiqlAst.Expr)
    }

    override fun visitExprQueryNegative(ctx: PartiQLParser.ExprQueryNegativeContext): PartiqlAst.PartiqlAstNode {
        return PartiqlAst.BUILDER().neg(visit(ctx.rhs) as PartiqlAst.Expr)
    }

    override fun visitExprQueryEq(ctx: PartiQLParser.ExprQueryEqContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().eq(listOf(lhs, rhs))
    }

    override fun visitExprQueryNeq(ctx: PartiQLParser.ExprQueryNeqContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().ne(listOf(lhs, rhs))
    }

    override fun visitExprQueryLt(ctx: PartiQLParser.ExprQueryLtContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().lt(listOf(lhs, rhs))
    }

    override fun visitExprQueryGt(ctx: PartiQLParser.ExprQueryGtContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().gt(listOf(lhs, rhs))
    }

    override fun visitExprQueryGtEq(ctx: PartiQLParser.ExprQueryGtEqContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().gte(listOf(lhs, rhs))
    }

    override fun visitExprQueryLtEq(ctx: PartiQLParser.ExprQueryLtEqContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().lte(listOf(lhs, rhs))
    }

    override fun visitExprQueryPrimary(ctx: PartiQLParser.ExprQueryPrimaryContext): PartiqlAst.PartiqlAstNode {
        return visit(ctx.exprPrimary())
    }

    override fun visitExprQueryIs(ctx: PartiQLParser.ExprQueryIsContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Type
        val isType = PartiqlAst.BUILDER().isType(lhs, rhs)
        return if (ctx.NOT() == null) isType
        else PartiqlAst.BUILDER().not(isType)
    }

    override fun visitExprQueryBetween(ctx: PartiQLParser.ExprQueryBetweenContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val lower = visit(ctx.lower) as PartiqlAst.Expr
        val upper = visit(ctx.upper) as PartiqlAst.Expr
        val between = PartiqlAst.BUILDER().between(lhs, lower, upper)
        return if (ctx.NOT() == null) between
        else PartiqlAst.BUILDER().not(between)
    }

    override fun visitExprQueryLike(ctx: PartiQLParser.ExprQueryLikeContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        val escape = if (ctx.escape == null) null else visit(ctx.escape) as PartiqlAst.Expr
        val like = PartiqlAst.BUILDER().like(lhs, rhs, escape)
        return if (ctx.NOT() == null) like
        else PartiqlAst.BUILDER().not(like)
    }

    override fun visitExprQueryConcat(ctx: PartiQLParser.ExprQueryConcatContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.rhs) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().concat(listOf(lhs, rhs))
    }

    /**
     *
     * LITERALS
     *
     */

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

    override fun visitLiteralInteger(ctx: PartiQLParser.LiteralIntegerContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newInt(BigInteger(ctx.LITERAL_INTEGER().text, 10)).toIonElement())

    override fun visitLiteralDate(ctx: PartiQLParser.LiteralDateContext): PartiqlAst.PartiqlAstNode {
        val dateString = ctx.LITERAL_STRING().text.toPartiQLString()
        val (year, month, day) = dateString.split("-")
        return PartiqlAst.BUILDER().date(year.toLong(), month.toLong(), day.toLong())
    }

    override fun visitLiteralTime(ctx: PartiQLParser.LiteralTimeContext): PartiqlAst.PartiqlAstNode {
        val timeString = ctx.LITERAL_STRING().text.toPartiQLString()
        // TODO: Get precision if specified
        val precision = getPrecisionFromTimeString(timeString).toLong()
        val time = LocalTime.parse(timeString, DateTimeFormatter.ISO_TIME)
        return PartiqlAst.BUILDER().litTime(
            PartiqlAst.BUILDER().timeValue(
                time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                precision, false, null
            )
        )
    }

    // TODO: Catch exception for exponent too large
    override fun visitLiteralDecimal(ctx: PartiQLParser.LiteralDecimalContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.Expr.Lit(ion.newDecimal(bigDecimalOf(ctx.LITERAL_DECIMAL().text)).toIonElement())

    /**
     *
     * HELPER METHODS
     *
     */

    private fun PartiQLParser.VarRefExprIdentAtUnquotedContext.toRawString() =
        this.IDENTIFIER_AT_UNQUOTED().text.removePrefix("@")

    private fun PartiQLParser.VarRefExprIdentAtQuotedContext.toRawString() =
        this.IDENTIFIER_AT_QUOTED().text.removePrefix("@").trim('"')

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
            else -> "UNKNOWN"
        }
    }
}

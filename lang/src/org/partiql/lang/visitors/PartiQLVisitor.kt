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
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.toIonElement
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.IsImplictJoinMeta
import org.partiql.lang.ast.IsPathIndexMeta
import org.partiql.lang.ast.LegacyLogicalNotMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.time.MAX_PRECISION_FOR_TIME
import org.partiql.lang.generated.PartiQLBaseVisitor
import org.partiql.lang.generated.PartiQLParser
import org.partiql.lang.syntax.DATE_TIME_PART_KEYWORDS
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
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Extends ANTLR's generated [PartiQLBaseVisitor] to visit an ANTLR ParseTree and convert it into a PartiQL AST. This
 * class uses the [PartiqlAst.PartiqlAstNode] to represent all nodes within the new AST.
 */
class PartiQLVisitor(val ion: IonSystem, val customTypes: List<CustomType> = listOf(), val parameterIndexes: Map<Int, Int> = mapOf()) :
    PartiQLBaseVisitor<PartiqlAst.PartiqlAstNode>() {

    private val CUSTOM_KEYWORDS = customTypes.map { it.name.toLowerCase() }

    private val CUSTOM_TYPE_ALIASES =
        customTypes.map { customType ->
            customType.aliases.map { alias ->
                Pair(alias.toLowerCase(), customType.name.toLowerCase())
            }
        }.flatten().toMap()

    override fun visitSfwQuery(ctx: PartiQLParser.SfwQueryContext): PartiqlAst.Expr.Select {
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

    override fun visitSelectAll(ctx: PartiQLParser.SelectAllContext) = PartiqlAst.build { projectStar() }
    override fun visitSelectItems(ctx: PartiQLParser.SelectItemsContext) = visitProjectionItems(ctx.projectionItems())

    override fun visitSelectPivot(ctx: PartiQLParser.SelectPivotContext) = PartiqlAst.build {
        projectPivot(visitExpr(ctx.at), visitExpr(ctx.pivot))
    }

    override fun visitSelectValue(ctx: PartiQLParser.SelectValueContext) = PartiqlAst.build {
        projectValue(visitExpr(ctx.expr()))
    }

    override fun visitProjectionItems(ctx: PartiQLParser.ProjectionItemsContext): PartiqlAst.Projection.ProjectList {
        val projections = ctx.projectionItem().map { projection -> visit(projection) as PartiqlAst.ProjectItem }
        return PartiqlAst.BUILDER().projectList(projections)
    }

    override fun visitProjectionItem(ctx: PartiQLParser.ProjectionItemContext): PartiqlAst.ProjectItem {
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
        val alias = if (ctx.symbolPrimitive() != null) ctx.symbolPrimitive().getString() else null
        return if (expr is PartiqlAst.Expr.Path) convertPathToProjectionItem(expr, alias)
        else PartiqlAst.build { projectExpr(expr, asAlias = alias) }
    }

    override fun visitExprTermTuple(ctx: PartiQLParser.ExprTermTupleContext): PartiqlAst.PartiqlAstNode {
        val pairs = ctx.exprPair().map { pair -> visitExprPair(pair) }
        return PartiqlAst.BUILDER().struct(pairs)
    }

    override fun visitExprPair(ctx: PartiQLParser.ExprPairContext): PartiqlAst.ExprPair {
        val lhs = visitExpr(ctx.lhs)
        val rhs = visitExpr(ctx.rhs)
        return PartiqlAst.BUILDER().exprPair(lhs, rhs)
    }

    override fun visitLimitClause(ctx: PartiQLParser.LimitClauseContext): PartiqlAst.Expr =
        visitExpr(ctx.expr())

    override fun visitExpr(ctx: PartiQLParser.ExprContext) = visitExprOr(ctx.exprOr())

    override fun visitOffsetByClause(ctx: PartiQLParser.OffsetByClauseContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.expr())

    override fun visitWhereClause(ctx: PartiQLParser.WhereClauseContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.expr())

    override fun visitHavingClause(ctx: PartiQLParser.HavingClauseContext): PartiqlAst.PartiqlAstNode =
        visit(ctx.expr())

    override fun visitLetClause(ctx: PartiQLParser.LetClauseContext): PartiqlAst.Let {
        val letBindings = ctx.letBinding().map { binding -> visit(binding) as PartiqlAst.LetBinding }
        return PartiqlAst.BUILDER().let(letBindings)
    }

    override fun visitLetBinding(ctx: PartiQLParser.LetBindingContext): PartiqlAst.LetBinding {
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
        val name = ctx.symbolPrimitive().getString()
        return PartiqlAst.BUILDER().letBinding(expr, name)
    }

    override fun visitOrderBy(ctx: PartiQLParser.OrderByContext): PartiqlAst.OrderBy {
        val sortSpecs = ctx.orderSortSpec().map { spec -> visit(spec) as PartiqlAst.SortSpec }
        return PartiqlAst.BUILDER().orderBy(sortSpecs)
    }

    override fun visitOrderBySortSpec(ctx: PartiQLParser.OrderBySortSpecContext): PartiqlAst.SortSpec {
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
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
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().groupKey(expr)
    }

    override fun visitGroupKeyAlias(ctx: PartiQLParser.GroupKeyAliasContext): PartiqlAst.GroupKey {
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
        val alias = if (ctx.symbolPrimitive() != null) ctx.symbolPrimitive().getString() else null
        return PartiqlAst.BUILDER().groupKey(expr, asAlias = alias)
    }

    override fun visitFromClause(ctx: PartiQLParser.FromClauseContext) = visit(ctx.tableReference()) as PartiqlAst.FromSource
    override fun visitExprTermWrappedQuery(ctx: PartiQLParser.ExprTermWrappedQueryContext) = visit(ctx.query()) as PartiqlAst.Expr

    override fun visitTopQuery(ctx: PartiQLParser.TopQueryContext): PartiqlAst.Statement.Query {
        val queryExpr = visitQuery(ctx.query())
        return PartiqlAst.BUILDER().query(queryExpr)
    }

    override fun visitQuery(ctx: PartiQLParser.QueryContext) = visit(ctx.querySet()) as PartiqlAst.Expr
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

    // TODO: Add metas
    private fun convertSymbolPrimitive(sym: PartiQLParser.SymbolPrimitiveContext?): SymbolPrimitive? = when (sym) {
        null -> null
        else -> SymbolPrimitive(sym.getString(), mapOf())
    }

    /**
     *
     * TABLE REFERENCES & JOINS
     *
     */

    override fun visitTableBaseRefClauses(ctx: PartiQLParser.TableBaseRefClausesContext): PartiqlAst.FromSource.Scan {
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
        val asAlias = if (ctx.asIdent() != null) convertSymbolPrimitive(ctx.asIdent().symbolPrimitive()) else null
        val atAlias = if (ctx.atIdent() != null) convertSymbolPrimitive(ctx.atIdent().symbolPrimitive()) else null
        val byAlias = if (ctx.byIdent() != null) convertSymbolPrimitive(ctx.byIdent().symbolPrimitive()) else null
        return PartiqlAst.FromSource.Scan(expr, asAlias = asAlias, byAlias = byAlias, atAlias = atAlias)
    }

    override fun visitTableUnpivot(ctx: PartiQLParser.TableUnpivotContext): PartiqlAst.PartiqlAstNode {
        val expr = visit(ctx.expr()) as PartiqlAst.Expr
        val asAlias = if (ctx.asIdent() != null) ctx.asIdent().symbolPrimitive().getString() else null
        val atAlias = if (ctx.atIdent() != null) ctx.atIdent().symbolPrimitive().getString() else null
        val byAlias = if (ctx.byIdent() != null) ctx.byIdent().symbolPrimitive().getString() else null
        return PartiqlAst.build {
            unpivot(expr, asAlias = asAlias, atAlias = atAlias, byAlias = byAlias)
        }
    }

    /**
     * Note: Similar to the old SqlParser, we have an odd condition (if the RHS is a nested join), where we flip
     * the LHS and RHS operands.
     */
    override fun visitTableCrossJoin(ctx: PartiQLParser.TableCrossJoinContext): PartiqlAst.FromSource {
        val lhs = visit(ctx.lhs) as PartiqlAst.FromSource
        val joinType = visitJoinType(ctx.joinType())
        val rhs = visit(ctx.rhs) as PartiqlAst.FromSource
        val metas = metaContainerOf(IsImplictJoinMeta.instance)
        return when (ctx.rhs) {
            is PartiQLParser.JoinRhsTableJoinedContext -> PartiqlAst.BUILDER().join(joinType, rhs, lhs, metas = metas)
            else -> PartiqlAst.BUILDER().join(joinType, lhs, rhs, metas = metas)
        }
    }

    /**
     * Note: Similar to the old SqlParser, we have an odd condition (if the RHS is a nested join), where we flip
     * the LHS and RHS operands.
     */
    override fun visitTableQualifiedJoin(ctx: PartiQLParser.TableQualifiedJoinContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.FromSource
        val joinType = visitJoinType(ctx.joinType())
        val rhs = visit(ctx.rhs) as PartiqlAst.FromSource
        val predicate = if (ctx.joinSpec() != null) visit(ctx.joinSpec()) as PartiqlAst.Expr else null
        return when (ctx.rhs) {
            is PartiQLParser.JoinRhsTableJoinedContext -> PartiqlAst.BUILDER().join(joinType, rhs, lhs, predicate)
            else -> PartiqlAst.BUILDER().join(joinType, lhs, rhs, predicate)
        }
    }

    override fun visitTableBaseRefSymbol(ctx: PartiQLParser.TableBaseRefSymbolContext): PartiqlAst.FromSource {
        val expr = visitExpr(ctx.expr())
        val name = ctx.symbolPrimitive().getString()
        return PartiqlAst.BUILDER().scan(expr, name)
    }

    override fun visitTableWrapped(ctx: PartiQLParser.TableWrappedContext): PartiqlAst.PartiqlAstNode = visit(ctx.tableReference())

    override fun visitJoinSpec(ctx: PartiQLParser.JoinSpecContext) = visitExpr(ctx.expr())

    override fun visitJoinType(ctx: PartiQLParser.JoinTypeContext?): PartiqlAst.JoinType {
        return when {
            ctx == null -> PartiqlAst.JoinType.Inner()
            ctx.LEFT() != null -> PartiqlAst.JoinType.Left()
            ctx.RIGHT() != null -> PartiqlAst.JoinType.Right()
            ctx.INNER() != null -> PartiqlAst.JoinType.Inner()
            ctx.FULL() != null -> PartiqlAst.JoinType.Full()
            ctx.OUTER() != null -> PartiqlAst.JoinType.Full()
            else -> PartiqlAst.JoinType.Inner()
        }
    }

    override fun visitJoinRhsTableJoined(ctx: PartiQLParser.JoinRhsTableJoinedContext) = visit(ctx.tableReference()) as PartiqlAst.FromSource

    /**
     *
     * EXPRESSIONS
     *
     */

    override fun visitExprTermBag(ctx: PartiQLParser.ExprTermBagContext): PartiqlAst.Expr.Bag {
        val exprList = ctx.expr().map { expr -> visit(expr) as PartiqlAst.Expr }
        return PartiqlAst.Expr.Bag(exprList)
    }

    override fun visitParameter(ctx: PartiQLParser.ParameterContext): PartiqlAst.PartiqlAstNode {
        val parameterIndex = parameterIndexes[ctx.QUESTION_MARK().symbol.tokenIndex]
            ?: throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unable to find index of parameter.")
        return PartiqlAst.build { parameter(parameterIndex.toLong()) }
    }

    override fun visitSequenceConstructor(ctx: PartiQLParser.SequenceConstructorContext): PartiqlAst.Expr {
        val expressions = visitOrEmpty(PartiqlAst.Expr::class, ctx.expr())
        return PartiqlAst.build {
            when (ctx.datatype.type) {
                PartiQLParser.LIST -> list(expressions)
                PartiQLParser.SEXP -> sexp(expressions)
                else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unknown sequence")
            }
        }
    }

    override fun visitAggregateBase(ctx: PartiQLParser.AggregateBaseContext): PartiqlAst.Expr.CallAgg {
        val strategy = getStrategy(ctx.setQuantifierStrategy(), default = PartiqlAst.SetQuantifier.All())
        val arg = visitExpr(ctx.expr())
        return PartiqlAst.build { callAgg(strategy, ctx.func.text.toLowerCase(), arg) }
    }

    override fun visitCountAll(ctx: PartiQLParser.CountAllContext) = PartiqlAst.build {
        callAgg(all(), ctx.func.text.toLowerCase(), lit(ionInt(1)), metaContainerOf(IsCountStarMeta.instance))
    }

    override fun visitExtract(ctx: PartiQLParser.ExtractContext): PartiqlAst.Expr.Call {
        if (!DATE_TIME_PART_KEYWORDS.contains(ctx.IDENTIFIER().text.toLowerCase())) {
            throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Expected one of: $DATE_TIME_PART_KEYWORDS")
        }
        val datetimePart = PartiqlAst.Expr.Lit(ion.newSymbol(ctx.IDENTIFIER().text).toIonElement())
        val timeExpr = visit(ctx.rhs) as PartiqlAst.Expr
        val args = listOf(datetimePart, timeExpr)
        return PartiqlAst.Expr.Call(SymbolPrimitive(ctx.EXTRACT().text.toLowerCase(), mapOf()), args)
    }

    override fun visitTrimFunction(ctx: PartiQLParser.TrimFunctionContext): PartiqlAst.PartiqlAstNode {
        val modifier = if (ctx.mod != null) ctx.mod.text.toLowerCase().toSymbol() else null
        val substring = if (ctx.sub != null) visitExpr(ctx.sub) else null
        val target = visitExpr(ctx.target)
        var args = listOfNotNull(modifier, substring, target)
        return PartiqlAst.Expr.Call(SymbolPrimitive(ctx.func.text.toLowerCase(), mapOf()), args)
    }

    override fun visitDateFunction(ctx: PartiQLParser.DateFunctionContext): PartiqlAst.Expr.Call {
        if (!DATE_TIME_PART_KEYWORDS.contains(ctx.dt.text.toLowerCase())) {
            throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Expected one of: $DATE_TIME_PART_KEYWORDS")
        }
        val datetimePart = PartiqlAst.Expr.Lit(ion.newSymbol(ctx.dt.text).toIonElement())
        val secondaryArgs = visitOrEmpty(PartiqlAst.Expr::class, ctx.expr())
        val args = listOf(datetimePart) + secondaryArgs
        return PartiqlAst.Expr.Call(SymbolPrimitive(ctx.func.text.toLowerCase(), mapOf()), args, mapOf())
    }

    override fun visitSubstring(ctx: PartiQLParser.SubstringContext): PartiqlAst.Expr.Call {
        val args = ctx.expr().map { expr -> visit(expr) as PartiqlAst.Expr }
        return PartiqlAst.Expr.Call(SymbolPrimitive(ctx.SUBSTRING().text.toLowerCase(), mapOf()), args, mapOf())
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

    /**
     * EXPRESSIONS
     */

    override fun visitExprOr(ctx: PartiQLParser.ExprOrContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitExprAnd(ctx: PartiQLParser.ExprAndContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitMathOp00(ctx: PartiQLParser.MathOp00Context): PartiqlAst.PartiqlAstNode = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitMathOp01(ctx: PartiQLParser.MathOp01Context): PartiqlAst.PartiqlAstNode = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitMathOp02(ctx: PartiQLParser.MathOp02Context): PartiqlAst.PartiqlAstNode = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
    override fun visitPredicateComparison(ctx: PartiQLParser.PredicateComparisonContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op)

    private fun visitBinaryOperation(lhs: ParserRuleContext?, rhs: ParserRuleContext?, op: Token?, parent: ParserRuleContext? = null): PartiqlAst.Expr {
        if (parent != null) return visit(parent) as PartiqlAst.Expr
        val args = visitOrEmpty(PartiqlAst.Expr::class, lhs!!, rhs!!)
        return PartiqlAst.build {
            when (op!!.type) {
                PartiQLParser.AND -> and(args)
                PartiQLParser.OR -> or(args)
                PartiQLParser.ASTERISK -> times(args)
                PartiQLParser.SLASH_FORWARD -> divide(args)
                PartiQLParser.PLUS -> plus(args)
                PartiQLParser.MINUS -> minus(args)
                PartiQLParser.PERCENT -> modulo(args)
                PartiQLParser.CONCAT -> concat(args)
                PartiQLParser.ANGLE_LEFT -> lt(args)
                PartiQLParser.LT_EQ -> lte(args)
                PartiQLParser.ANGLE_RIGHT -> gt(args)
                PartiQLParser.GT_EQ -> gte(args)
                PartiQLParser.NEQ -> ne(args)
                PartiQLParser.EQ -> eq(args)
                else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unknown binary operator")
            }
        }
    }

    private fun visitUnaryOperation(operand: ParserRuleContext?, op: Token?, parent: ParserRuleContext? = null): PartiqlAst.PartiqlAstNode {
        if (parent != null) return visit(parent) as PartiqlAst.Expr
        val arg = visitOrEmpty(PartiqlAst.Expr::class, operand!!)
        return PartiqlAst.build {
            when (op!!.type) {
                PartiQLParser.PLUS -> pos(arg)
                PartiQLParser.MINUS -> neg(arg)
                PartiQLParser.NOT -> not(arg)
                else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unknown unary operator")
            }
        }
    }

    override fun visitValueExpr(ctx: PartiQLParser.ValueExprContext) = visitUnaryOperation(ctx.rhs, ctx.sign, ctx.parent)
    override fun visitExprNot(ctx: PartiQLParser.ExprNotContext) = visitUnaryOperation(ctx.rhs, ctx.op, ctx.parent)

    override fun visitPredicateIn(ctx: PartiQLParser.PredicateInContext): PartiqlAst.PartiqlAstNode {
        val args = visitOrEmpty(PartiqlAst.Expr::class, ctx.lhs, ctx.rhs)
        return PartiqlAst.build { if (ctx.NOT() != null) not(inCollection(args), metaContainerOf(LegacyLogicalNotMeta.instance)) else inCollection(args) }
    }

    override fun visitPredicateIs(ctx: PartiQLParser.PredicateIsContext): PartiqlAst.PartiqlAstNode {
        val lhs = visit(ctx.lhs) as PartiqlAst.Expr
        val rhs = visit(ctx.type()) as PartiqlAst.Type
        val isType = PartiqlAst.build { isType(lhs, rhs) }
        return if (ctx.NOT() == null) isType else PartiqlAst.build { not(isType, metaContainerOf(LegacyLogicalNotMeta.instance)) }
    }

    override fun visitPredicateBetween(ctx: PartiQLParser.PredicateBetweenContext): PartiqlAst.PartiqlAstNode {
        val args = visitOrEmpty(PartiqlAst.Expr::class, ctx.lhs, ctx.lower, ctx.upper)
        val between = PartiqlAst.build { between(args[0], args[1], args[2]) }
        return if (ctx.NOT() == null) between else PartiqlAst.build { not(between, metaContainerOf(LegacyLogicalNotMeta.instance)) }
    }

    override fun visitPredicateLike(ctx: PartiQLParser.PredicateLikeContext): PartiqlAst.PartiqlAstNode {
        val args = visitOrEmpty(PartiqlAst.Expr::class, ctx.lhs, ctx.rhs)
        val escape = if (ctx.escape == null) null else visit(ctx.escape) as PartiqlAst.Expr
        var like: PartiqlAst.Expr = PartiqlAst.BUILDER().like(args[0], args[1], escape)
        return if (ctx.NOT() == null) like else PartiqlAst.build { not(like, metas = metaContainerOf(LegacyLogicalNotMeta.instance)) }
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

    override fun visitValues(ctx: PartiQLParser.ValuesContext): PartiqlAst.Expr.Bag {
        val rows = ctx.valueRow().map { row -> visitValueRow(row) }
        return PartiqlAst.build { bag(rows) }
    }

    override fun visitValueRow(ctx: PartiQLParser.ValueRowContext): PartiqlAst.Expr.List {
        val expressions = ctx.expr().map { expr -> visitExpr(expr) }
        return PartiqlAst.build { list(expressions) }
    }

    override fun visitValueList(ctx: PartiQLParser.ValueListContext): PartiqlAst.Expr.List {
        val expressions = ctx.expr().map { expr -> visitExpr(expr) }
        return PartiqlAst.build { list(expressions) }
    }

    override fun visitCaseExpr(ctx: PartiQLParser.CaseExprContext): PartiqlAst.Expr {
        val exprPairList = mutableListOf<PartiqlAst.ExprPair>()
        val start = if (ctx.case_ == null) 0 else 1
        val end = if (ctx.ELSE() == null) ctx.expr().size else ctx.expr().size - 1
        for (i in start until end step 2) {
            val whenExpr = visitExpr(ctx.expr(i))
            val thenExpr = visitExpr(ctx.expr(i + 1))
            exprPairList.add(PartiqlAst.build { exprPair(whenExpr, thenExpr) })
        }
        val elseExpr = if (ctx.ELSE() != null) visitExpr(ctx.expr(end)) else null
        return PartiqlAst.build {
            when (ctx.case_) {
                null -> searchedCase(exprPairList(exprPairList), elseExpr)
                else -> simpleCase(visitExpr(ctx.case_), exprPairList(exprPairList), elseExpr)
            }
        }
    }

    override fun visitCast(ctx: PartiQLParser.CastContext): PartiqlAst.Expr.Cast {
        val expr = visitExpr(ctx.expr())
        val type = visit(ctx.type()) as PartiqlAst.Type
        return PartiqlAst.BUILDER().cast(expr, type)
    }

    override fun visitCanCast(ctx: PartiQLParser.CanCastContext): PartiqlAst.Expr.CanCast {
        val expr = visitExpr(ctx.expr())
        val type = visit(ctx.type()) as PartiqlAst.Type
        return PartiqlAst.BUILDER().canCast(expr, type)
    }

    override fun visitCanLosslessCast(ctx: PartiQLParser.CanLosslessCastContext): PartiqlAst.Expr.CanLosslessCast {
        val expr = visitExpr(ctx.expr())
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
        val name = ctx.name.getString().toLowerCase()
        val args = ctx.expr().map { arg -> visit(arg) as PartiqlAst.Expr }
        return PartiqlAst.BUILDER().call(name, args)
    }

    override fun visitExprPrimaryPath(ctx: PartiQLParser.ExprPrimaryPathContext): PartiqlAst.PartiqlAstNode {
        val base = visit(ctx.exprPrimary()) as PartiqlAst.Expr
        val steps = ctx.pathStep().map { step -> visit(step) as PartiqlAst.PathStep }
        return PartiqlAst.Expr.Path(base, steps)
    }

    override fun visitPathStepIndexExpr(ctx: PartiQLParser.PathStepIndexExprContext): PartiqlAst.PartiqlAstNode {
        val expr = visit(ctx.key) as PartiqlAst.Expr
        return PartiqlAst.build { pathExpr(expr, PartiqlAst.CaseSensitivity.CaseSensitive(), metaContainerOf(IsPathIndexMeta.instance)) }
    }

    // TODO: VarPathExpr should NOT allow the @ symbol
    override fun visitPathStepDotExpr(ctx: PartiQLParser.PathStepDotExprContext): PartiqlAst.PartiqlAstNode {
        return when (val key = ctx.key) {
            is PartiQLParser.VarRefExprIdentUnquotedContext -> PartiqlAst.build { pathExpr(lit(ion.newString(key.toRawString()).toIonElement()), caseInsensitive()) }
            is PartiQLParser.VarRefExprIdentQuotedContext -> PartiqlAst.build { pathExpr(lit(ion.newString(key.toRawString()).toIonElement()), caseSensitive()) }
            else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unidentifiable path.")
        }
    }

    override fun visitPathStepIndexAll(ctx: PartiQLParser.PathStepIndexAllContext) = PartiqlAst.build { pathWildcard() }
    override fun visitPathStepDotAll(ctx: PartiQLParser.PathStepDotAllContext) = PartiqlAst.build { pathUnpivot() }

    override fun visitArray(ctx: PartiQLParser.ArrayContext) = PartiqlAst.build {
        list(visitOrEmpty(PartiqlAst.Expr::class, ctx.expr()))
    }

    override fun visitSetQuantifierStrategy(ctx: PartiQLParser.SetQuantifierStrategyContext?): PartiqlAst.SetQuantifier? = when {
        ctx == null -> null
        ctx.DISTINCT() != null -> PartiqlAst.SetQuantifier.Distinct()
        ctx.ALL() != null -> PartiqlAst.SetQuantifier.All()
        else -> null
    }

    /**
     * NOT OVERRIDDEN
     */

    override fun visitAsIdent(ctx: PartiQLParser.AsIdentContext?): PartiqlAst.PartiqlAstNode = super.visitAsIdent(ctx)
    override fun visitAtIdent(ctx: PartiQLParser.AtIdentContext?): PartiqlAst.PartiqlAstNode = super.visitAtIdent(ctx)
    override fun visitByIdent(ctx: PartiQLParser.ByIdentContext?): PartiqlAst.PartiqlAstNode = super.visitByIdent(ctx)
    override fun visitTerminal(node: TerminalNode?): PartiqlAst.PartiqlAstNode = super.visitTerminal(node)
    override fun shouldVisitNextChild(node: RuleNode?, currentResult: PartiqlAst.PartiqlAstNode?) = super.shouldVisitNextChild(node, currentResult)
    override fun visitErrorNode(node: ErrorNode?): PartiqlAst.PartiqlAstNode = super.visitErrorNode(node)
    override fun visitChildren(node: RuleNode?): PartiqlAst.PartiqlAstNode = super.visitChildren(node)
    override fun visitExprPrimaryBase(ctx: PartiQLParser.ExprPrimaryBaseContext?): PartiqlAst.PartiqlAstNode = super.visitExprPrimaryBase(ctx)
    override fun visitExprTermBase(ctx: PartiQLParser.ExprTermBaseContext?): PartiqlAst.PartiqlAstNode = super.visitExprTermBase(ctx)
    override fun visitExprTermCollection(ctx: PartiQLParser.ExprTermCollectionContext?): PartiqlAst.PartiqlAstNode = super.visitExprTermCollection(ctx)
    override fun visitPredicateBase(ctx: PartiQLParser.PredicateBaseContext?): PartiqlAst.PartiqlAstNode = super.visitPredicateBase(ctx)
    override fun visitGroupAlias(ctx: PartiQLParser.GroupAliasContext?): PartiqlAst.PartiqlAstNode = super.visitGroupAlias(ctx)

    /**
     *
     * HELPER METHODS
     *
     */

    /**
     * Converts a Path expression into a Projection Item (either ALL or EXPR). Note: A Projection Item only allows a
     * subset of a typical Path expressions. See the following examples.
     *
     * Examples of valid projections are:
     *
     * ```sql
     *      SELECT * FROM foo
     *      SELECT foo.* FROM foo
     *      SELECT f.* FROM foo as f
     *      SELECT foo.bar.* FROM foo
     *      SELECT f.bar.* FROM foo as f
     * ```
     * Also validates that the expression is valid for select list context. It does this by making
     * sure that expressions looking like the following do not appear:
     *
     * ```sql
     *      SELECT foo[*] FROM foo
     *      SELECT f.*.bar FROM foo as f
     *      SELECT foo[1].* FROM foo
     *      SELECT foo.*.bar FROM foo
     * ```
     */
    private fun convertPathToProjectionItem(path: PartiqlAst.Expr.Path, alias: String?): PartiqlAst.ProjectItem {
        val steps = mutableListOf<PartiqlAst.PathStep>()
        var containsIndex = false
        path.steps.forEachIndexed { index, step ->

            // Only last step can have a '.*'
            if (step is PartiqlAst.PathStep.PathUnpivot && index != path.steps.lastIndex) {
                throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Projection item cannot unpivot unless at end.")
            }

            // No step can have an indexed wildcard: '[*]'
            if (step is PartiqlAst.PathStep.PathWildcard) {
                throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Projection item cannot index using wildcard.")
            }

            // If the last step is '.*', no indexing is allowed
            if (step.metas.containsKey(IsPathIndexMeta.TAG)) {
                containsIndex = true
            }

            if (step !is PartiqlAst.PathStep.PathUnpivot) {
                steps.add(step)
            }
        }

        if (path.steps.last() is PartiqlAst.PathStep.PathUnpivot && containsIndex) {
            throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Projection item use wildcard with any indexing.")
        }

        return PartiqlAst.build {
            when {
                path.steps.last() is PartiqlAst.PathStep.PathUnpivot && steps.isEmpty() -> projectAll(path.root)
                path.steps.last() is PartiqlAst.PathStep.PathUnpivot -> projectAll(path(path.root, steps))
                else -> projectExpr(path, asAlias = alias)
            }
        }
    }

    private fun <T : PartiqlAst.PartiqlAstNode> visitOrEmpty(clazz: KClass<T>, ctx: ParserRuleContext): T = clazz.cast(visit(ctx))
    private fun <T : PartiqlAst.PartiqlAstNode> visitOrEmpty(clazz: KClass<T>, ctx: List<ParserRuleContext>): List<T> = ctx.map { clazz.cast(visit(it)) }
    private fun <T : PartiqlAst.PartiqlAstNode> visitOrEmpty(clazz: KClass<T>, vararg ctx: ParserRuleContext): List<T> = when {
        ctx.isNullOrEmpty() -> emptyList()
        else -> visitOrEmpty(clazz, ctx.asList())
    }

    private fun <T : PartiqlAst.PartiqlAstNode> visitOrNull(clazz: KClass<T>, ctx: ParserRuleContext): T? = when (ctx) {
        null -> null
        else -> clazz.cast(visit(ctx))
    }

    private fun visitOrNull(ctx: ParserRuleContext): PartiqlAst.PartiqlAstNode? = when (ctx) {
        null -> null
        else -> visit(ctx)
    }

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

    private fun getStrategy(strategy: PartiQLParser.SetQuantifierStrategyContext?, default: PartiqlAst.SetQuantifier): PartiqlAst.SetQuantifier {
        return when {
            strategy == null -> default
            strategy.DISTINCT() != null -> PartiqlAst.SetQuantifier.Distinct()
            strategy.ALL() != null -> PartiqlAst.SetQuantifier.All()
            else -> default
        }
    }

    private fun getStrategy(strategy: PartiQLParser.SetQuantifierStrategyContext?): PartiqlAst.SetQuantifier? {
        return when {
            strategy == null -> null
            strategy.DISTINCT() != null -> PartiqlAst.SetQuantifier.Distinct()
            else -> null
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
        return when {
            this.IDENTIFIER_QUOTED() != null -> this.IDENTIFIER_QUOTED().text.toPartiQLIdentifier()
            this.IDENTIFIER() != null -> this.IDENTIFIER().text
            else -> throw org.partiql.lang.syntax.PartiQLParser.ParseErrorListener.ParseException("Unable to get symbol's text.")
        }
    }

    private fun String.toInteger() = BigInteger(this, 10)

    private fun String.toSymbol(): PartiqlAst.Expr.Lit {
        val str = this
        return PartiqlAst.build {
            lit(ionSymbol(str))
        }
    }
}

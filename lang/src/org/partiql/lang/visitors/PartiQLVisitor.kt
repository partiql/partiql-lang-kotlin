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

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.toIonElement
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.grammar.parser.generated.PartiQLBaseVisitor
import org.partiql.grammar.parser.generated.PartiQLParser
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.IsImplictJoinMeta
import org.partiql.lang.ast.IsIonLiteralMeta
import org.partiql.lang.ast.IsPathIndexMeta
import org.partiql.lang.ast.IsValuesExprMeta
import org.partiql.lang.ast.LegacyLogicalNotMeta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.time.MAX_PRECISION_FOR_TIME
import org.partiql.lang.syntax.DATE_TIME_PART_KEYWORDS
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.TRIM_SPECIFICATION_KEYWORDS
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.DATE_PATTERN_REGEX
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.error
import org.partiql.lang.util.getPrecisionFromTimeString
import org.partiql.pig.runtime.SymbolPrimitive
import java.lang.IndexOutOfBoundsException
import java.math.BigInteger
import java.time.LocalDate
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
internal class PartiQLVisitor(val ion: IonSystem, val customTypes: List<CustomType> = listOf(), private val parameterIndexes: Map<Int, Int> = mapOf()) :
    PartiQLBaseVisitor<PartiqlAst.PartiqlAstNode>() {

    private val customKeywords = customTypes.map { it.name.toLowerCase() }

    private val customTypeAliases =
        customTypes.map { customType ->
            customType.aliases.map { alias ->
                Pair(alias.toLowerCase(), customType.name.toLowerCase())
            }
        }.flatten().toMap()

    /**
     *
     * TOP LEVEL
     *
     */

    override fun visitQueryDql(ctx: PartiQLParser.QueryDqlContext) = visitDql(ctx.dql())

    override fun visitQueryDml(ctx: PartiQLParser.QueryDmlContext): PartiqlAst.PartiqlAstNode = visit(ctx.dml())

    /**
     *
     * COMMON USAGES
     *
     */

    override fun visitAsIdent(ctx: PartiQLParser.AsIdentContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

    override fun visitAtIdent(ctx: PartiQLParser.AtIdentContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

    override fun visitByIdent(ctx: PartiQLParser.ByIdentContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

    override fun visitSymbolPrimitive(ctx: PartiQLParser.SymbolPrimitiveContext) = PartiqlAst.build {
        val metas = ctx.ident.getSourceMetaContainer()
        when (ctx.ident.type) {
            PartiQLParser.IDENTIFIER_QUOTED -> id(ctx.IDENTIFIER_QUOTED().getStringValue(), caseSensitive(), unqualified(), metas)
            PartiQLParser.IDENTIFIER -> id(ctx.IDENTIFIER().getStringValue(), caseInsensitive(), unqualified(), metas)
            else -> throw ParserException("Invalid symbol reference.", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    /**
     *
     * DATA DEFINITION LANGUAGE (DDL)
     *
     */

    override fun visitQueryDdl(ctx: PartiQLParser.QueryDdlContext) = PartiqlAst.build {
        val op = visitDdl(ctx.ddl()) as PartiqlAst.DdlOp
        ddl(op, op.metas)
    }

    override fun visitDropTable(ctx: PartiQLParser.DropTableContext) = PartiqlAst.build {
        val id = visitSymbolPrimitive(ctx.symbolPrimitive())
        dropTable(id.toIdentifier(), ctx.DROP().getSourceMetaContainer())
    }

    override fun visitDropIndex(ctx: PartiQLParser.DropIndexContext) = PartiqlAst.build {
        val id = visitSymbolPrimitive(ctx.target)
        val key = visitSymbolPrimitive(ctx.on)
        dropIndex(key.toIdentifier(), id.toIdentifier(), ctx.DROP().getSourceMetaContainer())
    }

    override fun visitCreateTable(ctx: PartiQLParser.CreateTableContext) = PartiqlAst.build {
        val name = visitSymbolPrimitive(ctx.symbolPrimitive()).name
        createTable_(name, ctx.CREATE().getSourceMetaContainer())
    }

    override fun visitCreateIndex(ctx: PartiQLParser.CreateIndexContext) = PartiqlAst.build {
        val id = visitSymbolPrimitive(ctx.symbolPrimitive())
        val fields = ctx.pathSimple().map { path -> visitPathSimple(path) }
        createIndex(id.toIdentifier(), fields, ctx.CREATE().getSourceMetaContainer())
    }

    /**
     *
     * EXECUTE
     *
     */

    override fun visitQueryExec(ctx: PartiQLParser.QueryExecContext) = visitExecCommand(ctx.execCommand())

    override fun visitExecCommand(ctx: PartiQLParser.ExecCommandContext) = PartiqlAst.build {
        val name = visitExpr(ctx.expr(0)).getStringValue(ctx.expr(0).getStart())
        val args = visitOrEmpty(ctx.args, PartiqlAst.Expr::class)
        exec(name, args, ctx.EXEC().getSourceMetaContainer())
    }

    /**
     *
     * DATA MANIPULATION LANGUAGE (DML)
     *
     */

    override fun visitDmlBaseWrapper(ctx: PartiQLParser.DmlBaseWrapperContext) = PartiqlAst.build {
        val sourceContext = when {
            ctx.updateClause() != null -> ctx.updateClause()
            ctx.fromClause() != null -> ctx.fromClause()
            else -> throw ParserException("Unable to deduce from source in DML", ErrorCode.PARSE_INVALID_QUERY)
        }
        val from = visitOrNull(sourceContext, PartiqlAst.FromSource::class)
        val where = visitOrNull(ctx.whereClause(), PartiqlAst.Expr::class)
        val returning = visitOrNull(ctx.returningClause(), PartiqlAst.ReturningExpr::class)
        val operations = ctx.dmlBaseCommand().map { command -> getCommandList(visit(command)) }.flatten()
        dml(dmlOpList(operations, operations[0].metas), from, where, returning, metas = operations[0].metas)
    }

    override fun visitDmlBase(ctx: PartiQLParser.DmlBaseContext) = PartiqlAst.build {
        val commands = getCommandList(visit(ctx.dmlBaseCommand()))
        dml(dmlOpList(commands, commands[0].metas), metas = commands[0].metas)
    }

    private fun getCommandList(command: PartiqlAst.PartiqlAstNode): List<PartiqlAst.DmlOp> {
        return when (command) {
            is PartiqlAst.DmlOpList -> command.ops
            is PartiqlAst.DmlOp -> listOf(command)
            else -> throw ParserException("Unable to grab DML operation.", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    override fun visitRemoveCommand(ctx: PartiQLParser.RemoveCommandContext) = PartiqlAst.build {
        val target = visitPathSimple(ctx.pathSimple())
        remove(target, ctx.REMOVE().getSourceMetaContainer())
    }

    override fun visitDeleteCommand(ctx: PartiQLParser.DeleteCommandContext) = PartiqlAst.build {
        val from = visit(ctx.fromClauseSimple(), PartiqlAst.FromSource::class)
        val where = visitOrNull(ctx.whereClause(), PartiqlAst.Expr::class)
        val returning = visitOrNull(ctx.returningClause(), PartiqlAst.ReturningExpr::class)
        dml(dmlOpList(delete(ctx.DELETE().getSourceMetaContainer()), metas = ctx.DELETE().getSourceMetaContainer()), from, where, returning, ctx.DELETE().getSourceMetaContainer())
    }

    override fun visitInsertSimple(ctx: PartiQLParser.InsertSimpleContext) = PartiqlAst.build {
        val target = visitPathSimple(ctx.pathSimple())
        insert(target, visit(ctx.value, PartiqlAst.Expr::class), ctx.INSERT().getSourceMetaContainer())
    }

    override fun visitInsertValue(ctx: PartiQLParser.InsertValueContext) = PartiqlAst.build {
        val target = visitPathSimple(ctx.pathSimple())
        val index = visitOrNull(ctx.pos, PartiqlAst.Expr::class)
        val onConflict = visitOrNull(ctx.onConflict(), PartiqlAst.OnConflict::class)
        insertValue(target, visit(ctx.value, PartiqlAst.Expr::class), index = index, onConflict = onConflict, ctx.INSERT().getSourceMetaContainer())
    }

    // FIXME: See `FIXME #001` in file `PartiQL.g4`.
    override fun visitInsertCommandReturning(ctx: PartiQLParser.InsertCommandReturningContext) = PartiqlAst.build {
        val target = visitPathSimple(ctx.pathSimple())
        val index = visitOrNull(ctx.pos, PartiqlAst.Expr::class)
        val onConflict = visitOrNull(ctx.onConflict(), PartiqlAst.OnConflict::class)
        val returning = visitOrNull(ctx.returningClause(), PartiqlAst.ReturningExpr::class)
        dml(
            dmlOpList(
                insertValue(target, visit(ctx.value, PartiqlAst.Expr::class), index = index, onConflict = onConflict, ctx.INSERT().getSourceMetaContainer()),
                metas = ctx.INSERT().getSourceMetaContainer()
            ),
            returning = returning,
            metas = ctx.INSERT().getSourceMetaContainer()
        )
    }

    override fun visitReturningClause(ctx: PartiQLParser.ReturningClauseContext) = PartiqlAst.build {
        val elements = visitOrEmpty(ctx.returningColumn(), PartiqlAst.ReturningElem::class)
        returningExpr(elements, ctx.RETURNING().getSourceMetaContainer())
    }

    private fun getReturningMapping(status: Token, age: Token) = PartiqlAst.build {
        when {
            status.type == PartiQLParser.MODIFIED && age.type == PartiQLParser.NEW -> modifiedNew()
            status.type == PartiQLParser.MODIFIED && age.type == PartiQLParser.OLD -> modifiedOld()
            status.type == PartiQLParser.ALL && age.type == PartiQLParser.NEW -> allNew()
            status.type == PartiQLParser.ALL && age.type == PartiQLParser.OLD -> allOld()
            else -> throw status.err("Unable to get return mapping.", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        }
    }

    override fun visitReturningColumn(ctx: PartiQLParser.ReturningColumnContext) = PartiqlAst.build {
        val column = when (ctx.ASTERISK()) {
            null -> returningColumn(visitExpr(ctx.expr()))
            else -> returningWildcard()
        }
        returningElem(getReturningMapping(ctx.status, ctx.age), column)
    }

    override fun visitOnConflict(ctx: PartiQLParser.OnConflictContext) = PartiqlAst.build {
        onConflict(visitExpr(ctx.expr()), doNothing(), ctx.ON().getSourceMetaContainer())
    }

    override fun visitPathSimple(ctx: PartiQLParser.PathSimpleContext) = PartiqlAst.build {
        val root = visitSymbolPrimitive(ctx.symbolPrimitive())
        if (ctx.pathSimpleSteps().isEmpty()) return@build root
        val steps = visitOrEmpty(ctx.pathSimpleSteps(), PartiqlAst.PathStep::class)
        path(root, steps, root.metas)
    }

    override fun visitPathSimpleLiteral(ctx: PartiQLParser.PathSimpleLiteralContext) = PartiqlAst.build {
        pathExpr(visit(ctx.literal()) as PartiqlAst.Expr, caseSensitive())
    }

    override fun visitPathSimpleSymbol(ctx: PartiQLParser.PathSimpleSymbolContext) = PartiqlAst.build {
        pathExpr(visitSymbolPrimitive(ctx.symbolPrimitive()), caseSensitive())
    }

    override fun visitPathSimpleDotSymbol(ctx: PartiQLParser.PathSimpleDotSymbolContext) = getSymbolPathExpr(ctx.symbolPrimitive())

    override fun visitSetCommand(ctx: PartiQLParser.SetCommandContext) = PartiqlAst.build {
        val assignments = visitOrEmpty(ctx.setAssignment(), PartiqlAst.DmlOp.Set::class)
        val newSets = assignments.map { assignment -> assignment.copy(metas = ctx.SET().getSourceMetaContainer()) }
        dmlOpList(newSets, ctx.SET().getSourceMetaContainer())
    }

    override fun visitSetAssignment(ctx: PartiQLParser.SetAssignmentContext) = PartiqlAst.build {
        set(assignment(visitPathSimple(ctx.pathSimple()), visitExpr(ctx.expr())))
    }

    override fun visitUpdateClause(ctx: PartiQLParser.UpdateClauseContext) = visit(ctx.tableBaseReference(), PartiqlAst.FromSource::class)

    /**
     *
     * DATA QUERY LANGUAGE (DQL)
     *
     */

    override fun visitDql(ctx: PartiQLParser.DqlContext) = PartiqlAst.build {
        val query = visit(ctx.expr(), PartiqlAst.Expr::class)
        query(query, query.metas)
    }

    override fun visitQueryBase(ctx: PartiQLParser.QueryBaseContext) = visit(ctx.exprSelect(), PartiqlAst.Expr::class)

    override fun visitSfwQuery(ctx: PartiQLParser.SfwQueryContext) = PartiqlAst.build {
        val projection = visit(ctx.select, PartiqlAst.Projection::class)
        val strategy = getSetQuantifierStrategy(ctx.select)
        val from = visitFromClause(ctx.from)
        val order = visitOrNull(ctx.order, PartiqlAst.OrderBy::class)
        val group = visitOrNull(ctx.group, PartiqlAst.GroupBy::class)
        val limit = visitOrNull(ctx.limit, PartiqlAst.Expr::class)
        val offset = visitOrNull(ctx.offset, PartiqlAst.Expr::class)
        val where = visitOrNull(ctx.where, PartiqlAst.Expr::class)
        val having = visitOrNull(ctx.having, PartiqlAst.Expr::class)
        val let = visitOrNull(ctx.let, PartiqlAst.Let::class)
        val metas = ctx.selectClause().getMetas()
        select(
            project = projection,
            from = from,
            setq = strategy,
            order = order,
            group = group,
            limit = limit,
            offset = offset,
            where = where,
            having = having,
            fromLet = let,
            metas = metas
        )
    }

    /**
     *
     * SELECT & PROJECTIONS
     *
     */

    override fun visitSetQuantifierStrategy(ctx: PartiQLParser.SetQuantifierStrategyContext?): PartiqlAst.SetQuantifier? = when {
        ctx == null -> null
        ctx.DISTINCT() != null -> PartiqlAst.SetQuantifier.Distinct()
        ctx.ALL() != null -> PartiqlAst.SetQuantifier.All()
        else -> null
    }

    override fun visitSelectAll(ctx: PartiQLParser.SelectAllContext) = PartiqlAst.build {
        projectStar(ctx.ASTERISK().getSourceMetaContainer())
    }

    override fun visitSelectItems(ctx: PartiQLParser.SelectItemsContext) = convertProjectionItems(ctx.projectionItems(), ctx.SELECT().getSourceMetaContainer())

    override fun visitSelectPivot(ctx: PartiQLParser.SelectPivotContext) = PartiqlAst.build {
        projectPivot(visitExpr(ctx.at), visitExpr(ctx.pivot))
    }

    override fun visitSelectValue(ctx: PartiQLParser.SelectValueContext) = PartiqlAst.build {
        projectValue(visitExpr(ctx.expr()))
    }

    override fun visitProjectionItem(ctx: PartiQLParser.ProjectionItemContext) = PartiqlAst.build {
        val expr = visit(ctx.expr(), PartiqlAst.Expr::class)
        val alias = visitOrNull(ctx.symbolPrimitive(), PartiqlAst.Expr.Id::class)?.name
        if (expr is PartiqlAst.Expr.Path) convertPathToProjectionItem(expr, alias)
        else projectExpr_(expr, asAlias = alias, expr.metas)
    }

    /**
     *
     * SIMPLE CLAUSES
     *
     */

    override fun visitLimitClause(ctx: PartiQLParser.LimitClauseContext): PartiqlAst.Expr = visit(ctx.arg, PartiqlAst.Expr::class)

    override fun visitExpr(ctx: PartiQLParser.ExprContext) = visit(ctx.exprBagOp(), PartiqlAst.Expr::class)

    override fun visitOffsetByClause(ctx: PartiQLParser.OffsetByClauseContext) = visit(ctx.arg, PartiqlAst.Expr::class)

    override fun visitWhereClause(ctx: PartiQLParser.WhereClauseContext) = visitExpr(ctx.arg)

    override fun visitWhereClauseSelect(ctx: PartiQLParser.WhereClauseSelectContext) = visit(ctx.arg, PartiqlAst.Expr::class)

    override fun visitHavingClause(ctx: PartiQLParser.HavingClauseContext) = visit(ctx.arg, PartiqlAst.Expr::class)

    /**
     *
     * LET CLAUSE
     *
     */

    override fun visitLetClause(ctx: PartiQLParser.LetClauseContext) = PartiqlAst.build {
        val letBindings = visitOrEmpty(ctx.letBinding(), PartiqlAst.LetBinding::class)
        let(letBindings)
    }

    override fun visitLetBinding(ctx: PartiQLParser.LetBindingContext) = PartiqlAst.build {
        val expr = visit(ctx.expr(), PartiqlAst.Expr::class)
        val metas = ctx.symbolPrimitive().getSourceMetaContainer()
        letBinding_(expr, convertSymbolPrimitive(ctx.symbolPrimitive())!!, metas)
    }

    /**
     *
     * ORDER BY CLAUSE
     *
     */

    override fun visitOrderByClause(ctx: PartiQLParser.OrderByClauseContext) = PartiqlAst.build {
        val sortSpecs = visitOrEmpty(ctx.orderSortSpec(), PartiqlAst.SortSpec::class)
        val metas = ctx.ORDER().getSourceMetaContainer()
        orderBy(sortSpecs, metas)
    }

    override fun visitOrderSortSpec(ctx: PartiQLParser.OrderSortSpecContext) = PartiqlAst.build {
        val expr = visit(ctx.expr(), PartiqlAst.Expr::class)
        val orderSpec = when {
            ctx.dir == null -> asc()
            ctx.dir.type == PartiQLParser.ASC -> asc()
            ctx.dir.type == PartiQLParser.DESC -> desc()
            else -> throw ctx.dir.err("Invalid query syntax", ErrorCode.PARSE_INVALID_QUERY)
        }
        val nullSpec = when {
            ctx.nulls == null && orderSpec is PartiqlAst.OrderingSpec.Desc -> nullsFirst()
            ctx.nulls == null -> nullsLast()
            ctx.nulls.type == PartiQLParser.FIRST -> nullsFirst()
            ctx.nulls.type == PartiQLParser.LAST -> nullsLast()
            else -> nullsLast()
        }
        sortSpec(expr, orderingSpec = orderSpec, nullsSpec = nullSpec)
    }

    /**
     *
     * GROUP BY CLAUSE
     *
     */

    override fun visitGroupClause(ctx: PartiQLParser.GroupClauseContext) = PartiqlAst.build {
        val strategy = if (ctx.PARTIAL() != null) groupPartial() else groupFull()
        val keys = visitOrEmpty(ctx.groupKey(), PartiqlAst.GroupKey::class)
        val keyList = groupKeyList(keys)
        val alias = visitOrNull(ctx.groupAlias(), PartiqlAst.Expr.Id::class).toPigSymbolPrimitive()
        groupBy_(strategy, keyList = keyList, groupAsAlias = alias, ctx.GROUP().getSourceMetaContainer())
    }

    override fun visitGroupAlias(ctx: PartiQLParser.GroupAliasContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

    /**
     * Returns a GROUP BY key
     * TODO: Support ordinal case. Also, the conditional defining the exception is odd. 1 + 1 is allowed, but 2 is not.
     *  This is to match the functionality of SqlParser, but this should likely be adjusted.
     */
    override fun visitGroupKey(ctx: PartiQLParser.GroupKeyContext) = PartiqlAst.build {
        val expr = visit(ctx.key, PartiqlAst.Expr::class)
        val possibleLiteral = when (expr) {
            is PartiqlAst.Expr.Pos -> expr.expr
            is PartiqlAst.Expr.Neg -> expr.expr
            else -> expr
        }
        if (
            (possibleLiteral is PartiqlAst.Expr.Lit && possibleLiteral.value != ionNull()) ||
            possibleLiteral is PartiqlAst.Expr.LitTime || possibleLiteral is PartiqlAst.Expr.Date
        ) {
            throw ctx.key.getStart().err(
                "Literals (including ordinals) not supported in GROUP BY",
                ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY
            )
        }
        val alias = visitOrNull(ctx.symbolPrimitive(), PartiqlAst.Expr.Id::class).toPigSymbolPrimitive()
        groupKey_(expr, asAlias = alias, expr.metas)
    }

    /**
     *
     * BAG OPERATIONS
     *
     */

    override fun visitIntersect(ctx: PartiQLParser.IntersectContext) = PartiqlAst.build {
        val lhs = visit(ctx.lhs, PartiqlAst.Expr::class)
        val rhs = visit(ctx.rhs, PartiqlAst.Expr::class)
        val quantifier = if (ctx.ALL() != null) all() else distinct()
        val (intersect, metas) = when (ctx.OUTER()) {
            null -> intersect() to ctx.INTERSECT().getSourceMetaContainer()
            else -> outerIntersect() to ctx.OUTER().getSourceMetaContainer()
        }
        bagOp(intersect, quantifier, listOf(lhs, rhs), metas)
    }

    override fun visitExcept(ctx: PartiQLParser.ExceptContext) = PartiqlAst.build {
        val lhs = visit(ctx.lhs, PartiqlAst.Expr::class)
        val rhs = visit(ctx.rhs, PartiqlAst.Expr::class)
        val quantifier = if (ctx.ALL() != null) all() else distinct()
        val (except, metas) = when (ctx.OUTER()) {
            null -> except() to ctx.EXCEPT().getSourceMetaContainer()
            else -> outerExcept() to ctx.OUTER().getSourceMetaContainer()
        }
        bagOp(except, quantifier, listOf(lhs, rhs), metas)
    }

    override fun visitUnion(ctx: PartiQLParser.UnionContext) = PartiqlAst.build {
        val lhs = visit(ctx.lhs, PartiqlAst.Expr::class)
        val rhs = visit(ctx.rhs, PartiqlAst.Expr::class)
        val quantifier = if (ctx.ALL() != null) all() else distinct()
        val (union, metas) = when (ctx.OUTER()) {
            null -> union() to ctx.UNION().getSourceMetaContainer()
            else -> outerUnion() to ctx.OUTER().getSourceMetaContainer()
        }
        bagOp(union, quantifier, listOf(lhs, rhs), metas)
    }

    /**
     *
     * GRAPH PATTERN MANIPULATION LANGUAGE (GPML)
     *
     */

    override fun visitMatchExpr(ctx: PartiQLParser.MatchExprContext) = PartiqlAst.build {
        val selector = visitOrNull(ctx.matchSelector(), PartiqlAst.GraphMatchSelector::class)
        val pattern = visitMatchPattern(ctx.matchPattern())
        graphMatchExpr(selector, listOf(pattern))
    }

    override fun visitMatchExprList(ctx: PartiQLParser.MatchExprListContext) = PartiqlAst.build {
        val selector = visitOrNull(ctx.matchSelector(), PartiqlAst.GraphMatchSelector::class)
        val patterns = ctx.matchPattern().map { pattern -> visitMatchPattern(pattern) }
        graphMatchExpr(selector, patterns)
    }

    override fun visitMatchPattern(ctx: PartiQLParser.MatchPatternContext) = PartiqlAst.build {
        val parts = visitOrEmpty(ctx.graphPart(), PartiqlAst.GraphMatchPatternPart::class)
        val restrictor = visitOrNull(ctx.restrictor, PartiqlAst.GraphMatchRestrictor::class)
        val variable = visitOrNull(ctx.variable, PartiqlAst.Expr.Id::class)?.name
        graphMatchPattern_(parts = parts, restrictor = restrictor, variable = variable)
    }

    override fun visitPatternPathVariable(ctx: PartiQLParser.PatternPathVariableContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

    override fun visitSelectorBasic(ctx: PartiQLParser.SelectorBasicContext) = PartiqlAst.build {
        val metas = ctx.mod.getSourceMetaContainer()
        when (ctx.mod.type) {
            PartiQLParser.ANY -> selectorAnyShortest(metas)
            PartiQLParser.ALL -> selectorAllShortest(metas)
            else -> throw ParserException("Unsupported match selector.", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    override fun visitSelectorAny(ctx: PartiQLParser.SelectorAnyContext) = PartiqlAst.build {
        val metas = ctx.ANY().getSourceMetaContainer()
        when (ctx.k) {
            null -> selectorAny(metas)
            else -> selectorAnyK(ctx.k.text.toLong(), metas)
        }
    }

    override fun visitSelectorShortest(ctx: PartiQLParser.SelectorShortestContext) = PartiqlAst.build {
        val k = ctx.k.text.toLong()
        val metas = ctx.k.getSourceMetaContainer()
        when (ctx.GROUP()) {
            null -> selectorShortestK(k, metas)
            else -> selectorShortestKGroup(k, metas)
        }
    }

    override fun visitPatternPartLabel(ctx: PartiQLParser.PatternPartLabelContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

    override fun visitPattern(ctx: PartiQLParser.PatternContext) = PartiqlAst.build {
        val restrictor = visitOrNull(ctx.restrictor, PartiqlAst.GraphMatchRestrictor::class)
        val variable = visitOrNull(ctx.variable, PartiqlAst.Expr.Id::class)?.name
        val prefilter = visitOrNull(ctx.where, PartiqlAst.Expr::class)
        val quantifier = visitOrNull(ctx.quantifier, PartiqlAst.GraphMatchQuantifier::class)
        val parts = visitOrEmpty(ctx.graphPart(), PartiqlAst.GraphMatchPatternPart::class)
        pattern(
            graphMatchPattern_(parts = parts, variable = variable, restrictor = restrictor, quantifier = quantifier, prefilter = prefilter)
        )
    }

    override fun visitEdgeAbbreviated(ctx: PartiQLParser.EdgeAbbreviatedContext) = PartiqlAst.build {
        val direction = visitEdgeAbbrev(ctx.edgeAbbrev())
        val quantifier = visitOrNull(ctx.quantifier, PartiqlAst.GraphMatchQuantifier::class)
        edge(direction = direction, quantifier = quantifier)
    }

    override fun visitEdgeWithSpec(ctx: PartiQLParser.EdgeWithSpecContext) = PartiqlAst.build {
        val quantifier = visitOrNull(ctx.quantifier, PartiqlAst.GraphMatchQuantifier::class)
        val edge = visitOrNull(ctx.edgeWSpec(), PartiqlAst.GraphMatchPatternPart.Edge::class)
        edge!!.copy(quantifier = quantifier)
    }

    override fun visitEdgeSpec(ctx: PartiQLParser.EdgeSpecContext) = PartiqlAst.build {
        val placeholderDirection = edgeRight()
        val variable = visitOrNull(ctx.symbolPrimitive(), PartiqlAst.Expr.Id::class)?.name
        val prefilter = visitOrNull(ctx.whereClause(), PartiqlAst.Expr::class)
        val label = visitOrNull(ctx.patternPartLabel(), PartiqlAst.Expr.Id::class)?.name
        edge_(direction = placeholderDirection, variable = variable, prefilter = prefilter, label = listOfNotNull(label))
    }

    override fun visitEdgeSpecLeft(ctx: PartiQLParser.EdgeSpecLeftContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeLeft())
    }

    override fun visitEdgeSpecRight(ctx: PartiQLParser.EdgeSpecRightContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeRight())
    }

    override fun visitEdgeSpecBidirectional(ctx: PartiQLParser.EdgeSpecBidirectionalContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeLeftOrRight())
    }

    override fun visitEdgeSpecUndirectedBidirectional(ctx: PartiQLParser.EdgeSpecUndirectedBidirectionalContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeLeftOrUndirectedOrRight())
    }

    override fun visitEdgeSpecUndirected(ctx: PartiQLParser.EdgeSpecUndirectedContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeUndirected())
    }

    override fun visitEdgeSpecUndirectedLeft(ctx: PartiQLParser.EdgeSpecUndirectedLeftContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeLeftOrUndirected())
    }

    override fun visitEdgeSpecUndirectedRight(ctx: PartiQLParser.EdgeSpecUndirectedRightContext) = PartiqlAst.build {
        val edge = visitEdgeSpec(ctx.edgeSpec())
        edge.copy(direction = edgeUndirectedOrRight())
    }

    override fun visitEdgeAbbrev(ctx: PartiQLParser.EdgeAbbrevContext) = PartiqlAst.build {
        when {
            ctx.TILDE() != null && ctx.ANGLE_RIGHT() != null -> edgeUndirectedOrRight()
            ctx.TILDE() != null && ctx.ANGLE_LEFT() != null -> edgeLeftOrUndirected()
            ctx.TILDE() != null -> edgeUndirected()
            ctx.MINUS() != null && ctx.ANGLE_LEFT() != null && ctx.ANGLE_RIGHT() != null -> edgeLeftOrRight()
            ctx.MINUS() != null && ctx.ANGLE_LEFT() != null -> edgeLeft()
            ctx.MINUS() != null && ctx.ANGLE_RIGHT() != null -> edgeRight()
            ctx.MINUS() != null -> edgeLeftOrUndirectedOrRight()
            else -> throw ParserException("Unsupported edge type", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    override fun visitPatternQuantifier(ctx: PartiQLParser.PatternQuantifierContext) = PartiqlAst.build {
        when {
            ctx.quant == null -> graphMatchQuantifier(ctx.lower.text.toLong(), ctx.upper?.text?.toLong())
            ctx.quant.type == PartiQLParser.PLUS -> graphMatchQuantifier(1L)
            ctx.quant.type == PartiQLParser.ASTERISK -> graphMatchQuantifier(0L)
            else -> throw ParserException("Unsupported quantifier", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    override fun visitNode(ctx: PartiQLParser.NodeContext) = PartiqlAst.build {
        val variable = visitOrNull(ctx.symbolPrimitive(), PartiqlAst.Expr.Id::class)?.name
        val prefilter = visitOrNull(ctx.whereClause(), PartiqlAst.Expr::class)
        val label = visitOrNull(ctx.patternPartLabel(), PartiqlAst.Expr.Id::class)?.name
        node_(variable = variable, prefilter = prefilter, label = listOfNotNull(label))
    }

    override fun visitPatternRestrictor(ctx: PartiQLParser.PatternRestrictorContext) = PartiqlAst.build {
        val metas = ctx.restrictor.getSourceMetaContainer()
        when (ctx.restrictor.text.toLowerCase()) {
            "trail" -> restrictorTrail(metas)
            "acyclic" -> restrictorAcyclic(metas)
            "simple" -> restrictorSimple(metas)
            else -> throw ParserException("Unrecognized pattern restrictor", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    /**
     *
     * TABLE REFERENCES & JOINS & FROM CLAUSE
     *
     */

    override fun visitFromClause(ctx: PartiQLParser.FromClauseContext) = visit(ctx.tableReference(), PartiqlAst.FromSource::class)

    override fun visitTableBaseRefClauses(ctx: PartiQLParser.TableBaseRefClausesContext) = PartiqlAst.build {
        val expr = visit(ctx.source, PartiqlAst.Expr::class)
        val (asAlias, atAlias, byAlias) = visitNullableItems(listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()), PartiqlAst.Expr.Id::class)
        scan_(expr, asAlias = asAlias.toPigSymbolPrimitive(), byAlias = byAlias.toPigSymbolPrimitive(), atAlias = atAlias.toPigSymbolPrimitive(), metas = expr.metas)
    }

    override fun visitMatchSingle(ctx: PartiQLParser.MatchSingleContext) = PartiqlAst.build {
        val source = visitExpr(ctx.lhs)
        val metas = ctx.MATCH().getSourceMetaContainer()
        val graphExpr = visitMatchExpr(ctx.matchExpr())
        graphMatch(source, graphExpr, metas)
    }

    override fun visitMatchMultiple(ctx: PartiQLParser.MatchMultipleContext) = PartiqlAst.build {
        val source = visitExpr(ctx.lhs)
        val metas = ctx.MATCH().getSourceMetaContainer()
        val graphExpr = visitMatchExprList(ctx.matchExprList())
        graphMatch(source, graphExpr, metas)
    }

    override fun visitFromClauseSimpleExplicit(ctx: PartiQLParser.FromClauseSimpleExplicitContext) = PartiqlAst.build {
        val expr = visitPathSimple(ctx.pathSimple())
        val (asAlias, atAlias, byAlias) = visitNullableItems(listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()), PartiqlAst.Expr.Id::class)
        scan_(expr, asAlias = asAlias.toPigSymbolPrimitive(), byAlias = byAlias.toPigSymbolPrimitive(), atAlias = atAlias.toPigSymbolPrimitive(), metas = expr.metas)
    }

    override fun visitTableUnpivot(ctx: PartiQLParser.TableUnpivotContext) = PartiqlAst.build {
        val expr = visitExpr(ctx.expr())
        val metas = ctx.UNPIVOT().getSourceMetaContainer()
        val (asAlias, atAlias, byAlias) = visitNullableItems(listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()), PartiqlAst.Expr.Id::class)
        unpivot_(expr, asAlias = asAlias.toPigSymbolPrimitive(), atAlias = atAlias.toPigSymbolPrimitive(), byAlias = byAlias.toPigSymbolPrimitive(), metas)
    }

    override fun visitTableCrossJoin(ctx: PartiQLParser.TableCrossJoinContext) = PartiqlAst.build {
        val lhs = visit(ctx.lhs, PartiqlAst.FromSource::class)
        val joinType = visitJoinType(ctx.joinType())
        val rhs = visit(ctx.rhs, PartiqlAst.FromSource::class)
        val metas = metaContainerOf(IsImplictJoinMeta.instance)
        join(joinType, lhs, rhs, metas = metas)
    }

    override fun visitTableQualifiedJoin(ctx: PartiQLParser.TableQualifiedJoinContext) = PartiqlAst.build {
        val lhs = visit(ctx.lhs, PartiqlAst.FromSource::class)
        val joinType = visitJoinType(ctx.joinType())
        val rhs = visit(ctx.rhs, PartiqlAst.FromSource::class)
        val condition = visitOrNull(ctx.joinSpec(), PartiqlAst.Expr::class)
        join(joinType, lhs, rhs, condition)
    }

    override fun visitTableBaseRefSymbol(ctx: PartiQLParser.TableBaseRefSymbolContext) = PartiqlAst.build {
        val expr = visit(ctx.source, PartiqlAst.Expr::class)
        val name = visitOrNull(ctx.symbolPrimitive(), PartiqlAst.Expr.Id::class)?.name
        scan_(expr, name, metas = expr.metas)
    }

    override fun visitFromClauseSimpleImplicit(ctx: PartiQLParser.FromClauseSimpleImplicitContext) = PartiqlAst.build {
        val path = visitPathSimple(ctx.pathSimple())
        val name = visitOrNull(ctx.symbolPrimitive(), PartiqlAst.Expr.Id::class)?.name
        scan_(path, name, metas = path.metas)
    }

    override fun visitTableWrapped(ctx: PartiQLParser.TableWrappedContext): PartiqlAst.PartiqlAstNode = visit(ctx.tableReference())

    override fun visitJoinSpec(ctx: PartiQLParser.JoinSpecContext) = visitExpr(ctx.expr())

    override fun visitJoinType(ctx: PartiQLParser.JoinTypeContext?) = PartiqlAst.build {
        when {
            ctx == null -> inner()
            ctx.LEFT() != null -> left()
            ctx.RIGHT() != null -> right()
            ctx.INNER() != null -> inner()
            ctx.FULL() != null -> full()
            ctx.OUTER() != null -> full()
            else -> inner()
        }
    }

    override fun visitJoinRhsTableJoined(ctx: PartiQLParser.JoinRhsTableJoinedContext) = visit(ctx.tableReference(), PartiqlAst.FromSource::class)

    /**
     * SIMPLE EXPRESSIONS
     */

    override fun visitOr(ctx: PartiQLParser.OrContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.OR().symbol, null)

    override fun visitAnd(ctx: PartiQLParser.AndContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, null)

    override fun visitNot(ctx: PartiQLParser.NotContext) = visitUnaryOperation(ctx.rhs, ctx.op, null)

    override fun visitMathOp00(ctx: PartiQLParser.MathOp00Context): PartiqlAst.PartiqlAstNode = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)

    override fun visitMathOp01(ctx: PartiQLParser.MathOp01Context): PartiqlAst.PartiqlAstNode = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)

    override fun visitMathOp02(ctx: PartiQLParser.MathOp02Context): PartiqlAst.PartiqlAstNode = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)

    override fun visitValueExpr(ctx: PartiQLParser.ValueExprContext) = visitUnaryOperation(ctx.rhs, ctx.sign, ctx.parent)

    /**
     *
     * PREDICATES
     *
     */

    override fun visitPredicateComparison(ctx: PartiQLParser.PredicateComparisonContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op)

    /**
     * Note: This predicate can take a wrapped expression on the RHS, and it will wrap it in a LIST. However, if the
     * expression is a SELECT or VALUES expression, it will NOT wrap it in a list. This is per SqlParser.
     */
    override fun visitPredicateIn(ctx: PartiQLParser.PredicateInContext) = PartiqlAst.build {
        // Wrap Expression with LIST unless SELECT / VALUES
        val rhs = if (ctx.expr() != null) {
            val possibleRhs = visitExpr(ctx.expr())
            if (possibleRhs is PartiqlAst.Expr.Select || possibleRhs.metas.containsKey(IsValuesExprMeta.TAG))
                possibleRhs
            else list(possibleRhs)
        } else {
            visit(ctx.rhs, PartiqlAst.Expr::class)
        }
        val lhs = visit(ctx.lhs, PartiqlAst.Expr::class)
        val args = listOf(lhs, rhs)
        val inCollection = inCollection(args, ctx.IN().getSourceMetaContainer())
        if (ctx.NOT() == null) return@build inCollection
        not(inCollection, ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
    }

    override fun visitPredicateIs(ctx: PartiQLParser.PredicateIsContext) = PartiqlAst.build {
        val lhs = visit(ctx.lhs, PartiqlAst.Expr::class)
        val rhs = visit(ctx.type(), PartiqlAst.Type::class)
        val isType = isType(lhs, rhs, ctx.IS().getSourceMetaContainer())
        if (ctx.NOT() == null) return@build isType
        not(isType, ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
    }

    override fun visitPredicateBetween(ctx: PartiQLParser.PredicateBetweenContext) = PartiqlAst.build {
        val args = visitOrEmpty(listOf(ctx.lhs, ctx.lower, ctx.upper), PartiqlAst.Expr::class)
        val between = between(args[0], args[1], args[2], ctx.BETWEEN().getSourceMetaContainer())
        if (ctx.NOT() == null) return@build between
        not(between, ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
    }

    override fun visitPredicateLike(ctx: PartiQLParser.PredicateLikeContext) = PartiqlAst.build {
        val args = visitOrEmpty(listOf(ctx.lhs, ctx.rhs), PartiqlAst.Expr::class)
        val escape = visitOrNull(ctx.escape, PartiqlAst.Expr::class)
        val like = like(args[0], args[1], escape, ctx.LIKE().getSourceMetaContainer())
        if (ctx.NOT() == null) return@build like
        not(like, metas = ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
    }

    /**
     *
     * PRIMARY EXPRESSIONS
     *
     */

    override fun visitExprTermWrappedQuery(ctx: PartiQLParser.ExprTermWrappedQueryContext) = visit(ctx.expr(), PartiqlAst.Expr::class)

    override fun visitVarRefExpr(ctx: PartiQLParser.VarRefExprContext): PartiqlAst.PartiqlAstNode = PartiqlAst.build {
        val metas = ctx.ident.getSourceMetaContainer()
        val qualifier = if (ctx.qualifier == null) unqualified() else localsFirst()
        val sensitivity = if (ctx.ident.type == PartiQLParser.IDENTIFIER) caseInsensitive() else caseSensitive()
        id(ctx.ident.getStringValue(), sensitivity, qualifier, metas)
    }

    override fun visitParameter(ctx: PartiQLParser.ParameterContext) = PartiqlAst.build {
        val parameterIndex = parameterIndexes[ctx.QUESTION_MARK().symbol.tokenIndex]
            ?: throw ParserException("Unable to find index of parameter.", ErrorCode.PARSE_INVALID_QUERY)
        parameter(parameterIndex.toLong(), ctx.QUESTION_MARK().getSourceMetaContainer())
    }

    override fun visitSequenceConstructor(ctx: PartiQLParser.SequenceConstructorContext) = PartiqlAst.build {
        val expressions = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        val metas = ctx.datatype.getSourceMetaContainer()
        when (ctx.datatype.type) {
            PartiQLParser.LIST -> list(expressions, metas)
            PartiQLParser.SEXP -> sexp(expressions, metas)
            else -> throw ParserException("Unknown sequence", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    override fun visitExprPrimaryPath(ctx: PartiQLParser.ExprPrimaryPathContext) = PartiqlAst.build {
        val base = visit(ctx.exprPrimary()) as PartiqlAst.Expr
        val steps = ctx.pathStep().map { step -> visit(step) as PartiqlAst.PathStep }
        path(base, steps, base.metas)
    }

    override fun visitPathStepIndexExpr(ctx: PartiQLParser.PathStepIndexExprContext) = PartiqlAst.build {
        val expr = visit(ctx.key, PartiqlAst.Expr::class)
        pathExpr(expr, PartiqlAst.CaseSensitivity.CaseSensitive(), metaContainerOf(IsPathIndexMeta.instance))
    }

    override fun visitPathStepDotExpr(ctx: PartiQLParser.PathStepDotExprContext) = getSymbolPathExpr(ctx.key)

    override fun visitPathStepIndexAll(ctx: PartiQLParser.PathStepIndexAllContext) = PartiqlAst.build {
        pathWildcard()
    }

    override fun visitPathStepDotAll(ctx: PartiQLParser.PathStepDotAllContext) = PartiqlAst.build {
        pathUnpivot()
    }

    override fun visitValues(ctx: PartiQLParser.ValuesContext) = PartiqlAst.build {
        val rows = visitOrEmpty(ctx.valueRow(), PartiqlAst.Expr.List::class)
        bag(rows, ctx.VALUES().getSourceMetaContainer() + metaContainerOf(IsValuesExprMeta.instance))
    }

    override fun visitValueRow(ctx: PartiQLParser.ValueRowContext) = PartiqlAst.build {
        val expressions = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        list(expressions)
    }

    override fun visitValueList(ctx: PartiQLParser.ValueListContext) = PartiqlAst.build {
        val expressions = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        list(expressions)
    }

    /**
     *
     * FUNCTIONS
     *
     */

    override fun visitNullIf(ctx: PartiQLParser.NullIfContext) = PartiqlAst.build {
        val lhs = visitExpr(ctx.expr(0))
        val rhs = visitExpr(ctx.expr(1))
        val metas = ctx.NULLIF().getSourceMetaContainer()
        nullIf(lhs, rhs, metas)
    }

    override fun visitCoalesce(ctx: PartiQLParser.CoalesceContext) = PartiqlAst.build {
        val expressions = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        val metas = ctx.COALESCE().getSourceMetaContainer()
        coalesce(expressions, metas)
    }

    override fun visitCaseExpr(ctx: PartiQLParser.CaseExprContext) = PartiqlAst.build {
        val pairs = ctx.whens.indices.map { i ->
            exprPair(visitExpr(ctx.whens[i]), visitExpr(ctx.thens[i]))
        }
        val elseExpr = visitOrNull(ctx.else_, PartiqlAst.Expr::class)
        val caseMeta = ctx.CASE().getSourceMetaContainer()
        when (ctx.case_) {
            null -> searchedCase(exprPairList(pairs), elseExpr, metas = caseMeta)
            else -> simpleCase(visitExpr(ctx.case_), exprPairList(pairs), elseExpr, metas = caseMeta)
        }
    }

    override fun visitCast(ctx: PartiQLParser.CastContext) = PartiqlAst.build {
        val expr = visitExpr(ctx.expr())
        val type = visit(ctx.type(), PartiqlAst.Type::class)
        cast(expr, type)
    }

    override fun visitCanCast(ctx: PartiQLParser.CanCastContext) = PartiqlAst.build {
        val expr = visitExpr(ctx.expr())
        val type = visit(ctx.type(), PartiqlAst.Type::class)
        canCast(expr, type)
    }

    override fun visitCanLosslessCast(ctx: PartiQLParser.CanLosslessCastContext) = PartiqlAst.build {
        val expr = visitExpr(ctx.expr())
        val type = visit(ctx.type(), PartiqlAst.Type::class)
        canLosslessCast(expr, type)
    }

    override fun visitFunctionCallIdent(ctx: PartiQLParser.FunctionCallIdentContext) = PartiqlAst.build {
        val name = ctx.name.getString().toLowerCase()
        val args = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        val metas = ctx.name.getSourceMetaContainer()
        call(name, args = args, metas = metas)
    }

    override fun visitFunctionCallReserved(ctx: PartiQLParser.FunctionCallReservedContext) = PartiqlAst.build {
        val name = ctx.name.text.toLowerCase()
        val args = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        val metas = ctx.name.getSourceMetaContainer()
        call(name, args = args, metas = metas)
    }

    override fun visitDateFunction(ctx: PartiQLParser.DateFunctionContext) = PartiqlAst.build {
        if (!DATE_TIME_PART_KEYWORDS.contains(ctx.dt.text.toLowerCase())) {
            throw ctx.dt.err("Expected one of: $DATE_TIME_PART_KEYWORDS", ErrorCode.PARSE_EXPECTED_DATE_TIME_PART)
        }
        val datetimePart = lit(ion.newSymbol(ctx.dt.text).toIonElement())
        val secondaryArgs = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        val args = listOf(datetimePart) + secondaryArgs
        val metas = ctx.func.getSourceMetaContainer()
        call(ctx.func.text.toLowerCase(), args, metas)
    }

    override fun visitSubstring(ctx: PartiQLParser.SubstringContext) = PartiqlAst.build {
        val args = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        val metas = ctx.SUBSTRING().getSourceMetaContainer()
        call(ctx.SUBSTRING().text.toLowerCase(), args, metas)
    }

    override fun visitCountAll(ctx: PartiQLParser.CountAllContext) = PartiqlAst.build {
        callAgg(
            all(),
            ctx.func.text.toLowerCase(),
            lit(ionInt(1)),
            ctx.COUNT().getSourceMetaContainer() + metaContainerOf(IsCountStarMeta.instance)
        )
    }

    override fun visitExtract(ctx: PartiQLParser.ExtractContext) = PartiqlAst.build {
        if (!DATE_TIME_PART_KEYWORDS.contains(ctx.IDENTIFIER().text.toLowerCase())) {
            throw ctx.IDENTIFIER().err("Expected one of: $DATE_TIME_PART_KEYWORDS", ErrorCode.PARSE_EXPECTED_DATE_TIME_PART)
        }
        val datetimePart = lit(ion.newSymbol(ctx.IDENTIFIER().text).toIonElement())
        val timeExpr = visit(ctx.rhs, PartiqlAst.Expr::class)
        val args = listOf(datetimePart, timeExpr)
        val metas = ctx.EXTRACT().getSourceMetaContainer()
        call(ctx.EXTRACT().text.toLowerCase(), args, metas)
    }

    /**
     * Note: This implementation is odd because the TRIM function contains keywords that are not keywords outside
     * of TRIM. Therefore, TRIM(<spec> <substring> FROM <target>) needs to be parsed as below. The <spec> needs to be
     * an identifier (according to SqlParser), but if the identifier is NOT a trim specification, and the <substring> is
     * null, we need to make the substring equal to the <spec> (and make <spec> null).
     */
    override fun visitTrimFunction(ctx: PartiQLParser.TrimFunctionContext) = PartiqlAst.build {
        val possibleModText = if (ctx.mod != null) ctx.mod.text.toLowerCase() else null
        val isTrimSpec = TRIM_SPECIFICATION_KEYWORDS.contains(possibleModText)
        val (modifier, substring) = when {
            ctx.mod != null && ctx.sub == null -> {
                if (isTrimSpec) ctx.mod.toSymbol() to null
                else null to id(possibleModText!!, caseInsensitive(), unqualified(), ctx.mod.getSourceMetaContainer())
            }
            ctx.mod == null && ctx.sub != null -> {
                null to visitExpr(ctx.sub)
            }
            ctx.mod != null && ctx.sub != null -> {
                if (isTrimSpec) ctx.mod.toSymbol() to visitExpr(ctx.sub)
                else throw ParserException("Expected one of: $TRIM_SPECIFICATION_KEYWORDS", ErrorCode.PARSE_INVALID_QUERY)
            }
            else -> null to null
        }

        val target = visitExpr(ctx.target)
        val args = listOfNotNull(modifier, substring, target)
        val metas = ctx.func.getSourceMetaContainer()
        call(ctx.func.text.toLowerCase(), args, metas)
    }

    override fun visitAggregateBase(ctx: PartiQLParser.AggregateBaseContext) = PartiqlAst.build {
        val strategy = getStrategy(ctx.setQuantifierStrategy(), default = all())
        val arg = visitExpr(ctx.expr())
        val metas = ctx.func.getSourceMetaContainer()
        callAgg(strategy, ctx.func.text.toLowerCase(), arg, metas)
    }

    /**
     *
     * LITERALS
     *
     */

    override fun visitBag(ctx: PartiQLParser.BagContext) = PartiqlAst.build {
        val exprList = visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class)
        bag(exprList, ctx.ANGLE_DOUBLE_LEFT().getSourceMetaContainer())
    }

    override fun visitLiteralDecimal(ctx: PartiQLParser.LiteralDecimalContext) = PartiqlAst.build {
        val decimal = try {
            ion.newDecimal(bigDecimalOf(ctx.LITERAL_DECIMAL().text)).toIonElement()
        } catch (e: NumberFormatException) {
            val errorContext = PropertyValueMap()
            errorContext[Property.TOKEN_STRING] = ctx.LITERAL_DECIMAL().text
            throw ctx.LITERAL_DECIMAL().err("Invalid decimal literal", ErrorCode.LEXER_INVALID_LITERAL, errorContext)
        }
        lit(
            decimal,
            ctx.LITERAL_DECIMAL().getSourceMetaContainer()
        )
    }

    override fun visitArray(ctx: PartiQLParser.ArrayContext) = PartiqlAst.build {
        list(visitOrEmpty(ctx.expr(), PartiqlAst.Expr::class))
    }

    override fun visitLiteralNull(ctx: PartiQLParser.LiteralNullContext) = PartiqlAst.build {
        lit(ionNull(), ctx.NULL().getSourceMetaContainer())
    }

    override fun visitLiteralMissing(ctx: PartiQLParser.LiteralMissingContext) = PartiqlAst.build {
        missing(ctx.MISSING().getSourceMetaContainer())
    }

    override fun visitLiteralTrue(ctx: PartiQLParser.LiteralTrueContext) = PartiqlAst.build {
        lit(ionBool(true), ctx.TRUE().getSourceMetaContainer())
    }

    override fun visitLiteralFalse(ctx: PartiQLParser.LiteralFalseContext) = PartiqlAst.build {
        lit(ionBool(false), ctx.FALSE().getSourceMetaContainer())
    }

    override fun visitLiteralIon(ctx: PartiQLParser.LiteralIonContext) = PartiqlAst.build {
        lit(
            ion.singleValue(ctx.ION_CLOSURE().getStringValue()).toIonElement(),
            ctx.ION_CLOSURE().getSourceMetaContainer() + metaContainerOf(IsIonLiteralMeta.instance)
        )
    }

    override fun visitLiteralString(ctx: PartiQLParser.LiteralStringContext) = PartiqlAst.build {
        lit(ionString(ctx.LITERAL_STRING().getStringValue()), ctx.LITERAL_STRING().getSourceMetaContainer())
    }

    override fun visitLiteralInteger(ctx: PartiQLParser.LiteralIntegerContext): PartiqlAst.Expr.Lit = PartiqlAst.build {
        lit(ion.newInt(BigInteger(ctx.LITERAL_INTEGER().text, 10)).toIonElement(), ctx.LITERAL_INTEGER().getSourceMetaContainer())
    }

    override fun visitLiteralDate(ctx: PartiQLParser.LiteralDateContext) = PartiqlAst.build {
        val dateString = ctx.LITERAL_STRING().getStringValue()
        if (DATE_PATTERN_REGEX.matches(dateString).not()) {
            throw ctx.LITERAL_STRING().err("Expected DATE string to be of the format yyyy-MM-dd", ErrorCode.PARSE_INVALID_DATE_STRING)
        }
        try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            val (year, month, day) = dateString.split("-")
            date(year.toLong(), month.toLong(), day.toLong(), ctx.DATE().getSourceMetaContainer())
        } catch (e: DateTimeParseException) {
            throw ctx.LITERAL_STRING().err(e.localizedMessage, ErrorCode.PARSE_INVALID_DATE_STRING, cause = e)
        } catch (e: IndexOutOfBoundsException) {
            throw ctx.LITERAL_STRING().err(e.localizedMessage, ErrorCode.PARSE_INVALID_DATE_STRING, cause = e)
        }
    }

    override fun visitLiteralTime(ctx: PartiQLParser.LiteralTimeContext) = PartiqlAst.build {
        val (timeString, precision) = getTimeStringAndPrecision(ctx.LITERAL_STRING(), ctx.LITERAL_INTEGER())
        when (ctx.WITH()) {
            null -> getLocalTime(timeString, false, precision, ctx.LITERAL_STRING(), ctx.TIME(0))
            else -> getOffsetTime(timeString, precision, ctx.LITERAL_STRING(), ctx.TIME(0))
        }
    }

    override fun visitTuple(ctx: PartiQLParser.TupleContext) = PartiqlAst.build {
        val pairs = visitOrEmpty(ctx.pair(), PartiqlAst.ExprPair::class)
        struct(pairs)
    }

    override fun visitPair(ctx: PartiQLParser.PairContext) = PartiqlAst.build {
        val lhs = visitExpr(ctx.lhs)
        val rhs = visitExpr(ctx.rhs)
        exprPair(lhs, rhs)
    }

    /**
     *
     * TYPES
     *
     */

    override fun visitTypeAtomic(ctx: PartiQLParser.TypeAtomicContext) = PartiqlAst.build {
        val metas = ctx.datatype.getSourceMetaContainer()
        when (ctx.datatype.type) {
            PartiQLParser.NULL -> nullType(metas)
            PartiQLParser.BOOL -> scalarType("bool", emptyList(), metas)
            PartiQLParser.BOOLEAN -> scalarType("boolean", emptyList(), metas)
            PartiQLParser.SMALLINT -> scalarType("smallint", emptyList(), metas)
            PartiQLParser.INT2 -> scalarType("int2", emptyList(), metas)
            PartiQLParser.INTEGER2 -> scalarType("integer2", emptyList(), metas)
            PartiQLParser.INT -> scalarType("int", emptyList(), metas)
            PartiQLParser.INTEGER -> scalarType("integer", emptyList(), metas)
            PartiQLParser.INT4 -> scalarType("int4", emptyList(), metas)
            PartiQLParser.INTEGER4 -> scalarType("integer4", emptyList(), metas)
            PartiQLParser.INT8 -> scalarType("int8", emptyList(), metas)
            PartiQLParser.INTEGER8 -> scalarType("integer8", emptyList(), metas)
            PartiQLParser.BIGINT -> scalarType("bigint", emptyList(), metas)
            PartiQLParser.REAL -> scalarType("real", emptyList(), metas)
            PartiQLParser.DOUBLE -> scalarType("double_precision", emptyList(), metas)
            PartiQLParser.TIMESTAMP -> scalarType("timestamp", emptyList(), metas)
            PartiQLParser.CHAR -> scalarType("char", emptyList(), metas)
            PartiQLParser.CHARACTER -> scalarType("character", emptyList(), metas)
            PartiQLParser.MISSING -> missingType(metas)
            PartiQLParser.STRING -> scalarType("string", emptyList(), metas)
            PartiQLParser.SYMBOL -> scalarType("symbol", emptyList(), metas)
            PartiQLParser.BLOB -> scalarType("blob", emptyList(), metas)
            PartiQLParser.CLOB -> scalarType("clob", emptyList(), metas)
            PartiQLParser.DATE -> scalarType("date", emptyList(), metas)
            PartiQLParser.STRUCT -> structType(metas)
            PartiQLParser.TUPLE -> tupleType(metas)
            PartiQLParser.LIST -> listType(metas)
            PartiQLParser.BAG -> bagType(metas)
            PartiQLParser.SEXP -> sexpType(metas)
            PartiQLParser.ANY -> anyType(metas)
            else -> throw ParserException("Unsupported type.", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    override fun visitTypeVarChar(ctx: PartiQLParser.TypeVarCharContext) = PartiqlAst.build {
        val arg0 = if (ctx.arg0 != null) ion.newInt(BigInteger(ctx.arg0.text, 10)) else null
        assertIntegerValue(ctx.arg0, arg0)
        scalarType("varchar", listOfNotNull(arg0?.longValue()))
    }

    override fun visitTypeArgSingle(ctx: PartiQLParser.TypeArgSingleContext) = PartiqlAst.build {
        val arg0 = if (ctx.arg0 != null) ion.newInt(BigInteger(ctx.arg0.text, 10)) else null
        assertIntegerValue(ctx.arg0, arg0)
        when (ctx.datatype.type) {
            PartiQLParser.FLOAT -> scalarType("float", listOfNotNull(arg0?.longValue()))
            PartiQLParser.CHAR -> scalarType("char", listOfNotNull(arg0?.longValue()))
            PartiQLParser.CHARACTER -> scalarType("character", listOfNotNull(arg0?.longValue()))
            PartiQLParser.VARCHAR -> scalarType("varchar", listOfNotNull(arg0?.longValue()))
            else -> throw ParserException("Unknown datatype", ErrorCode.PARSE_UNEXPECTED_TOKEN, PropertyValueMap())
        }
    }

    override fun visitTypeArgDouble(ctx: PartiQLParser.TypeArgDoubleContext) = PartiqlAst.build {
        val arg0 = if (ctx.arg0 != null) ion.newInt(BigInteger(ctx.arg0.text, 10)) else null
        val arg1 = if (ctx.arg1 != null) ion.newInt(BigInteger(ctx.arg1.text, 10)) else null
        assertIntegerValue(ctx.arg0, arg0)
        assertIntegerValue(ctx.arg1, arg1)
        when (ctx.datatype.type) {
            PartiQLParser.DECIMAL -> scalarType("decimal", listOfNotNull(arg0?.longValue(), arg1?.longValue()))
            PartiQLParser.DEC -> scalarType("dec", listOfNotNull(arg0?.longValue(), arg1?.longValue()))
            PartiQLParser.NUMERIC -> scalarType("numeric", listOfNotNull(arg0?.longValue(), arg1?.longValue()))
            else -> throw ParserException("Unknown datatype", ErrorCode.PARSE_UNEXPECTED_TOKEN, PropertyValueMap())
        }
    }

    override fun visitTypeTimeZone(ctx: PartiQLParser.TypeTimeZoneContext) = PartiqlAst.build {
        val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong() else null
        if (precision != null && (precision < 0 || precision > MAX_PRECISION_FOR_TIME)) {
            throw ctx.precision.err("Unsupported precision", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME)
        }
        if (ctx.WITH() == null) return@build scalarType("time", listOfNotNull(precision))
        scalarType("time_with_time_zone", listOfNotNull(precision))
    }

    override fun visitTypeCustom(ctx: PartiQLParser.TypeCustomContext) = PartiqlAst.build {
        val customName: String = when (val name = ctx.symbolPrimitive().getString().toLowerCase()) {
            in customKeywords -> name
            in customTypeAliases.keys -> customTypeAliases.getOrDefault(name, name)
            else -> throw ParserException("Invalid custom type name: $name", ErrorCode.PARSE_INVALID_QUERY)
        }
        scalarType(customName, emptyList())
    }

    /**
     * NOT OVERRIDDEN
     * Explicitly defining the override helps by showing the user (via the IDE) which methods remain to be overridden.
     */

    override fun visitTerminal(node: TerminalNode?): PartiqlAst.PartiqlAstNode = super.visitTerminal(node)
    override fun shouldVisitNextChild(node: RuleNode?, currentResult: PartiqlAst.PartiqlAstNode?) = super.shouldVisitNextChild(node, currentResult)
    override fun visitErrorNode(node: ErrorNode?): PartiqlAst.PartiqlAstNode = super.visitErrorNode(node)
    override fun visitChildren(node: RuleNode?): PartiqlAst.PartiqlAstNode = super.visitChildren(node)
    override fun visitExprPrimaryBase(ctx: PartiQLParser.ExprPrimaryBaseContext?): PartiqlAst.PartiqlAstNode = super.visitExprPrimaryBase(ctx)
    override fun visitExprTermBase(ctx: PartiQLParser.ExprTermBaseContext?): PartiqlAst.PartiqlAstNode = super.visitExprTermBase(ctx)
    override fun visitCollection(ctx: PartiQLParser.CollectionContext?): PartiqlAst.PartiqlAstNode = super.visitCollection(ctx)
    override fun visitPredicateBase(ctx: PartiQLParser.PredicateBaseContext?): PartiqlAst.PartiqlAstNode = super.visitPredicateBase(ctx)
    override fun visitTableJoined(ctx: PartiQLParser.TableJoinedContext?): PartiqlAst.PartiqlAstNode = super.visitTableJoined(ctx)
    override fun visitTableNonJoin(ctx: PartiQLParser.TableNonJoinContext?): PartiqlAst.PartiqlAstNode = super.visitTableNonJoin(ctx)
    override fun visitTableRefBase(ctx: PartiQLParser.TableRefBaseContext?): PartiqlAst.PartiqlAstNode = super.visitTableRefBase(ctx)
    override fun visitJoinRhsBase(ctx: PartiQLParser.JoinRhsBaseContext?): PartiqlAst.PartiqlAstNode = super.visitJoinRhsBase(ctx)

    /**
     *
     * HELPER METHODS
     *
     */

    private fun <T : PartiqlAst.PartiqlAstNode> visitOrEmpty(ctx: List<ParserRuleContext>?, clazz: KClass<T>): List<T> = when {
        ctx.isNullOrEmpty() -> emptyList()
        else -> ctx.map { clazz.cast(visit(it)) }
    }

    private fun <T : PartiqlAst.PartiqlAstNode> visitNullableItems(ctx: List<ParserRuleContext>?, clazz: KClass<T>): List<T?> = when {
        ctx.isNullOrEmpty() -> emptyList()
        else -> ctx.map { visitOrNull(it, clazz) }
    }

    private fun <T : PartiqlAst.PartiqlAstNode> visitOrNull(ctx: ParserRuleContext?, clazz: KClass<T>): T? = when (ctx) {
        null -> null
        else -> clazz.cast(visit(ctx))
    }

    private fun <T : PartiqlAst.PartiqlAstNode> visit(ctx: ParserRuleContext, clazz: KClass<T>): T = clazz.cast(visit(ctx))

    private fun TerminalNode?.getSourceMetaContainer(): MetaContainer {
        if (this == null) return emptyMetaContainer()
        val metas = this.getSourceMetas()
        return com.amazon.ionelement.api.metaContainerOf(Pair(metas.tag, metas))
    }

    private fun Token?.getSourceMetaContainer(): MetaContainer {
        if (this == null) return emptyMetaContainer()
        val metas = this.getSourceMetas()
        return com.amazon.ionelement.api.metaContainerOf(Pair(metas.tag, metas))
    }

    private fun TerminalNode.getSourceMetas(): SourceLocationMeta = this.symbol.getSourceMetas()

    private fun Token.getSourceMetas(): SourceLocationMeta {
        val length = this.stopIndex - this.startIndex + 1
        return SourceLocationMeta(this.line.toLong(), this.charPositionInLine.toLong() + 1, length.toLong())
    }

    private fun visitBinaryOperation(lhs: ParserRuleContext?, rhs: ParserRuleContext?, op: Token?, parent: ParserRuleContext? = null) = PartiqlAst.build {
        if (parent != null) return@build visit(parent, PartiqlAst.Expr::class)
        val args = visitOrEmpty(listOf(lhs!!, rhs!!), PartiqlAst.Expr::class)
        val metas = op.getSourceMetaContainer()
        when (op!!.type) {
            PartiQLParser.AND -> and(args, metas)
            PartiQLParser.OR -> or(args, metas)
            PartiQLParser.ASTERISK -> times(args, metas)
            PartiQLParser.SLASH_FORWARD -> divide(args, metas)
            PartiQLParser.PLUS -> plus(args, metas)
            PartiQLParser.MINUS -> minus(args, metas)
            PartiQLParser.PERCENT -> modulo(args, metas)
            PartiQLParser.CONCAT -> concat(args, metas)
            PartiQLParser.ANGLE_LEFT -> lt(args, metas)
            PartiQLParser.LT_EQ -> lte(args, metas)
            PartiQLParser.ANGLE_RIGHT -> gt(args, metas)
            PartiQLParser.GT_EQ -> gte(args, metas)
            PartiQLParser.NEQ -> ne(args, metas)
            PartiQLParser.EQ -> eq(args, metas)
            else -> throw ParserException("Unknown binary operator", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    private fun visitUnaryOperation(operand: ParserRuleContext?, op: Token?, parent: ParserRuleContext? = null) = PartiqlAst.build {
        if (parent != null) return@build visit(parent, PartiqlAst.Expr::class)
        val arg = visit(operand!!, PartiqlAst.Expr::class)
        val metas = op.getSourceMetaContainer()
        when (op!!.type) {
            PartiQLParser.PLUS -> pos(arg, metas)
            PartiQLParser.MINUS -> neg(arg, metas)
            PartiQLParser.NOT -> not(arg, metas)
            else -> throw ParserException("Unknown unary operator", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    private fun PartiQLParser.SymbolPrimitiveContext.getSourceMetaContainer() = when (this.ident.type) {
        PartiQLParser.IDENTIFIER -> this.IDENTIFIER().getSourceMetaContainer()
        PartiQLParser.IDENTIFIER_QUOTED -> this.IDENTIFIER_QUOTED().getSourceMetaContainer()
        else -> throw ParserException("Unable to get identifier's source meta-container.", ErrorCode.PARSE_INVALID_QUERY)
    }

    private fun PartiqlAst.Expr.getStringValue(token: Token? = null): String = when (this) {
        is PartiqlAst.Expr.Id -> this.name.text.toLowerCase()
        is PartiqlAst.Expr.Lit -> {
            when (this.value) {
                is SymbolElement -> this.value.symbolValue.toLowerCase()
                is StringElement -> this.value.stringValue.toLowerCase()
                else ->
                    this.value.stringValueOrNull ?: throw token.err("Unable to pass the string value", ErrorCode.PARSE_UNEXPECTED_TOKEN)
            }
        }
        else -> throw token.err("Unable to get value", ErrorCode.PARSE_UNEXPECTED_TOKEN)
    }

    private fun PartiqlAst.Expr.Id?.toPigSymbolPrimitive(): SymbolPrimitive? = when (this) {
        null -> null
        else -> this.name.copy(metas = this.metas)
    }

    private fun PartiqlAst.Expr.Id.toIdentifier(): PartiqlAst.Identifier {
        val name = this.name.text
        val case = this.case
        return PartiqlAst.build {
            identifier(name, case)
        }
    }

    /**
     * With the <string> and <int> nodes of a literal time expression, returns the parsed string and precision.
     * TIME (<int>)? (WITH TIME ZONE)? <string>
     */
    private fun getTimeStringAndPrecision(stringNode: TerminalNode, integerNode: TerminalNode?): Pair<String, Long> {
        val timeString = stringNode.getStringValue()
        val precision = when (integerNode) {
            null -> try {
                getPrecisionFromTimeString(timeString).toLong()
            } catch (e: EvaluationException) {
                throw stringNode.err(
                    "Unable to parse precision.", ErrorCode.PARSE_INVALID_TIME_STRING,
                    cause = e
                )
            }
            else -> integerNode.text.toInteger().toLong()
        }
        if (precision < 0 || precision > MAX_PRECISION_FOR_TIME) {
            throw integerNode.err("Precision out of bounds", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME)
        }
        return timeString to precision
    }

    /**
     * Parses a [timeString] using [OffsetTime] and converts to a [PartiqlAst.Expr.LitTime]. If unable to parse, parses
     * using [getLocalTime].
     */
    private fun getOffsetTime(timeString: String, precision: Long, stringNode: TerminalNode, timeNode: TerminalNode) = PartiqlAst.build {
        try {
            val time: OffsetTime = OffsetTime.parse(timeString)
            litTime(
                timeValue(
                    time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                    precision, true, (time.offset.totalSeconds / 60).toLong()
                )
            )
        } catch (e: DateTimeParseException) {
            getLocalTime(timeString, true, precision, stringNode, timeNode)
        }
    }

    /**
     * Parses a [timeString] using [LocalTime] and converts to a [PartiqlAst.Expr.LitTime]
     */
    private fun getLocalTime(timeString: String, withTimeZone: Boolean, precision: Long, stringNode: TerminalNode, timeNode: TerminalNode) = PartiqlAst.build {
        val time: LocalTime
        val formatter = when (withTimeZone) {
            false -> DateTimeFormatter.ISO_TIME
            else -> DateTimeFormatter.ISO_LOCAL_TIME
        }
        try {
            time = LocalTime.parse(timeString, formatter)
        } catch (e: DateTimeParseException) {
            throw stringNode.err("Unable to parse time", ErrorCode.PARSE_INVALID_TIME_STRING, cause = e)
        }
        litTime(
            timeValue(
                time.hour.toLong(), time.minute.toLong(), time.second.toLong(),
                time.nano.toLong(), precision, withTimeZone, null,
                stringNode.getSourceMetaContainer()
            ),
            timeNode.getSourceMetaContainer()
        )
    }

    private fun convertSymbolPrimitive(sym: PartiQLParser.SymbolPrimitiveContext?): SymbolPrimitive? = when (sym) {
        null -> null
        else -> SymbolPrimitive(sym.getString(), sym.getSourceMetaContainer())
    }

    private fun convertProjectionItems(ctx: PartiQLParser.ProjectionItemsContext, metas: MetaContainer) = PartiqlAst.build {
        val projections = visitOrEmpty(ctx.projectionItem(), PartiqlAst.ProjectItem::class)
        projectList(projections, metas)
    }

    private fun PartiQLParser.SelectClauseContext.getMetas(): MetaContainer = when (this) {
        is PartiQLParser.SelectAllContext -> this.SELECT().getSourceMetaContainer()
        is PartiQLParser.SelectItemsContext -> this.SELECT().getSourceMetaContainer()
        is PartiQLParser.SelectValueContext -> this.SELECT().getSourceMetaContainer()
        is PartiQLParser.SelectPivotContext -> this.PIVOT().getSourceMetaContainer()
        else -> throw ParserException("Unknown meta location.", ErrorCode.PARSE_INVALID_QUERY)
    }

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
    protected fun convertPathToProjectionItem(path: PartiqlAst.Expr.Path, alias: SymbolPrimitive?) = PartiqlAst.build {
        val steps = mutableListOf<PartiqlAst.PathStep>()
        var containsIndex = false
        path.steps.forEachIndexed { index, step ->

            // Only last step can have a '.*'
            if (step is PartiqlAst.PathStep.PathUnpivot && index != path.steps.lastIndex) {
                throw ParserException("Projection item cannot unpivot unless at end.", ErrorCode.PARSE_INVALID_QUERY)
            }

            // No step can have an indexed wildcard: '[*]'
            if (step is PartiqlAst.PathStep.PathWildcard) {
                throw ParserException("Projection item cannot index using wildcard.", ErrorCode.PARSE_INVALID_QUERY)
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
            throw ParserException("Projection item use wildcard with any indexing.", ErrorCode.PARSE_INVALID_QUERY)
        }

        when {
            path.steps.last() is PartiqlAst.PathStep.PathUnpivot && steps.isEmpty() -> projectAll(path.root, path.metas)
            path.steps.last() is PartiqlAst.PathStep.PathUnpivot -> projectAll(path(path.root, steps, path.metas), path.metas)
            else -> projectExpr_(path, asAlias = alias, path.metas)
        }
    }

    private fun TerminalNode.getStringValue(): String = this.symbol.getStringValue()

    private fun Token.getStringValue(): String = when (this.type) {
        PartiQLParser.IDENTIFIER -> this.text
        PartiQLParser.IDENTIFIER_QUOTED -> this.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
        PartiQLParser.LITERAL_STRING -> this.text.removePrefix("'").removeSuffix("'").replace("''", "'")
        PartiQLParser.ION_CLOSURE -> this.text.removePrefix("`").removeSuffix("`")
        else -> throw this.err("Unsupported token for grabbing string value.", ErrorCode.PARSE_INVALID_QUERY)
    }

    private fun getStrategy(strategy: PartiQLParser.SetQuantifierStrategyContext?, default: PartiqlAst.SetQuantifier) = PartiqlAst.build {
        when {
            strategy == null -> default
            strategy.DISTINCT() != null -> distinct()
            strategy.ALL() != null -> all()
            else -> default
        }
    }

    private fun getStrategy(strategy: PartiQLParser.SetQuantifierStrategyContext?): PartiqlAst.SetQuantifier? {
        return when {
            strategy == null -> null
            strategy.DISTINCT() != null -> PartiqlAst.build { distinct() }
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
            this.IDENTIFIER_QUOTED() != null -> this.IDENTIFIER_QUOTED().getStringValue()
            this.IDENTIFIER() != null -> this.IDENTIFIER().text
            else -> throw ParserException("Unable to get symbol's text.", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    private fun getSymbolPathExpr(ctx: PartiQLParser.SymbolPrimitiveContext) = PartiqlAst.build {
        when {
            ctx.IDENTIFIER_QUOTED() != null -> pathExpr(lit(ionString(ctx.IDENTIFIER_QUOTED().getStringValue())), caseSensitive())
            ctx.IDENTIFIER() != null -> pathExpr(lit(ionString(ctx.IDENTIFIER().text)), caseInsensitive())
            else -> throw ParserException("Unable to get symbol's text.", ErrorCode.PARSE_INVALID_QUERY)
        }
    }

    private fun String.toInteger() = BigInteger(this, 10)

    private fun Token.toSymbol(): PartiqlAst.Expr.Lit {
        val str = this.text
        val metas = this.getSourceMetaContainer()
        return PartiqlAst.build {
            lit(ionSymbol(str), metas)
        }
    }

    private fun assertIntegerValue(token: Token?, ionValue: IonValue?) {
        if (ionValue == null)
            return
        if (ionValue !is IonInt)
            throw token.err("Expected an integer value.", ErrorCode.PARSE_MALFORMED_PARSE_TREE)
        if (ionValue.integerSize == IntegerSize.LONG || ionValue.integerSize == IntegerSize.BIG_INTEGER)
            throw token.err("Type parameter exceeded maximum value", ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE)
    }

    private fun TerminalNode?.err(msg: String, code: ErrorCode, ctx: PropertyValueMap = PropertyValueMap(), cause: Throwable? = null) = this.error(msg, code, ctx, cause, ion)
    private fun Token?.err(msg: String, code: ErrorCode, ctx: PropertyValueMap = PropertyValueMap(), cause: Throwable? = null) = this.error(msg, code, ctx, cause, ion)
}

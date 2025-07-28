package org.partiql.ast

import org.partiql.ast.Ast.explain
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.identifierSimple
import org.partiql.ast.Ast.query
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.dml.ConflictAction
import org.partiql.ast.dml.ConflictTarget
import org.partiql.ast.dml.Delete
import org.partiql.ast.dml.DoReplaceAction
import org.partiql.ast.dml.DoUpdateAction
import org.partiql.ast.dml.Insert
import org.partiql.ast.dml.InsertSource
import org.partiql.ast.dml.OnConflict
import org.partiql.ast.dml.Replace
import org.partiql.ast.dml.SetClause
import org.partiql.ast.dml.Update
import org.partiql.ast.dml.UpdateTarget
import org.partiql.ast.dml.UpdateTargetStep
import org.partiql.ast.dml.Upsert
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprAnd
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprBetween
import org.partiql.ast.expr.ExprBoolTest
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCase
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprExtract
import org.partiql.ast.expr.ExprInCollection
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprMatch
import org.partiql.ast.expr.ExprMissingPredicate
import org.partiql.ast.expr.ExprNot
import org.partiql.ast.expr.ExprNullIf
import org.partiql.ast.expr.ExprNullPredicate
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprOr
import org.partiql.ast.expr.ExprOverlay
import org.partiql.ast.expr.ExprParameter
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprPosition
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprRowValue
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprSubstring
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.ExprValues
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.ExprVariant
import org.partiql.ast.expr.ExprWindow
import org.partiql.ast.expr.ExprWindowFunction
import org.partiql.ast.expr.PathStep
import org.partiql.ast.graph.GraphMatch
import org.partiql.ast.graph.GraphPattern
import org.partiql.ast.graph.GraphQuantifier
import org.partiql.ast.graph.GraphSelector

/**
 * Abstract base class to rewrite a given AST.
 */
public abstract class AstRewriter<C> : AstVisitor<AstNode, C>() {
    override fun defaultReturn(node: AstNode, context: C): AstNode = node

    private inline fun <reified T> _visitList(
        nodes: List<T>,
        ctx: C,
        method: (node: T, ctx: C) -> AstNode,
    ): List<T> {
        if (nodes.isEmpty()) return nodes
        var diff = false
        val transformed = ArrayList<T>(nodes.size)
        nodes.forEach {
            val n = method(it, ctx) as T
            if (it !== n) diff = true
            transformed.add(n)
        }
        return if (diff) transformed else nodes
    }

    // expr
    override fun visitExprAnd(node: ExprAnd, ctx: C): AstNode {
        val lhs = visitExpr(node.lhs, ctx) as Expr
        val rhs = visitExpr(node.rhs, ctx) as Expr
        return if (lhs !== node.lhs || rhs !== node.rhs) {
            ExprAnd(lhs, rhs)
        } else {
            node
        }
    }

    override fun visitExprArray(node: ExprArray, ctx: C): AstNode {
        val values = _visitList(node.values, ctx, ::visitExpr)
        return if (values !== node.values) {
            ExprArray(values)
        } else {
            node
        }
    }

    override fun visitExprBag(node: ExprBag, ctx: C): AstNode {
        val values = _visitList(node.values, ctx, ::visitExpr)
        return if (values !== node.values) {
            ExprBag(values)
        } else {
            node
        }
    }

    override fun visitExprBetween(node: ExprBetween, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val from = visitExpr(node.from, ctx) as Expr
        val to = visitExpr(node.to, ctx) as Expr
        val not = node.isNot
        return if (value !== node.value || from !== node.from || to !== node.to || not != node.isNot) {
            ExprBetween(value, from, to, not)
        } else {
            node
        }
    }

    override fun visitExprCall(node: ExprCall, ctx: C): AstNode {
        val function = visitIdentifier(node.function, ctx) as Identifier
        val args = _visitList(node.args, ctx, ::visitExpr)
        val setq = node.setq
        return if (function !== node.function || args !== node.args || setq !== node.setq) {
            ExprCall(function, args, setq)
        } else {
            node
        }
    }

    override fun visitExprCase(node: ExprCase, ctx: C): AstNode {
        val expr = node.expr?.let { visitExpr(it, ctx) as Expr? }
        val branches = _visitList(node.branches, ctx, ::visitExprCaseBranch)
        val defaultExpr = node.defaultExpr?.let { visitExpr(it, ctx) as Expr? }
        return if (expr !== node.expr || branches !== node.branches || defaultExpr !== node.defaultExpr) {
            ExprCase(expr, branches, defaultExpr)
        } else {
            node
        }
    }

    override fun visitExprCaseBranch(node: ExprCase.Branch, ctx: C): AstNode {
        val condition = visitExpr(node.condition, ctx) as Expr
        val expr = visitExpr(node.expr, ctx) as Expr
        return if (condition !== node.condition || expr !== node.expr) {
            ExprCase.Branch(condition, expr)
        } else {
            node
        }
    }

    override fun visitExprCast(node: ExprCast, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val type = node.asType
        return if (value !== node.value || type !== node.asType) {
            ExprCast(value, type)
        } else {
            node
        }
    }

    override fun visitExprCoalesce(node: ExprCoalesce, ctx: C): AstNode {
        val args = _visitList(node.args, ctx, ::visitExpr)
        return if (args !== node.args) {
            ExprCoalesce(args)
        } else {
            node
        }
    }

    override fun visitExprExtract(node: ExprExtract, ctx: C): AstNode {
        val field = node.field
        val source = visitExpr(node.source, ctx) as Expr
        return if (field !== node.field || source !== node.source) {
            ExprExtract(field, source)
        } else {
            node
        }
    }

    override fun visitExprInCollection(node: ExprInCollection, ctx: C): AstNode {
        val lhs = visitExpr(node.lhs, ctx) as Expr
        val rhs = visitExpr(node.rhs, ctx) as Expr
        val not = node.isNot
        return if (lhs !== node.lhs || rhs !== node.rhs || not != node.isNot) {
            ExprInCollection(lhs, rhs, not)
        } else {
            node
        }
    }

    override fun visitExprMissingPredicate(node: ExprMissingPredicate, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val not = node.isNot
        return if (value !== node.value || not != node.isNot) {
            ExprMissingPredicate(value, not)
        } else {
            node
        }
    }

    override fun visitExprNullPredicate(node: ExprNullPredicate, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val not = node.isNot
        return if (value !== node.value || not != node.isNot) {
            ExprNullPredicate(value, not)
        } else {
            node
        }
    }

    override fun visitExprBoolTest(node: ExprBoolTest, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val not = node.isNot
        val truthValue = node.truthValue
        return if (value !== node.value || not != node.isNot || truthValue != node.truthValue) {
            ExprBoolTest(value, not, truthValue)
        } else {
            node
        }
    }

    override fun visitExprIsType(node: ExprIsType, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val type = node.type
        val not = node.isNot
        return if (value !== node.value || type !== node.type || not != node.isNot) {
            ExprIsType(value, type, not)
        } else {
            node
        }
    }

    override fun visitExprLike(node: ExprLike, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val pattern = visitExpr(node.pattern, ctx) as Expr
        val escape = node.escape?.let { visitExpr(it, ctx) as Expr? }
        val not = node.isNot
        return if (value !== node.value || pattern !== node.pattern || escape !== node.escape || not != node.isNot) {
            ExprLike(value, pattern, escape, not)
        } else {
            node
        }
    }

    override fun visitExprLit(node: ExprLit, ctx: C): AstNode {
        return node
    }

    override fun visitExprMatch(node: ExprMatch, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val pattern = visitGraphMatch(node.pattern, ctx) as GraphMatch
        return if (expr !== node.expr || pattern !== node.pattern) {
            ExprMatch(expr, pattern)
        } else {
            node
        }
    }

    override fun visitExprNot(node: ExprNot, ctx: C): AstNode {
        val expr = visitExpr(node.value, ctx) as Expr
        return if (expr !== node.value) {
            ExprNot(expr)
        } else {
            node
        }
    }

    override fun visitExprNullIf(node: ExprNullIf, ctx: C): AstNode {
        val v1 = visitExpr(node.v1, ctx) as Expr
        val v2 = visitExpr(node.v2, ctx) as Expr
        return if (v1 !== node.v1 || v2 !== node.v2) {
            ExprNullIf(v1, v2)
        } else {
            node
        }
    }

    override fun visitExprOperator(node: ExprOperator, ctx: C): AstNode {
        val symbol = node.symbol
        val lhs = node.lhs?.let { visitExpr(it, ctx) as Expr? }
        val rhs = visitExpr(node.rhs, ctx) as Expr
        return if (symbol !== node.symbol || lhs !== node.lhs || rhs !== node.rhs) {
            ExprOperator(symbol, lhs, rhs)
        } else {
            node
        }
    }

    override fun visitExprOr(node: ExprOr, ctx: C): AstNode {
        val lhs = visitExpr(node.lhs, ctx) as Expr
        val rhs = visitExpr(node.rhs, ctx) as Expr
        return if (lhs !== node.lhs || rhs !== node.rhs) {
            ExprOr(lhs, rhs)
        } else {
            node
        }
    }

    override fun visitExprOverlay(node: ExprOverlay, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val overlay = visitExpr(node.placing, ctx) as Expr
        val from = visitExpr(node.from, ctx) as Expr
        val forLength = node.forLength?.let { visitExpr(it, ctx) as Expr? }
        return if (value !== node.value || overlay !== node.placing || from !== node.from || forLength !== node.forLength) {
            ExprOverlay(value, overlay, from, forLength)
        } else {
            node
        }
    }

    override fun visitExprParameter(node: ExprParameter, ctx: C): AstNode {
        val index = node.index
        return node
    }

    override fun visitExprPath(node: ExprPath, ctx: C): AstNode {
        val root = visitExpr(node.root, ctx) as Expr
        val pathSteps = _visitList(node.steps, ctx, ::visitPathStep)
        return if (root !== node.root || pathSteps !== node.steps) {
            ExprPath(root, pathSteps)
        } else {
            node
        }
    }

    override fun visitExprPosition(node: ExprPosition, ctx: C): AstNode {
        val lhs = visitExpr(node.lhs, ctx) as Expr
        val rhs = visitExpr(node.rhs, ctx) as Expr
        return if (lhs !== node.lhs || rhs !== node.rhs) {
            ExprPosition(lhs, rhs)
        } else {
            node
        }
    }

    override fun visitExprQuerySet(node: ExprQuerySet, ctx: C): AstNode {
        val body = node.body.let { visitQueryBody(it, ctx) as QueryBody }
        val orderBy = node.orderBy?.let { visitOrderBy(it, ctx) as OrderBy? }
        val limit = node.limit?.let { visitExpr(it, ctx) as Expr? }
        val offset = node.offset?.let { visitExpr(it, ctx) as Expr? }
        val with = node.with?.let { visitWith(it, ctx) as With? }
        return if (body !== node.body || orderBy !== node.orderBy || limit !== node.limit || offset !==
            node.offset || with !== node.with
        ) {
            exprQuerySet(body, orderBy, limit, offset, with)
        } else {
            node
        }
    }

    override fun visitWith(node: With, ctx: C): AstNode {
        val elements = _visitList(node.elements, ctx, ::visitWithListElement)
        return if (elements !== node.elements) {
            With(elements, node.isRecursive)
        } else {
            node
        }
    }

    override fun visitWithListElement(node: WithListElement, ctx: C): AstNode {
        val columns = node.columnList?.let { _visitList(it, ctx, ::visitIdentifierSimple) }
        val queryName = visitIdentifierSimple(node.queryName, ctx) as Identifier.Simple
        val query = visitExprQuerySet(node.asQuery, ctx) as ExprQuerySet
        return if (columns !== node.columnList || queryName !== node.queryName || query !== node.asQuery) {
            WithListElement(queryName, query, columns)
        } else {
            node
        }
    }

    override fun visitExprSessionAttribute(node: ExprSessionAttribute, ctx: C): AstNode {
        val sessionAttribute = node.sessionAttribute
        return node
    }

    override fun visitExprStruct(node: ExprStruct, ctx: C): AstNode {
        val fields = _visitList(node.fields, ctx, ::visitExprStructField)
        return if (fields !== node.fields) {
            ExprStruct(fields)
        } else {
            node
        }
    }

    override fun visitExprStructField(node: ExprStruct.Field, ctx: C): AstNode {
        val name = visitExpr(node.name, ctx) as Expr
        val value = visitExpr(node.value, ctx) as Expr
        return if (name !== node.name || value !== node.value) {
            ExprStruct.Field(name, value)
        } else {
            node
        }
    }

    override fun visitExprSubstring(node: ExprSubstring, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val start = node.start?.let { visitExpr(it, ctx) as Expr? }
        val length = node.length?.let { visitExpr(it, ctx) as Expr? }
        return if (value !== node.value || start !== node.start || length !== node.length) {
            ExprSubstring(value, start, length)
        } else {
            node
        }
    }

    override fun visitExprTrim(node: ExprTrim, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val chars = node.chars?.let { visitExpr(it, ctx) as Expr? }
        val trimSpec = node.trimSpec
        return if (value !== node.value || chars !== node.chars || trimSpec !== node.trimSpec) {
            ExprTrim(value, chars, trimSpec)
        } else {
            node
        }
    }

    override fun visitExprValues(node: ExprValues, ctx: C): AstNode {
        val values = _visitList(node.rows, ctx, ::visitExpr)
        return if (values !== node.rows) {
            ExprValues(values)
        } else {
            node
        }
    }

    override fun visitExprRowValue(node: ExprRowValue, ctx: C): AstNode {
        val values = _visitList(node.values, ctx, ::visitExpr)
        return if (values !== node.values) {
            ExprRowValue(values)
        } else {
            node
        }
    }

    override fun visitExprVariant(node: ExprVariant, ctx: C): AstNode {
        val value = node.value
        val encoding = node.encoding
        return node
    }

    override fun visitExprVarRef(node: ExprVarRef, ctx: C): AstNode {
        val identifier = visitIdentifier(node.identifier, ctx) as Identifier
        val isQualified = node.isQualified
        return if (identifier !== node.identifier || isQualified !== node.isQualified) {
            ExprVarRef(identifier, isQualified)
        } else {
            node
        }
    }

    override fun visitExprWindow(node: ExprWindow, ctx: C): AstNode {
        val windowFunction = node.windowFunction
        val expression = visitExpr(node.expression, ctx) as Expr
        val offset = node.offset?.let { visitExpr(it, ctx) as Expr? }
        val defaultValue = node.defaultValue?.let { visitExpr(it, ctx) as Expr? }
        val over = visitExprWindowOver(node.over, ctx) as ExprWindow.Over
        return if (windowFunction !== node.windowFunction || expression !== node.expression || offset !==
            node.offset || defaultValue !== node.defaultValue || over !== node.over
        ) {
            ExprWindow(windowFunction, expression, offset, defaultValue, over)
        } else {
            node
        }
    }

    override fun visitExprWindowOver(node: ExprWindow.Over, ctx: C): AstNode {
        val partitions = _visitList(node.partitions, ctx, ::visitExpr)
        val sorts = node.sorts.let { _visitList(it, ctx, ::visitSort) }
        return if (partitions !== node.partitions || sorts !== node.sorts) {
            ExprWindow.Over(partitions, sorts)
        } else {
            node
        }
    }

    override fun visitLiteral(node: Literal, ctx: C): AstNode {
        return node
    }

    override fun visitPathStepField(node: PathStep.Field, ctx: C): AstNode {
        val field = visitIdentifierSimple(node.field, ctx) as Identifier.Simple
        return if (field !== node.field) {
            PathStep.Field(field)
        } else {
            node
        }
    }

    override fun visitPathStepElement(node: PathStep.Element, ctx: C): AstNode {
        val element = node.element
        return node
    }

    override fun visitPathStepAllFields(node: PathStep.AllFields, ctx: C): AstNode {
        return node
    }

    override fun visitPathStepAllElements(node: PathStep.AllElements, ctx: C): AstNode {
        return node
    }

    // graph
    override fun visitGraphMatch(node: GraphMatch, ctx: C): AstNode {
        val patterns = _visitList(node.patterns, ctx, ::visitGraphPattern)
        val selector = node.selector?.let { visitGraphSelector(it, ctx) as GraphSelector? }
        return if (patterns !== node.patterns || selector !== node.selector) {
            GraphMatch(patterns, selector)
        } else {
            node
        }
    }

    override fun visitGraphPattern(node: GraphPattern, ctx: C): AstNode {
        val restrictor = node.restrictor
        val prefilter = node.prefilter?.let { visitExpr(it, ctx) as Expr? }
        val variable = node.variable
        val quantifier = node.quantifier?.let { visitGraphQuantifier(it, ctx) as GraphQuantifier? }
        val parts = _visitList(node.parts, ctx, ::visitGraphPart)
        return if (restrictor !== node.restrictor || prefilter !== node.prefilter || variable !==
            node.variable || quantifier !== node.quantifier || parts !== node.parts
        ) {
            GraphPattern(restrictor, prefilter, variable, quantifier, parts)
        } else {
            node
        }
    }

    override fun visitGraphQuantifier(node: GraphQuantifier, ctx: C): AstNode {
        val lower = node.lower
        val upper = node.upper
        return node
    }

    override fun visitGraphSelectorAny(node: GraphSelector.Any, ctx: C): AstNode {
        return node
    }

    override fun visitGraphSelectorAnyK(node: GraphSelector.AnyK, ctx: C): AstNode {
        val k = node.k
        return node
    }

    override fun visitGraphSelectorAllShortest(node: GraphSelector.AllShortest, ctx: C): AstNode {
        return node
    }

    override fun visitGraphSelectorAnyShortest(node: GraphSelector.AnyShortest, ctx: C): AstNode {
        return node
    }

    override fun visitGraphSelectorShortestK(node: GraphSelector.ShortestK, ctx: C): AstNode {
        val k = node.k
        return node
    }

    override fun visitGraphSelectorShortestKGroup(node: GraphSelector.ShortestKGroup, ctx: C): AstNode {
        val k = node.k
        return node
    }

    override fun visitExclude(node: Exclude, ctx: C): AstNode {
        val excludePaths = _visitList(node.excludePaths, ctx, ::visitExcludePath)
        return if (excludePaths !== node.excludePaths) {
            Exclude(excludePaths)
        } else {
            node
        }
    }

    override fun visitExcludePath(node: ExcludePath, ctx: C): AstNode {
        val root = visitExprVarRef(node.root, ctx) as ExprVarRef
        val excludeSteps = _visitList(node.excludeSteps, ctx, ::visitExcludeStep)
        return if (root !== node.root || excludeSteps !== node.excludeSteps) {
            ExcludePath(root, excludeSteps)
        } else {
            node
        }
    }

    override fun visitExcludeStepCollIndex(node: ExcludeStep.CollIndex, ctx: C): AstNode {
        val index = node.index
        return node
    }

    override fun visitExcludeStepStructField(node: ExcludeStep.StructField, ctx: C): AstNode {
        val symbol = visitIdentifierSimple(node.symbol, ctx) as Identifier.Simple
        return if (symbol !== node.symbol) {
            ExcludeStep.StructField(symbol)
        } else {
            node
        }
    }

    override fun visitExcludeStepCollWildcard(node: ExcludeStep.CollWildcard, ctx: C): AstNode {
        return node
    }

    override fun visitExcludeStepStructWildcard(node: ExcludeStep.StructWildcard, ctx: C): AstNode {
        return node
    }

    override fun visitExplain(node: Explain, ctx: C): AstNode {
        val statement = visitStatement(node.statement, ctx) as Statement
        return if (statement !== node.statement) {
            explain(node.options, statement)
        } else {
            node
        }
    }

    override fun visitFrom(node: From, ctx: C): AstNode {
        val tableRefs = _visitList(node.tableRefs, ctx, ::visitFromTableRef)
        return if (tableRefs !== node.tableRefs) {
            From(tableRefs)
        } else {
            node
        }
    }

    override fun visitFromExpr(node: FromExpr, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val fromType = node.fromType
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple? }
        val atAlias = node.atAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple? }
        return if (expr !== node.expr || fromType !== node.fromType || asAlias !== node.asAlias ||
            atAlias !== node.atAlias
        ) {
            FromExpr(expr, fromType, asAlias, atAlias)
        } else {
            node
        }
    }

    override fun visitFromJoin(node: FromJoin, ctx: C): AstNode {
        val lhs = visitFromTableRef(node.lhs, ctx) as FromTableRef
        val rhs = visitFromTableRef(node.rhs, ctx) as FromTableRef
        val joinType = node.joinType
        val condition = node.condition?.let { visitExpr(it, ctx) as Expr? }
        return if (lhs !== node.lhs || rhs !== node.rhs || joinType !== node.joinType ||
            condition !== node.condition
        ) {
            FromJoin(lhs, rhs, joinType, condition)
        } else {
            node
        }
    }

    override fun visitGroupBy(node: GroupBy, ctx: C): AstNode {
        val strategy = node.strategy
        val keys = _visitList(node.keys, ctx, ::visitGroupByKey)
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple? }
        return if (strategy !== node.strategy || keys !== node.keys || asAlias !== node.asAlias) {
            GroupBy(strategy, keys, asAlias)
        } else {
            node
        }
    }

    override fun visitGroupByKey(node: GroupBy.Key, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple? }
        return if (expr !== node.expr || asAlias !== node.asAlias) {
            GroupBy.Key(expr, asAlias)
        } else {
            node
        }
    }

    override fun visitIdentifierSimple(node: Identifier.Simple, ctx: C): AstNode {
        val symbol = node.text
        val isDelimited = node.isRegular
        return identifierSimple(symbol, isDelimited)
    }

    override fun visitIdentifier(node: Identifier, ctx: C): AstNode {
        val qualifier = _visitList(node.qualifier, ctx, ::visitIdentifierSimple)
        val identifier = visitIdentifierSimple(node.identifier, ctx) as Identifier.Simple
        return if (qualifier !== node.qualifier || identifier !== node.identifier) {
            Identifier(qualifier, identifier)
        } else {
            node
        }
    }

    override fun visitLet(node: Let, ctx: C): AstNode {
        val bindings = _visitList(node.bindings, ctx, ::visitLetBinding)
        return if (bindings !== node.bindings) {
            Let(bindings)
        } else {
            node
        }
    }

    override fun visitLetBinding(node: Let.Binding, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val asAlias = visitIdentifierSimple(node.asAlias, ctx) as Identifier.Simple
        return if (expr !== node.expr || asAlias !== node.asAlias) {
            Let.Binding(expr, asAlias)
        } else {
            node
        }
    }

    override fun visitQuery(node: Query, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        return if (expr !== node.expr) {
            query(expr)
        } else {
            node
        }
    }

    override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: C): AstNode {
        val select = visitSelect(node.select, ctx) as Select
        val exclude = node.exclude?.let { visitExclude(it, ctx) as Exclude? }
        val from = visitFrom(node.from, ctx) as From
        val let = node.let?.let { visitLet(it, ctx) as Let? }
        val where = node.where?.let { visitExpr(it, ctx) as Expr? }
        val groupBy = node.groupBy?.let { visitGroupBy(it, ctx) as GroupBy? }
        val having = node.having?.let { visitExpr(it, ctx) as Expr? }
        val window = node.window?.let { visitWindowClause(it, ctx) as WindowClause }
        return if (select !== node.select || exclude !== node.exclude || from !== node.from || let !==
            node.let || where !== node.where || groupBy !== node.groupBy || having !== node.having || window !== node.window
        ) {
            QueryBody.SFW(select, exclude, from, let, where, groupBy, having, window)
        } else {
            node
        }
    }

    public override fun visitQueryBodySetOp(node: QueryBody.SetOp, ctx: C): AstNode {
        val type = visitSetOp(node.type, ctx) as SetOp
        val isOuter = node.isOuter
        val lhs = visitExpr(node.lhs, ctx) as Expr
        val rhs = visitExpr(node.rhs, ctx) as Expr
        return if (type !== node.type || isOuter != node.isOuter || lhs !== node.lhs || rhs !== node.rhs) {
            QueryBody.SetOp(type, isOuter, lhs, rhs)
        } else {
            node
        }
    }

    public override fun visitSelectItemStar(node: SelectItem.Star, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        return if (expr !== node.expr) {
            SelectItem.Star(expr)
        } else {
            node
        }
    }

    public override fun visitSelectItemExpr(node: SelectItem.Expr, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple? }
        return if (expr !== node.expr || asAlias !== node.asAlias) {
            SelectItem.Expr(expr, asAlias)
        } else {
            node
        }
    }

    override fun visitSelectList(node: SelectList, ctx: C): AstNode {
        val items = _visitList(node.items, ctx, ::visitSelectItem)
        val setq = node.setq
        return if (items !== node.items || setq !== node.setq) {
            SelectList(items, setq)
        } else {
            node
        }
    }

    override fun visitWindowClause(node: WindowClause, ctx: C): AstNode {
        val definitions = _visitList(node.definitions, ctx, ::visitWindowDefinition)
        return if (definitions !== node.definitions) {
            WindowClause(definitions)
        } else {
            node
        }
    }

    override fun visitWindowDefinition(node: WindowClause.WindowDefinition, ctx: C): AstNode {
        val name = visitIdentifierSimple(node.name, ctx) as Identifier.Simple
        val spec = visitWindowSpecification(node.specification, ctx) as WindowSpecification
        return if (name !== node.name || spec !== node.specification) {
            WindowClause.WindowDefinition(name, spec)
        } else {
            node
        }
    }

    override fun visitWindowSpecification(node: WindowSpecification, ctx: C): AstNode {
        val partitionClause = node.partitionClause?.let { visitWindowPartitionClause(it, ctx) as WindowPartitionClause }
        val orderClause = node.orderClause?.let { visitWindowOrderClause(it, ctx) as WindowOrderClause }
        val name = node.existingName?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple }
        return if (partitionClause !== node.partitionClause || orderClause !== node.orderClause || name !== node.existingName) {
            WindowSpecification(name, partitionClause, orderClause)
        } else {
            node
        }
    }

    override fun visitWindowPartitionClause(node: WindowPartitionClause, ctx: C): AstNode {
        val partitions = _visitList(node.partitions, ctx, ::visitWindowPartition)
        return if (partitions !== node.partitions) {
            WindowPartitionClause(partitions)
        } else {
            node
        }
    }

    override fun visitWindowOrderClause(node: WindowOrderClause, ctx: C): AstNode {
        val sorts = _visitList(node.sorts, ctx, ::visitSort)
        return if (sorts !== node.sorts) {
            WindowOrderClause(sorts)
        } else {
            node
        }
    }

    override fun visitExprWindowFunction(node: ExprWindowFunction, ctx: C): AstNode {
        val functionType = visitWindowFunctionType(node.functionType, ctx) as WindowFunctionType
        val windowReference = visitWindowReference(node.windowReference, ctx) as WindowReference
        return if (functionType !== node.functionType || windowReference !== node.windowReference) {
            ExprWindowFunction(functionType, windowReference)
        } else {
            node
        }
    }

    override fun visitWindowFunctionTypeLagOrLead(node: WindowFunctionType.LeadOrLag, ctx: C): AstNode {
        val extent = visitExpr(node.extent, ctx) as Expr
        val default = node.defaultValue?.let { visitExpr(it, ctx) as Expr }
        return if (extent !== node.extent || default !== node.defaultValue) {
            WindowFunctionType.LeadOrLag(node.isLead, extent, node.offset, default, node.nullTreatment)
        } else {
            node
        }
    }

    override fun visitWindowPartitionName(node: WindowPartition.Name, ctx: C): AstNode {
        val identifier = visitIdentifier(node.columnReference, ctx) as Identifier
        return if (identifier !== node.columnReference) {
            WindowPartition.Name(identifier)
        } else {
            node
        }
    }

    override fun visitWindowReferenceInLineSpecification(node: WindowReference.InLineSpecification, ctx: C): AstNode {
        val specification = visitWindowSpecification(node.specification, ctx) as WindowSpecification
        return if (specification !== node.specification) {
            WindowReference.InLineSpecification(specification)
        } else {
            node
        }
    }

    override fun visitWindowReferenceName(node: WindowReference.Name, ctx: C): AstNode {
        val name = visitIdentifierSimple(node.name, ctx) as Identifier.Simple
        return if (name !== node.name) {
            WindowReference.Name(name)
        } else {
            node
        }
    }

    override fun visitSelectPivot(node: SelectPivot, ctx: C): AstNode {
        val key = visitExpr(node.key, ctx) as Expr
        val value = visitExpr(node.value, ctx) as Expr
        return if (key !== node.key || value !== node.value) {
            SelectPivot(key, value)
        } else {
            node
        }
    }

    override fun visitSelectStar(node: SelectStar, ctx: C): AstNode {
        val setq = node.setq
        return node
    }

    override fun visitSelectValue(node: SelectValue, ctx: C): AstNode {
        val constructor = visitExpr(node.constructor, ctx) as Expr
        val setq = node.setq
        return if (constructor !== node.constructor || setq !== node.setq) {
            SelectValue(constructor, setq)
        } else {
            node
        }
    }

    override fun visitSetOp(node: SetOp, ctx: C): AstNode {
        val setOpType = node.setOpType
        val setq = node.setq
        return node
    }

    // TODO: DDL
    override fun visitCreateTable(node: CreateTable?, ctx: C): AstNode {
        throw UnsupportedOperationException("CREATE TABLE has not been supported yet in AstRewriter")
    }

    override fun visitInsert(node: Insert, ctx: C): AstNode {
        val source = visitInsertSource(node.source, ctx) as InsertSource
        val target = visitIdentifier(node.tableName, ctx) as Identifier
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple }
        val onConflict = node.onConflict?.let { visitOnConflict(it, ctx) as OnConflict }
        if (source !== node.source || target !== node.tableName || asAlias !== node.asAlias || onConflict !== node.onConflict) {
            return Insert(target, asAlias, source, onConflict)
        }
        return node
    }

    override fun visitInsertSourceFromExpr(node: InsertSource.FromExpr, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val columns = node.columns?.let { _visitList(it, ctx, ::visitIdentifierSimple) }
        if (expr !== node.expr || columns != node.columns) {
            return InsertSource.FromExpr(columns, expr)
        }
        return node
    }

    override fun visitInsertSourceFromDefault(node: InsertSource.FromDefault, ctx: C): AstNode {
        return node
    }

    override fun visitOnConflict(node: OnConflict, ctx: C): AstNode {
        val action = visitConflictAction(node.action, ctx) as ConflictAction
        val target = visitConflictTarget(node.target, ctx) as ConflictTarget
        if (action !== node.action || target !== node.target) {
            return OnConflict(action, target)
        }
        return node
    }

    override fun visitConflictActionDoNothing(node: ConflictAction.DoNothing, ctx: C): AstNode {
        return node
    }

    override fun visitConflictActionDoReplace(node: ConflictAction.DoReplace, ctx: C): AstNode {
        val action = visitDoReplaceAction(node.action, ctx) as DoReplaceAction
        val condition = node.condition?.let { visitExpr(it, ctx) as Expr }
        if (action !== node.action || condition !== node.condition) {
            return ConflictAction.DoReplace(action, condition)
        }
        return node
    }

    override fun visitConflictActionDoUpdate(node: ConflictAction.DoUpdate, ctx: C): AstNode {
        val action = visitDoUpdateAction(node.action, ctx) as DoUpdateAction
        val condition = node.condition?.let { visitExpr(it, ctx) as Expr }
        if (action !== node.action || condition !== node.condition) {
            return ConflictAction.DoUpdate(action, condition)
        }
        return node
    }

    override fun visitConflictTargetConstraint(node: ConflictTarget.Constraint, ctx: C): AstNode {
        val constraint = visitIdentifier(node.name, ctx) as Identifier
        if (constraint !== node.name) {
            return ConflictTarget.Constraint(constraint)
        }
        return node
    }

    override fun visitConflictTargetIndex(node: ConflictTarget.Index, ctx: C): AstNode {
        val indexes = _visitList(node.indexes, ctx, ::visitIdentifierSimple)
        if (indexes !== node.indexes) {
            return ConflictTarget.Index(indexes)
        }
        return node
    }

    override fun visitDoReplaceActionExcluded(node: DoReplaceAction.Excluded, ctx: C): AstNode {
        return node
    }

    override fun visitDoUpdateActionExcluded(node: DoUpdateAction.Excluded, ctx: C): AstNode {
        return node
    }

    override fun visitDelete(node: Delete, ctx: C): AstNode {
        val tableName = visitIdentifier(node.tableName, ctx) as Identifier
        val condition = node.condition?.let { visitExpr(it, ctx) as Expr }
        if (tableName !== node.tableName || condition !== node.condition) {
            return Delete(tableName, condition)
        }
        return node
    }

    override fun visitUpsert(node: Upsert, ctx: C): AstNode {
        val tableName = visitIdentifier(node.tableName, ctx) as Identifier
        val source = visitInsertSource(node.source, ctx) as InsertSource
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple }
        if (tableName !== node.tableName || source !== node.source || asAlias !== node.asAlias) {
            return Upsert(tableName, asAlias, source)
        }
        return node
    }

    override fun visitReplace(node: Replace, ctx: C): AstNode {
        val tableName = visitIdentifier(node.tableName, ctx) as Identifier
        val source = visitInsertSource(node.source, ctx) as InsertSource
        val asAlias = node.asAlias?.let { visitIdentifierSimple(it, ctx) as Identifier.Simple }
        if (tableName !== node.tableName || source !== node.source || asAlias !== node.asAlias) {
            return Replace(tableName, asAlias, source)
        }
        return node
    }

    override fun visitSetClause(node: SetClause, ctx: C): AstNode {
        val target = visitUpdateTarget(node.target, ctx) as UpdateTarget
        val expr = visitExpr(node.expr, ctx) as Expr
        if (target !== node.target || expr !== node.expr) {
            return SetClause(target, expr)
        }
        return node
    }

    override fun visitUpdate(node: Update, ctx: C): AstNode {
        val tableName = visitIdentifier(node.tableName, ctx) as Identifier
        val setClauses = _visitList(node.setClauses, ctx, ::visitSetClause)
        val condition = node.condition?.let { visitExpr(it, ctx) as Expr }
        if (tableName !== node.tableName || setClauses !== node.setClauses || condition !== node.condition) {
            return Update(tableName, setClauses, condition)
        }
        return node
    }

    override fun visitUpdateTarget(node: UpdateTarget, ctx: C): AstNode {
        val root = visitIdentifierSimple(node.root, ctx) as Identifier.Simple
        val steps = _visitList(node.steps, ctx, ::visitUpdateTargetStep)
        if (root !== node.root || steps !== node.steps) {
            return UpdateTarget(root, steps)
        }
        return node
    }

    override fun visitUpdateTargetStepElement(node: UpdateTargetStep.Element, ctx: C): AstNode {
        val exprLit = visitLiteral(node.key, ctx) as Literal
        if (exprLit !== node.key) {
            return UpdateTargetStep.Element(exprLit)
        }
        return node
    }

    override fun visitUpdateTargetStepField(node: UpdateTargetStep.Field, ctx: C): AstNode {
        val key = visitIdentifierSimple(node.key, ctx) as Identifier.Simple
        if (key !== node.key) {
            return UpdateTargetStep.Field(key)
        }
        return node
    }
}

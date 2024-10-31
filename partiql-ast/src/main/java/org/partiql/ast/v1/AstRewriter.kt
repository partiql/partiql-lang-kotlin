package org.partiql.ast.v1

import org.partiql.ast.v1.Ast.explain
import org.partiql.ast.v1.Ast.exprQuerySet
import org.partiql.ast.v1.Ast.identifier
import org.partiql.ast.v1.Ast.query
import org.partiql.ast.v1.expr.Expr
import org.partiql.ast.v1.expr.ExprAnd
import org.partiql.ast.v1.expr.ExprArray
import org.partiql.ast.v1.expr.ExprBag
import org.partiql.ast.v1.expr.ExprBetween
import org.partiql.ast.v1.expr.ExprCall
import org.partiql.ast.v1.expr.ExprCase
import org.partiql.ast.v1.expr.ExprCast
import org.partiql.ast.v1.expr.ExprCoalesce
import org.partiql.ast.v1.expr.ExprExtract
import org.partiql.ast.v1.expr.ExprInCollection
import org.partiql.ast.v1.expr.ExprIsType
import org.partiql.ast.v1.expr.ExprLike
import org.partiql.ast.v1.expr.ExprLit
import org.partiql.ast.v1.expr.ExprMatch
import org.partiql.ast.v1.expr.ExprNot
import org.partiql.ast.v1.expr.ExprNullIf
import org.partiql.ast.v1.expr.ExprOperator
import org.partiql.ast.v1.expr.ExprOr
import org.partiql.ast.v1.expr.ExprOverlay
import org.partiql.ast.v1.expr.ExprParameter
import org.partiql.ast.v1.expr.ExprPath
import org.partiql.ast.v1.expr.ExprPosition
import org.partiql.ast.v1.expr.ExprQuerySet
import org.partiql.ast.v1.expr.ExprSessionAttribute
import org.partiql.ast.v1.expr.ExprStruct
import org.partiql.ast.v1.expr.ExprSubstring
import org.partiql.ast.v1.expr.ExprTrim
import org.partiql.ast.v1.expr.ExprValues
import org.partiql.ast.v1.expr.ExprVarRef
import org.partiql.ast.v1.expr.ExprVariant
import org.partiql.ast.v1.expr.ExprWindow
import org.partiql.ast.v1.expr.PathStep
import org.partiql.ast.v1.graph.GraphMatch
import org.partiql.ast.v1.graph.GraphPattern
import org.partiql.ast.v1.graph.GraphQuantifier
import org.partiql.ast.v1.graph.GraphSelector
import org.partiql.value.PartiQLValueExperimental

// TODO docs and move to Kotlin sources
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
        val not = node.not
        return if (value !== node.value || from !== node.from || to !== node.to || not != node.not) {
            ExprBetween(value, from, to, not)
        } else {
            node
        }
    }

    override fun visitExprCall(node: ExprCall, ctx: C): AstNode {
        val function = visitIdentifierChain(node.function, ctx) as IdentifierChain
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
        val not = node.not
        return if (lhs !== node.lhs || rhs !== node.rhs || not != node.not) {
            ExprInCollection(lhs, rhs, not)
        } else {
            node
        }
    }

    override fun visitExprIsType(node: ExprIsType, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val type = node.type
        val not = node.not
        return if (value !== node.value || type !== node.type || not != node.not) {
            ExprIsType(value, type, not)
        } else {
            node
        }
    }

    override fun visitExprLike(node: ExprLike, ctx: C): AstNode {
        val value = visitExpr(node.value, ctx) as Expr
        val pattern = visitExpr(node.pattern, ctx) as Expr
        val escape = node.escape?.let { visitExpr(it, ctx) as Expr? }
        val not = node.not
        return if (value !== node.value || pattern !== node.pattern || escape !== node.escape || not != node.not) {
            ExprLike(value, pattern, escape, not)
        } else {
            node
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprLit(node: ExprLit, ctx: C): AstNode {
        val value = node.value
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
        val next = node.next?.let { visitPathStep(it, ctx) as PathStep? }
        return if (root !== node.root || next !== node.next) {
            ExprPath(root, next)
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
        return if (body !== node.body || orderBy !== node.orderBy || limit !== node.limit || offset !==
            node.offset
        ) {
            exprQuerySet(body, orderBy, limit, offset)
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
        val values = _visitList(node.rows, ctx, ::visitExprValuesRow)
        return if (values !== node.rows) {
            ExprValues(values)
        } else {
            node
        }
    }

    override fun visitExprValuesRow(node: ExprValues.Row, ctx: C): AstNode {
        val values = _visitList(node.values, ctx, ::visitExpr)
        return if (values !== node.values) {
            ExprValues.Row(values)
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
        val identifierChain = visitIdentifierChain(node.identifierChain, ctx) as IdentifierChain
        val scope = node.scope
        return if (identifierChain !== node.identifierChain || scope !== node.scope) {
            ExprVarRef(identifierChain, scope)
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
        val partitions = node.partitions?.let { _visitList(it, ctx, ::visitExpr) }
        val sorts = node.sorts?.let { _visitList(it, ctx, ::visitSort) }
        return if (partitions !== node.partitions || sorts !== node.sorts) {
            ExprWindow.Over(partitions, sorts)
        } else {
            node
        }
    }

    override fun visitPathStepField(node: PathStep.Field, ctx: C): AstNode {
        val field = visitIdentifier(node.field, ctx) as Identifier
        val next = node.next?.let { visitPathStep(it, ctx) as PathStep? }
        return if (field !== node.field || next !== node.next) {
            PathStep.Field(field, next)
        } else {
            node
        }
    }

    override fun visitPathStepElement(node: PathStep.Element, ctx: C): AstNode {
        val element = visitExpr(node.element, ctx) as Expr
        val next = node.next?.let { visitPathStep(it, ctx) as PathStep? }
        return if (element !== node.element || next !== node.next) {
            PathStep.Element(element, next)
        } else {
            node
        }
    }

    override fun visitPathStepAllFields(node: PathStep.AllFields, ctx: C): AstNode {
        val next = node.next?.let { visitPathStep(it, ctx) as PathStep? }
        return if (next !== node.next) {
            PathStep.AllFields(next)
        } else {
            node
        }
    }

    override fun visitPathStepAllElements(node: PathStep.AllElements, ctx: C): AstNode {
        val next = node.next?.let { visitPathStep(it, ctx) as PathStep? }
        return if (next !== node.next) {
            PathStep.AllElements(next)
        } else {
            node
        }
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

    // TODO rename the visitor
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
        val symbol = visitIdentifier(node.symbol, ctx) as Identifier
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

    @OptIn(PartiQLValueExperimental::class)
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
        val asAlias = node.asAlias?.let { visitIdentifier(it, ctx) as Identifier? }
        val atAlias = node.atAlias?.let { visitIdentifier(it, ctx) as Identifier? }
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
        val asAlias = node.asAlias?.let { visitIdentifier(it, ctx) as Identifier? }
        return if (strategy !== node.strategy || keys !== node.keys || asAlias !== node.asAlias) {
            GroupBy(strategy, keys, asAlias)
        } else {
            node
        }
    }

    override fun visitGroupByKey(node: GroupBy.Key, ctx: C): AstNode {
        val expr = visitExpr(node.expr, ctx) as Expr
        val asAlias = node.asAlias?.let { visitIdentifier(it, ctx) as Identifier? }
        return if (expr !== node.expr || asAlias !== node.asAlias) {
            GroupBy.Key(expr, asAlias)
        } else {
            node
        }
    }

    override fun visitIdentifier(node: Identifier, ctx: C): AstNode {
        val symbol = node.symbol
        val isDelimited = node.isDelimited
        return identifier(symbol, isDelimited)
    }

    override fun visitIdentifierChain(node: IdentifierChain, ctx: C): AstNode {
        val root = visitIdentifier(node.root, ctx) as Identifier
        val next = node.next?.let { visitIdentifierChain(it, ctx) as IdentifierChain? }
        return if (root !== node.root || next !== node.next) {
            IdentifierChain(root, next)
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
        val asAlias = visitIdentifier(node.asAlias, ctx) as Identifier
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
        return if (select !== node.select || exclude !== node.exclude || from !== node.from || let !==
            node.let || where !== node.where || groupBy !== node.groupBy || having !== node.having
        ) {
            QueryBody.SFW(select, exclude, from, let, where, groupBy, having)
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
        val asAlias = node.asAlias?.let { visitIdentifier(it, ctx) as Identifier? }
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
}

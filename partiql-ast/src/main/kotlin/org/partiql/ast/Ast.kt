package org.partiql.ast

import org.partiql.ast.ddl.AttributeConstraint
import org.partiql.ast.ddl.ColumnDefinition
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.ddl.KeyValue
import org.partiql.ast.ddl.PartitionBy
import org.partiql.ast.ddl.TableConstraint
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprAnd
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprBetween
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
import org.partiql.ast.expr.ExprNot
import org.partiql.ast.expr.ExprNullIf
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
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.PathStep.AllFields
import org.partiql.ast.expr.Scope
import org.partiql.ast.expr.SessionAttribute
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.expr.WindowFunction
import org.partiql.ast.graph.GraphDirection
import org.partiql.ast.graph.GraphLabel
import org.partiql.ast.graph.GraphMatch
import org.partiql.ast.graph.GraphPart
import org.partiql.ast.graph.GraphPattern
import org.partiql.ast.graph.GraphQuantifier
import org.partiql.ast.graph.GraphRestrictor
import org.partiql.ast.graph.GraphSelector
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

// TODO docs for all factory methods
//  Also consider defaults for nullable. Need to look more into backwards compatibility.
//  Tracking issue for defaults -- https://github.com/partiql/partiql-lang-kotlin/issues/1640.
public object Ast {
    // Expr
    @JvmStatic
    public fun exprAnd(lhs: Expr, rhs: Expr): ExprAnd {
        return ExprAnd(lhs, rhs)
    }

    @JvmStatic
    public fun exprArray(values: List<Expr>): ExprArray {
        return ExprArray(values)
    }

    @JvmStatic
    public fun exprBag(values: List<Expr>): ExprBag {
        return ExprBag(values)
    }

    @JvmStatic
    public fun exprBetween(value: Expr, from: Expr, to: Expr, not: Boolean): ExprBetween {
        return ExprBetween(value, from, to, not)
    }

    @JvmStatic
    public fun exprCall(function: IdentifierChain, args: List<Expr>, setq: SetQuantifier?): ExprCall {
        return ExprCall(function, args, setq)
    }

    @JvmStatic
    public fun exprCase(expr: Expr?, branches: List<ExprCase.Branch>, defaultExpr: Expr?): ExprCase {
        return ExprCase(expr, branches, defaultExpr)
    }

    @JvmStatic
    public fun exprCaseBranch(condition: Expr, expr: Expr): ExprCase.Branch {
        return ExprCase.Branch(condition, expr)
    }

    @JvmStatic
    public fun exprCast(value: Expr, asType: DataType): ExprCast {
        return ExprCast(value, asType)
    }

    @JvmStatic
    public fun exprCoalesce(args: List<Expr>): ExprCoalesce {
        return ExprCoalesce(args)
    }

    @JvmStatic
    public fun exprExtract(field: DatetimeField, source: Expr): ExprExtract {
        return ExprExtract(field, source)
    }

    @JvmStatic
    public fun exprInCollection(lhs: Expr, rhs: Expr, not: Boolean): ExprInCollection {
        return ExprInCollection(lhs, rhs, not)
    }

    @JvmStatic
    public fun exprIsType(value: Expr, type: DataType, not: Boolean): ExprIsType {
        return ExprIsType(value, type, not)
    }

    @JvmStatic
    public fun exprLike(value: Expr, pattern: Expr, escape: Expr?, not: Boolean): ExprLike {
        return ExprLike(value, pattern, escape, not)
    }

    // This representation will be changed in https://github.com/partiql/partiql-lang-kotlin/issues/1589
    @OptIn(PartiQLValueExperimental::class)
    @JvmStatic
    public fun exprLit(value: PartiQLValue): ExprLit {
        return ExprLit(value)
    }

    @JvmStatic
    public fun exprMatch(expr: Expr, pattern: GraphMatch): ExprMatch {
        return ExprMatch(expr, pattern)
    }

    @JvmStatic
    public fun exprNot(value: Expr): ExprNot {
        return ExprNot(value)
    }

    @JvmStatic
    public fun exprNullIf(v1: Expr, v2: Expr): ExprNullIf {
        return ExprNullIf(v1, v2)
    }

    @JvmStatic
    public fun exprOperator(symbol: String, lhs: Expr?, rhs: Expr): ExprOperator {
        return ExprOperator(symbol, lhs, rhs)
    }

    @JvmStatic
    public fun exprOr(lhs: Expr, rhs: Expr): ExprOr {
        return ExprOr(lhs, rhs)
    }

    @JvmStatic
    public fun exprOverlay(value: Expr, placing: Expr, from: Expr, forLength: Expr?): ExprOverlay {
        return ExprOverlay(value, placing, from, forLength)
    }

    @JvmStatic
    public fun exprParameter(index: Int): ExprParameter {
        return ExprParameter(index)
    }

    @JvmStatic
    public fun exprPath(root: Expr, next: PathStep?): ExprPath {
        return ExprPath(root, next)
    }

    @JvmStatic
    public fun exprPosition(lhs: Expr, rhs: Expr): ExprPosition {
        return ExprPosition(lhs, rhs)
    }

    @JvmStatic
    public fun exprQuerySet(body: QueryBody, orderBy: OrderBy?, limit: Expr?, offset: Expr?): ExprQuerySet {
        return ExprQuerySet(body, orderBy, limit, offset)
    }

    @JvmStatic
    public fun exprSessionAttribute(sessionAttribute: SessionAttribute): ExprSessionAttribute {
        return ExprSessionAttribute(sessionAttribute)
    }

    @JvmStatic
    public fun exprStruct(fields: List<ExprStruct.Field>): ExprStruct {
        return ExprStruct(fields)
    }

    @JvmStatic
    public fun exprStructField(name: Expr, value: Expr): ExprStruct.Field {
        return ExprStruct.Field(name, value)
    }

    @JvmStatic
    public fun exprSubstring(value: Expr, start: Expr?, length: Expr?): ExprSubstring {
        return ExprSubstring(value, start, length)
    }

    @JvmStatic
    public fun exprTrim(value: Expr, chars: Expr?, trimSpec: TrimSpec?): ExprTrim {
        return ExprTrim(value, chars, trimSpec)
    }

    @JvmStatic
    public fun exprValues(rows: List<ExprRowValue>): ExprValues {
        return ExprValues(rows)
    }

    @JvmStatic
    public fun exprRowValue(values: List<Expr>): ExprRowValue {
        return ExprRowValue(values)
    }

    @JvmStatic
    public fun exprVariant(value: String, encoding: String): ExprVariant {
        return ExprVariant(value, encoding)
    }

    @JvmStatic
    public fun exprVarRef(identifierChain: IdentifierChain, scope: Scope): ExprVarRef {
        return ExprVarRef(identifierChain, scope)
    }

    @JvmStatic
    public fun exprWindow(
        windowFunction: WindowFunction,
        exression: Expr,
        offset: Expr?,
        defaultValue: Expr?,
        over: ExprWindow.Over
    ): ExprWindow {
        return ExprWindow(windowFunction, exression, offset, defaultValue, over)
    }

    @JvmStatic
    public fun exprWindowOver(partitions: List<Expr>?, sorts: List<Sort>?): ExprWindow.Over {
        return ExprWindow.Over(partitions, sorts)
    }

    @JvmStatic
    public fun exprPathStepField(value: Identifier, next: PathStep?): PathStep.Field {
        return PathStep.Field(value, next)
    }

    @JvmStatic
    public fun exprPathStepElement(element: Expr, next: PathStep?): PathStep.Element {
        return PathStep.Element(element, next)
    }

    @JvmStatic
    public fun exprPathStepAllElements(next: PathStep?): PathStep.AllElements {
        return PathStep.AllElements(next)
    }

    @JvmStatic
    public fun exprPathStepAllFields(next: PathStep?): AllFields {
        return AllFields(next)
    }

    // Graph
    @JvmStatic
    public fun graphLabelName(name: String): GraphLabel.Name {
        return GraphLabel.Name(name)
    }

    @JvmStatic
    public fun graphLabelWildcard(): GraphLabel.Wildcard {
        return GraphLabel.Wildcard()
    }

    @JvmStatic
    public fun graphLabelNegation(arg: GraphLabel): GraphLabel.Negation {
        return GraphLabel.Negation(arg)
    }

    @JvmStatic
    public fun graphLabelConj(lhs: GraphLabel, rhs: GraphLabel): GraphLabel.Conj {
        return GraphLabel.Conj(lhs, rhs)
    }

    @JvmStatic
    public fun graphLabelDisj(lhs: GraphLabel, rhs: GraphLabel): GraphLabel.Disj {
        return GraphLabel.Disj(lhs, rhs)
    }

    @JvmStatic
    public fun graphMatch(patterns: List<GraphPattern>, selector: GraphSelector?): GraphMatch {
        return GraphMatch(patterns, selector)
    }

    @JvmStatic
    public fun graphMatchNode(prefilter: Expr?, variable: String?, label: GraphLabel?): GraphPart.Node {
        return GraphPart.Node(prefilter, variable, label)
    }

    @JvmStatic
    public fun graphMatchEdge(
        direction: GraphDirection,
        quantifier: GraphQuantifier?,
        prefilter: Expr?,
        variable: String?,
        label: GraphLabel?
    ): GraphPart.Edge {
        return GraphPart.Edge(direction, quantifier, prefilter, variable, label)
    }

    @JvmStatic
    public fun graphMatchPattern(pattern: GraphPattern): GraphPart.Pattern {
        return GraphPart.Pattern(pattern)
    }

    @JvmStatic
    public fun graphPattern(
        restrictor: GraphRestrictor?,
        prefilter: Expr?,
        variable: String?,
        quantifier: GraphQuantifier?,
        parts: List<GraphPart>
    ): GraphPattern {
        return GraphPattern(restrictor, prefilter, variable, quantifier, parts)
    }

    @JvmStatic
    public fun graphQuantifier(lower: Long, upper: Long?): GraphQuantifier {
        return GraphQuantifier(lower, upper)
    }

    @JvmStatic
    public fun graphSelectorAnyShortest(): GraphSelector.AnyShortest {
        return GraphSelector.AnyShortest()
    }

    @JvmStatic
    public fun graphSelectorAllShortest(): GraphSelector.AllShortest {
        return GraphSelector.AllShortest()
    }

    @JvmStatic
    public fun graphSelectorAny(): GraphSelector.Any {
        return GraphSelector.Any()
    }

    @JvmStatic
    public fun graphSelectorAnyK(k: Long): GraphSelector.AnyK {
        return GraphSelector.AnyK(k)
    }

    @JvmStatic
    public fun graphSelectorShortestK(k: Long): GraphSelector.ShortestK {
        return GraphSelector.ShortestK(k)
    }

    @JvmStatic
    public fun graphSelectorShortestKGroup(k: Long): GraphSelector.ShortestKGroup {
        return GraphSelector.ShortestKGroup(k)
    }

    // Other
    @JvmStatic
    public fun exclude(excludePaths: List<ExcludePath>): Exclude {
        return Exclude(excludePaths)
    }

    @JvmStatic
    public fun excludePath(varRef: ExprVarRef, excludeSteps: List<ExcludeStep>): ExcludePath {
        return ExcludePath(varRef, excludeSteps)
    }

    @JvmStatic
    public fun excludeStepStructField(symbol: Identifier): ExcludeStep.StructField {
        return ExcludeStep.StructField(symbol)
    }

    @JvmStatic
    public fun excludeStepCollIndex(index: Int): ExcludeStep.CollIndex {
        return ExcludeStep.CollIndex(index)
    }

    @JvmStatic
    public fun excludeStepStructWildcard(): ExcludeStep.StructWildcard {
        return ExcludeStep.StructWildcard()
    }

    @JvmStatic
    public fun excludeStepCollWildcard(): ExcludeStep.CollWildcard {
        return ExcludeStep.CollWildcard()
    }

    // This representation will be changed in https://github.com/partiql/partiql-lang-kotlin/issues/1589
    @OptIn(PartiQLValueExperimental::class)
    @JvmStatic
    public fun explain(options: Map<String, PartiQLValue>, statement: Statement): Explain {
        return Explain(options, statement)
    }

    @JvmStatic
    public fun from(tableRefs: List<FromTableRef>): From {
        return From(tableRefs)
    }

    @JvmStatic
    public fun fromExpr(expr: Expr, fromType: FromType, asAlias: Identifier?, atAlias: Identifier?): FromExpr {
        return FromExpr(expr, fromType, asAlias, atAlias)
    }

    @JvmStatic
    public fun fromJoin(lhs: FromTableRef, rhs: FromTableRef, joinType: JoinType?, condition: Expr?): FromJoin {
        return FromJoin(lhs, rhs, joinType, condition)
    }

    @JvmStatic
    public fun groupBy(strategy: GroupByStrategy, keys: List<GroupBy.Key>, asAlias: Identifier?): GroupBy {
        return GroupBy(strategy, keys, asAlias)
    }

    @JvmStatic
    public fun groupByKey(expr: Expr, asAlias: Identifier?): GroupBy.Key {
        return GroupBy.Key(expr, asAlias)
    }

    @JvmStatic
    public fun identifier(symbol: String, isDelimited: Boolean): Identifier {
        return Identifier(symbol, isDelimited)
    }

    @JvmStatic
    public fun identifierChain(root: Identifier, next: IdentifierChain?): IdentifierChain {
        return IdentifierChain(root, next)
    }

    @JvmStatic
    public fun let(bindings: List<Let.Binding>): Let {
        return Let(bindings)
    }

    @JvmStatic
    public fun letBinding(expr: Expr, asAlias: Identifier): Let.Binding {
        return Let.Binding(expr, asAlias)
    }

    @JvmStatic
    public fun orderBy(sorts: List<Sort>): OrderBy {
        return OrderBy(sorts)
    }

    @JvmStatic
    public fun query(expr: Expr): Query {
        return Query(expr)
    }

    @JvmStatic
    public fun queryBodySFW(
        select: Select,
        exclude: Exclude?,
        from: From,
        let: Let?,
        where: Expr?,
        groupBy: GroupBy?,
        having: Expr?
    ): QueryBody.SFW {
        return QueryBody.SFW(select, exclude, from, let, where, groupBy, having)
    }

    @JvmStatic
    public fun queryBodySetOp(type: SetOp, isOuter: Boolean, lhs: Expr, rhs: Expr): QueryBody.SetOp {
        return QueryBody.SetOp(type, isOuter, lhs, rhs)
    }

    @JvmStatic
    public fun selectItemStar(expr: Expr): SelectItem.Star {
        return SelectItem.Star(expr)
    }

    @JvmStatic
    public fun selectItemExpr(expr: Expr, asAlias: Identifier?): SelectItem.Expr {
        return SelectItem.Expr(expr, asAlias)
    }

    @JvmStatic
    public fun selectList(items: List<SelectItem>, setq: SetQuantifier?): SelectList {
        return SelectList(items, setq)
    }

    @JvmStatic
    public fun selectPivot(key: Expr, value: Expr): SelectPivot {
        return SelectPivot(key, value)
    }

    @JvmStatic
    public fun selectStar(setq: SetQuantifier?): SelectStar {
        return SelectStar(setq)
    }

    @JvmStatic
    public fun selectValue(constructor: Expr, setq: SetQuantifier?): SelectValue {
        return SelectValue(constructor, setq)
    }

    @JvmStatic
    public fun setOp(setOpType: SetOpType, setq: SetQuantifier?): SetOp {
        return SetOp(setOpType, setq)
    }

    @JvmStatic
    public fun sort(expr: Expr, order: Order?, nulls: Nulls?): Sort {
        return Sort(expr, order, nulls)
    }

    //
    // DDL
    //
    @JvmStatic
    public fun createTable(name: IdentifierChain, columns: List<ColumnDefinition>, constraints: List<TableConstraint>, partitionBy: PartitionBy?, tableProperties: List<KeyValue>): CreateTable {
        return CreateTable(
            name,
            columns,
            constraints,
            partitionBy,
            tableProperties
        )
    }

    @JvmStatic
    public fun columnDefinition(name: Identifier, type: DataType, isOptional: Boolean, constraints: List<AttributeConstraint>, comment: String?): ColumnDefinition {
        return ColumnDefinition(name, type, isOptional, constraints, comment)
    }

    @JvmStatic
    public fun tableConstraintUnique(name: String?, columns: List<Identifier>): TableConstraint.Unique {
        return TableConstraint.Unique(name, columns)
    }

    @JvmStatic
    public fun tableConstraintPrimaryKey(name: String?, columns: List<Identifier>): TableConstraint.PrimaryKey {
        return TableConstraint.PrimaryKey(name, columns)
    }

    @JvmStatic
    public fun tableConstraintCheck(name: String?, searchCondition: Expr): TableConstraint.Check {
        return TableConstraint.Check(name, searchCondition)
    }

    @JvmStatic
    public fun columnConstraintNullable(name: String?, isNullable: Boolean): AttributeConstraint.Null {
        return AttributeConstraint.Null(name, isNullable)
    }

    @JvmStatic
    public fun columnConstraintUnique(name: String?, isPrimaryKey: Boolean): AttributeConstraint.Unique {
        return AttributeConstraint.Unique(name, isPrimaryKey)
    }

    @JvmStatic
    public fun columnConstraintCheck(name: String?, searchCondition: Expr): AttributeConstraint.Check {
        return AttributeConstraint.Check(name, searchCondition)
    }

    @OptIn(PartiQLValueExperimental::class)
    @JvmStatic
    public fun keyValue(key: String, value: String): KeyValue {
        return KeyValue(key, value)
    }

    @JvmStatic
    public fun partitionBy(columns: List<Identifier>): PartitionBy {
        return PartitionBy(columns)
    }
}

package org.partiql.ast

import org.partiql.ast.ddl.AttributeConstraint
import org.partiql.ast.ddl.ColumnDefinition
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.ddl.KeyValue
import org.partiql.ast.ddl.PartitionBy
import org.partiql.ast.ddl.TableConstraint
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
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.PathStep.AllFields
import org.partiql.ast.expr.SessionAttribute
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.expr.TruthValue
import org.partiql.ast.expr.WindowFunction
import org.partiql.ast.graph.GraphDirection
import org.partiql.ast.graph.GraphLabel
import org.partiql.ast.graph.GraphMatch
import org.partiql.ast.graph.GraphPart
import org.partiql.ast.graph.GraphPattern
import org.partiql.ast.graph.GraphQuantifier
import org.partiql.ast.graph.GraphRestrictor
import org.partiql.ast.graph.GraphSelector

/**
 * Static factory methods for creating AST nodes.
 */
public object Ast {
    // TODO: add docs for all factory methods
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
    @JvmOverloads
    public fun exprCall(function: Identifier, args: List<Expr>, setq: SetQuantifier? = null): ExprCall {
        return ExprCall(function, args, setq)
    }

    @JvmStatic
    @JvmOverloads
    public fun exprCase(expr: Expr? = null, branches: List<ExprCase.Branch>, defaultExpr: Expr? = null): ExprCase {
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
    public fun exprNullPredicate(value: Expr, not: Boolean): ExprNullPredicate {
        return ExprNullPredicate(value, not)
    }

    @JvmStatic
    public fun exprMissingPredicate(value: Expr, not: Boolean): ExprMissingPredicate {
        return ExprMissingPredicate(value, not)
    }

    @JvmStatic
    public fun exprBoolTest(value: Expr, not: Boolean, truthValue: TruthValue): ExprBoolTest {
        return ExprBoolTest(value, not, truthValue)
    }

    @JvmStatic
    public fun exprIsType(value: Expr, type: DataType, not: Boolean): ExprIsType {
        return ExprIsType(value, type, not)
    }

    @JvmStatic
    @JvmOverloads
    public fun exprLike(value: Expr, pattern: Expr, escape: Expr? = null, not: Boolean): ExprLike {
        return ExprLike(value, pattern, escape, not)
    }

    @JvmStatic
    public fun exprLit(value: Literal): ExprLit {
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
    @JvmOverloads
    public fun exprOperator(symbol: String, lhs: Expr? = null, rhs: Expr): ExprOperator {
        return ExprOperator(symbol, lhs, rhs)
    }

    @JvmStatic
    public fun exprOr(lhs: Expr, rhs: Expr): ExprOr {
        return ExprOr(lhs, rhs)
    }

    @JvmStatic
    @JvmOverloads
    public fun exprOverlay(value: Expr, placing: Expr, from: Expr, forLength: Expr? = null): ExprOverlay {
        return ExprOverlay(value, placing, from, forLength)
    }

    @JvmStatic
    public fun exprParameter(index: Int): ExprParameter {
        return ExprParameter(index)
    }

    @JvmStatic
    public fun exprPath(root: Expr, steps: List<PathStep>): ExprPath {
        return ExprPath(root, steps)
    }

    @JvmStatic
    public fun exprPosition(lhs: Expr, rhs: Expr): ExprPosition {
        return ExprPosition(lhs, rhs)
    }

    // query set variant with only required field; optional fields set to null
    @JvmStatic
    public fun exprQuerySet(body: QueryBody): ExprQuerySet {
        return ExprQuerySet(body, null, null, null)
    }

    // query set variant with all fields and nullable defaults set to null
    @JvmStatic
    public fun exprQuerySet(body: QueryBody, orderBy: OrderBy? = null, limit: Expr? = null, offset: Expr? = null): ExprQuerySet {
        return ExprQuerySet(body, orderBy, limit, offset)
    }

    @JvmStatic
    public fun exprQuerySet(body: QueryBody, orderBy: OrderBy? = null, limit: Expr? = null, offset: Expr? = null, with: With? = null): ExprQuerySet {
        return ExprQuerySet(body, orderBy, limit, offset, with)
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
    @JvmOverloads
    public fun exprSubstring(value: Expr, start: Expr? = null, length: Expr? = null): ExprSubstring {
        return ExprSubstring(value, start, length)
    }

    @JvmStatic
    @JvmOverloads
    public fun exprTrim(value: Expr, chars: Expr? = null, trimSpec: TrimSpec? = null): ExprTrim {
        return ExprTrim(value, chars, trimSpec)
    }

    @JvmStatic
    public fun exprValues(rows: List<Expr>): ExprValues {
        // TODO: Is exprTable the right name here? IMO, ExprValues should really just be called TableValueConstructor to match the EBNF
        return ExprValues(rows)
    }

    @JvmStatic
    public fun exprRowValue(values: List<Expr>): ExprRowValue {
        return ExprRowValue(values)
    }

    @JvmStatic
    public fun exprRowValue(values: List<Expr>, isExplicit: Boolean): ExprRowValue {
        return ExprRowValue(isExplicit, values)
    }

    @JvmStatic
    public fun exprVariant(value: String, encoding: String): ExprVariant {
        return ExprVariant(value, encoding)
    }

    @JvmStatic
    public fun exprVarRef(identifier: Identifier, isQualified: Boolean): ExprVarRef {
        return ExprVarRef(identifier, isQualified)
    }

    @JvmStatic
    @JvmOverloads
    @Deprecated("This is replaced by ExprWindowFunction.")
    public fun exprWindow(
        windowFunction: WindowFunction,
        expression: Expr,
        offset: Expr? = null,
        defaultValue: Expr? = null,
        over: ExprWindow.Over
    ): ExprWindow {
        return ExprWindow(windowFunction, expression, offset, defaultValue, over)
    }

    @JvmStatic
    @Deprecated("This is replaced by WindowReference.")
    public fun exprWindowOver(partitions: List<Expr>, sorts: List<Sort>): ExprWindow.Over {
        return ExprWindow.Over(partitions, sorts)
    }

    @JvmStatic
    public fun exprPathStepField(value: Identifier.Simple): PathStep.Field {
        return PathStep.Field(value)
    }

    @JvmStatic
    public fun exprPathStepElement(element: Expr): PathStep.Element {
        return PathStep.Element(element)
    }

    @JvmStatic
    public fun exprPathStepAllElements(): PathStep.AllElements {
        return PathStep.AllElements()
    }

    @JvmStatic
    public fun exprPathStepAllFields(): AllFields {
        return AllFields()
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
    @JvmOverloads
    public fun graphMatch(patterns: List<GraphPattern>, selector: GraphSelector? = null): GraphMatch {
        return GraphMatch(patterns, selector)
    }

    @JvmStatic
    @JvmOverloads
    public fun graphMatchNode(prefilter: Expr? = null, variable: String? = null, label: GraphLabel? = null): GraphPart.Node {
        return GraphPart.Node(prefilter, variable, label)
    }

    @JvmStatic
    @JvmOverloads
    public fun graphMatchEdge(
        direction: GraphDirection,
        quantifier: GraphQuantifier? = null,
        prefilter: Expr? = null,
        variable: String? = null,
        label: GraphLabel? = null
    ): GraphPart.Edge {
        return GraphPart.Edge(direction, quantifier, prefilter, variable, label)
    }

    @JvmStatic
    public fun graphMatchPattern(pattern: GraphPattern): GraphPart.Pattern {
        return GraphPart.Pattern(pattern)
    }

    @JvmStatic
    @JvmOverloads
    public fun graphPattern(
        restrictor: GraphRestrictor? = null,
        prefilter: Expr? = null,
        variable: String? = null,
        quantifier: GraphQuantifier? = null,
        parts: List<GraphPart>
    ): GraphPattern {
        return GraphPattern(restrictor, prefilter, variable, quantifier, parts)
    }

    @JvmStatic
    @JvmOverloads
    public fun graphQuantifier(lower: Long, upper: Long? = null): GraphQuantifier {
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
    public fun excludeStepStructField(symbol: Identifier.Simple): ExcludeStep.StructField {
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

    @JvmStatic
    public fun explain(options: Map<String, Literal>, statement: Statement): Explain {
        return Explain(options, statement)
    }

    @JvmStatic
    public fun from(tableRefs: List<FromTableRef>): From {
        return From(tableRefs)
    }

    @JvmStatic
    @JvmOverloads
    public fun fromExpr(expr: Expr, fromType: FromType, asAlias: Identifier.Simple? = null, atAlias: Identifier.Simple? = null): FromExpr {
        return FromExpr(expr, fromType, asAlias, atAlias)
    }

    @JvmStatic
    @JvmOverloads
    public fun fromJoin(lhs: FromTableRef, rhs: FromTableRef, joinType: JoinType? = null, condition: Expr? = null): FromJoin {
        return FromJoin(lhs, rhs, joinType, condition)
    }

    @JvmStatic
    @JvmOverloads
    public fun groupBy(strategy: GroupByStrategy, keys: List<GroupBy.Key>, asAlias: Identifier.Simple? = null): GroupBy {
        return GroupBy(strategy, keys, asAlias)
    }

    @JvmStatic
    @JvmOverloads
    public fun groupByKey(expr: Expr, asAlias: Identifier.Simple? = null): GroupBy.Key {
        return GroupBy.Key(expr, asAlias)
    }

    @JvmStatic
    public fun identifierSimple(symbol: String, isRegular: Boolean): Identifier.Simple {
        return Identifier.Simple(symbol, isRegular)
    }

    @JvmStatic
    public fun identifier(qualifier: List<Identifier.Simple>, identifier: Identifier.Simple): Identifier {
        return Identifier(qualifier, identifier)
    }

    @JvmStatic
    public fun with(elements: List<WithListElement>, isRecursive: Boolean): With {
        return With(elements, isRecursive)
    }

    @JvmStatic
    public fun withListElement(queryName: Identifier.Simple, asQuery: ExprQuerySet, columnList: List<Identifier.Simple>?): WithListElement {
        return WithListElement(queryName, asQuery, columnList)
    }

    @JvmStatic
    public fun let(bindings: List<Let.Binding>): Let {
        return Let(bindings)
    }

    @JvmStatic
    public fun letBinding(expr: Expr, asAlias: Identifier.Simple): Let.Binding {
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
    @JvmOverloads
    public fun insert(tableName: Identifier, asAlias: Identifier.Simple? = null, source: InsertSource, onConflict: OnConflict? = null): Insert {
        return Insert(tableName, asAlias, source, onConflict)
    }

    @JvmStatic
    @JvmOverloads
    public fun upsert(tableName: Identifier, asAlias: Identifier.Simple? = null, source: InsertSource): Upsert {
        return Upsert(tableName, asAlias, source)
    }

    @JvmStatic
    @JvmOverloads
    public fun replace(tableName: Identifier, asAlias: Identifier.Simple? = null, source: InsertSource): Replace {
        return Replace(tableName, asAlias, source)
    }

    @JvmStatic
    @JvmOverloads
    public fun update(tableName: Identifier, setClauses: List<SetClause>, condition: Expr? = null): Update {
        return Update(tableName, setClauses, condition)
    }

    @JvmStatic
    @JvmOverloads
    public fun delete(tableName: Identifier, condition: Expr? = null): Delete {
        return Delete(tableName, condition)
    }

    @JvmStatic
    public fun setClause(target: UpdateTarget, value: Expr): SetClause {
        return SetClause(target, value)
    }

    @JvmStatic
    @JvmOverloads
    public fun insertSourceExpr(columns: List<Identifier.Simple>? = null, expr: Expr): InsertSource.FromExpr {
        return InsertSource.FromExpr(columns, expr)
    }

    @JvmStatic
    public fun insertSourceDefault(): InsertSource.FromDefault {
        return InsertSource.FromDefault()
    }

    @JvmStatic
    @JvmOverloads
    public fun onConflict(action: ConflictAction, target: ConflictTarget? = null): OnConflict {
        return OnConflict(action, target)
    }

    @JvmStatic
    public fun conflictTargetIndex(indexes: List<Identifier.Simple>): ConflictTarget.Index {
        return ConflictTarget.Index(indexes)
    }

    @JvmStatic
    public fun conflictTargetConstraint(constraint: Identifier): ConflictTarget.Constraint {
        return ConflictTarget.Constraint(constraint)
    }

    @JvmStatic
    public fun doNothing(): ConflictAction.DoNothing {
        return ConflictAction.DoNothing()
    }

    @JvmStatic
    @JvmOverloads
    public fun doReplace(action: DoReplaceAction, condition: Expr? = null): ConflictAction.DoReplace {
        return ConflictAction.DoReplace(action, condition)
    }

    @JvmStatic
    @JvmOverloads
    public fun doUpdate(action: DoUpdateAction, condition: Expr? = null): ConflictAction.DoUpdate {
        return ConflictAction.DoUpdate(action, condition)
    }

    @JvmStatic
    public fun doReplaceActionExcluded(): DoReplaceAction.Excluded {
        return DoReplaceAction.Excluded()
    }

    @JvmStatic
    public fun doUpdateActionExcluded(): DoUpdateAction.Excluded {
        return DoUpdateAction.Excluded()
    }

    @JvmStatic
    public fun updateTarget(root: Identifier.Simple, steps: List<UpdateTargetStep>): UpdateTarget {
        return UpdateTarget(root, steps)
    }

    @JvmStatic
    public fun updateTargetStepElement(key: Int): UpdateTargetStep.Element {
        return UpdateTargetStep.Element(key)
    }

    @JvmStatic
    public fun updateTargetStepElement(key: String): UpdateTargetStep.Element {
        return UpdateTargetStep.Element(key)
    }

    @JvmStatic
    public fun updateTargetStepField(key: Identifier.Simple): UpdateTargetStep.Field {
        return UpdateTargetStep.Field(key)
    }

    // SFW variant with only required fields; optional fields are set to null
    @JvmStatic
    public fun queryBodySFW(select: Select, from: From): QueryBody.SFW {
        return QueryBody.SFW(select, null, from, null, null, null, null)
    }

    // SFW variant with `WHERE`; optional fields are set to null
    @JvmStatic
    public fun queryBodySFW(select: Select, from: From, where: Expr): QueryBody.SFW {
        return QueryBody.SFW(select, null, from, null, where, null, null)
    }

    // SFW variant with all fields and nullable defaults set to null
    @JvmStatic
    public fun queryBodySFW(
        select: Select,
        exclude: Exclude? = null,
        from: From,
        let: Let? = null,
        where: Expr? = null,
        groupBy: GroupBy? = null,
        having: Expr? = null,
        window: WindowClause? = null
    ): QueryBody.SFW {
        return QueryBody.SFW(select, exclude, from, let, where, groupBy, having, window)
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
    @JvmOverloads
    public fun selectItemExpr(expr: Expr, asAlias: Identifier.Simple? = null): SelectItem.Expr {
        return SelectItem.Expr(expr, asAlias)
    }

    @JvmStatic
    @JvmOverloads
    public fun selectList(items: List<SelectItem>, setq: SetQuantifier? = null): SelectList {
        return SelectList(items, setq)
    }

    @JvmStatic
    public fun selectPivot(key: Expr, value: Expr): SelectPivot {
        return SelectPivot(key, value)
    }

    @JvmStatic
    @JvmOverloads
    public fun selectStar(setq: SetQuantifier? = null): SelectStar {
        return SelectStar(setq)
    }

    @JvmStatic
    @JvmOverloads
    public fun selectValue(constructor: Expr, setq: SetQuantifier? = null): SelectValue {
        return SelectValue(constructor, setq)
    }

    @JvmStatic
    @JvmOverloads
    public fun setOp(setOpType: SetOpType, setq: SetQuantifier? = null): SetOp {
        return SetOp(setOpType, setq)
    }

    @JvmStatic
    @JvmOverloads
    public fun sort(expr: Expr, order: Order? = null, nulls: Nulls? = null): Sort {
        return Sort(expr, order, nulls)
    }

    //
    // DDL
    //
    @JvmStatic
    @JvmOverloads
    public fun createTable(name: Identifier, columns: List<ColumnDefinition>, constraints: List<TableConstraint>, partitionBy: PartitionBy? = null, tableProperties: List<KeyValue>): CreateTable {
        return CreateTable(
            name,
            columns,
            constraints,
            partitionBy,
            tableProperties
        )
    }

    @JvmStatic
    @JvmOverloads
    public fun columnDefinition(name: Identifier.Simple, type: DataType, isOptional: Boolean, constraints: List<AttributeConstraint>, comment: String? = null): ColumnDefinition {
        return ColumnDefinition(name, type, isOptional, constraints, comment)
    }

    @JvmStatic
    @JvmOverloads
    public fun tableConstraintUnique(name: Identifier? = null, columns: List<Identifier.Simple>, isPrimaryKey: Boolean): TableConstraint.Unique {
        return TableConstraint.Unique(name, columns, isPrimaryKey)
    }

    @JvmStatic
    @JvmOverloads
    public fun columnConstraintNullable(name: Identifier? = null, isNullable: Boolean): AttributeConstraint.Null {
        return AttributeConstraint.Null(name, isNullable)
    }

    @JvmStatic
    @JvmOverloads
    public fun columnConstraintUnique(name: Identifier? = null, isPrimaryKey: Boolean): AttributeConstraint.Unique {
        return AttributeConstraint.Unique(name, isPrimaryKey)
    }

    @JvmStatic
    @JvmOverloads
    public fun columnConstraintCheck(name: Identifier? = null, searchCondition: Expr): AttributeConstraint.Check {
        return AttributeConstraint.Check(name, searchCondition)
    }

    @JvmStatic
    public fun keyValue(key: String, value: String): KeyValue {
        return KeyValue(key, value)
    }

    @JvmStatic
    public fun partitionBy(columns: List<Identifier.Simple>): PartitionBy {
        return PartitionBy(columns)
    }
}

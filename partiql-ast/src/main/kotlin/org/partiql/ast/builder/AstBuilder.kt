package org.partiql.ast.builder

import com.amazon.ionelement.api.IonElement
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GraphMatch
import org.partiql.ast.GroupBy
import org.partiql.ast.Let
import org.partiql.ast.OnConflict
import org.partiql.ast.OrderBy
import org.partiql.ast.Over
import org.partiql.ast.Returning
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition

public fun <T : AstNode> ast(factory: AstFactory = AstFactory.DEFAULT, block: AstBuilder.() -> T) =
    AstBuilder(factory).block()

public class AstBuilder(
    private val factory: AstFactory = AstFactory.DEFAULT
) {
    public fun statementQuery(expr: Expr? = null, block: StatementQueryBuilder.() -> Unit = {}): Statement.Query {
        val builder = StatementQueryBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDmlInsert(
        target: Expr? = null,
        values: Expr? = null,
        onConflict: OnConflict.Action? = null,
        block: StatementDmlInsertBuilder.() -> Unit = {}
    ): Statement.Dml.Insert {
        val builder = StatementDmlInsertBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDmlInsertValue(
        target: Expr? = null,
        `value`: Expr? = null,
        atAlias: Expr? = null,
        index: Expr? = null,
        onConflict: OnConflict? = null,
        block: StatementDmlInsertValueBuilder.() -> Unit = {}
    ): Statement.Dml.InsertValue {
        val builder = StatementDmlInsertValueBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDmlSet(
        assignments: MutableList<Statement.Dml.Set.Assignment> = mutableListOf(),
        block: StatementDmlSetBuilder.() -> Unit = {}
    ): Statement.Dml.Set {
        val builder = StatementDmlSetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDmlSetAssignment(
        target: Expr.Path? = null,
        `value`: Expr? = null,
        block: StatementDmlSetAssignmentBuilder.() -> Unit = {}
    ): Statement.Dml.Set.Assignment {
        val builder = StatementDmlSetAssignmentBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDmlRemove(
        target: Expr.Path? = null,
        block: StatementDmlRemoveBuilder.() -> Unit = {}
    ): Statement.Dml.Remove {
        val builder = StatementDmlRemoveBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDmlDelete(
        from: From? = null,
        `where`: Expr? = null,
        returning: Returning? = null,
        block: StatementDmlDeleteBuilder.() -> Unit = {}
    ): Statement.Dml.Delete {
        val builder = StatementDmlDeleteBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDdlCreateTable(
        name: String? = null,
        definition: TableDefinition? = null,
        block: StatementDdlCreateTableBuilder.() -> Unit = {}
    ): Statement.Ddl.CreateTable {
        val builder = StatementDdlCreateTableBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDdlCreateIndex(
        name: String? = null,
        fields: MutableList<Expr> = mutableListOf(),
        block: StatementDdlCreateIndexBuilder.() -> Unit = {}
    ): Statement.Ddl.CreateIndex {
        val builder = StatementDdlCreateIndexBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDdlDropTable(
        identifier: Expr.Identifier? = null,
        block: StatementDdlDropTableBuilder.() -> Unit = {}
    ): Statement.Ddl.DropTable {
        val builder = StatementDdlDropTableBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDdlDropIndex(
        table: Expr.Identifier? = null,
        keys: Expr.Identifier? = null,
        block: StatementDdlDropIndexBuilder.() -> Unit = {}
    ): Statement.Ddl.DropIndex {
        val builder = StatementDdlDropIndexBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementExec(
        procedure: String? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: StatementExecBuilder.() -> Unit = {}
    ): Statement.Exec {
        val builder = StatementExecBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementExplain(
        target: Statement.Explain.Target? = null,
        block: StatementExplainBuilder.() -> Unit = {}
    ): Statement.Explain {
        val builder = StatementExplainBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementExplainTargetDomain(
        statement: Statement? = null,
        type: String? = null,
        format: String? = null,
        block: StatementExplainTargetDomainBuilder.() -> Unit = {}
    ): Statement.Explain.Target.Domain {
        val builder = StatementExplainTargetDomainBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprMissing(block: ExprMissingBuilder.() -> Unit = {}): Expr.Missing {
        val builder = ExprMissingBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprLit(`value`: IonElement? = null, block: ExprLitBuilder.() -> Unit = {}): Expr.Lit {
        val builder = ExprLitBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprIdentifier(
        name: String? = null,
        case: Expr.Case? = null,
        scope: Expr.Identifier.Scope? = null,
        block: ExprIdentifierBuilder.() -> Unit = {}
    ): Expr.Identifier {
        val builder = ExprIdentifierBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPath(
        root: Expr? = null,
        steps: MutableList<Expr.Path.Step> = mutableListOf(),
        block: ExprPathBuilder.() -> Unit = {}
    ): Expr.Path {
        val builder = ExprPathBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPathStepKey(
        `value`: Expr? = null,
        block: ExprPathStepKeyBuilder.() -> Unit = {}
    ): Expr.Path.Step.Key {
        val builder = ExprPathStepKeyBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPathStepWildcard(block: ExprPathStepWildcardBuilder.() -> Unit = {}): Expr.Path.Step.Wildcard {
        val builder = ExprPathStepWildcardBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPathStepUnpivot(block: ExprPathStepUnpivotBuilder.() -> Unit = {}): Expr.Path.Step.Unpivot {
        val builder = ExprPathStepUnpivotBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCall(
        function: String? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: ExprCallBuilder.() -> Unit = {}
    ): Expr.Call {
        val builder = ExprCallBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprAgg(
        function: String? = null,
        args: MutableList<Expr> = mutableListOf(),
        quantifier: SetQuantifier? = null,
        block: ExprAggBuilder.() -> Unit = {}
    ): Expr.Agg {
        val builder = ExprAggBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprParameter(index: Int? = null, block: ExprParameterBuilder.() -> Unit = {}): Expr.Parameter {
        val builder = ExprParameterBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprUnary(
        op: Expr.Unary.Op? = null,
        expr: Expr? = null,
        block: ExprUnaryBuilder.() -> Unit = {}
    ): Expr.Unary {
        val builder = ExprUnaryBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprBinary(
        op: Expr.Binary.Op? = null,
        lhs: Expr? = null,
        rhs: Expr? = null,
        block: ExprBinaryBuilder.() -> Unit = {}
    ): Expr.Binary {
        val builder = ExprBinaryBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCollection(
        type: Expr.Collection.Type? = null,
        values: MutableList<Expr> = mutableListOf(),
        block: ExprCollectionBuilder.() -> Unit = {}
    ): Expr.Collection {
        val builder = ExprCollectionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprTuple(
        fields: MutableList<Expr.Tuple.Field> = mutableListOf(),
        block: ExprTupleBuilder.() -> Unit = {}
    ): Expr.Tuple {
        val builder = ExprTupleBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprTupleField(
        name: Expr? = null,
        `value`: Expr? = null,
        block: ExprTupleFieldBuilder.() -> Unit = {}
    ): Expr.Tuple.Field {
        val builder = ExprTupleFieldBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprDate(
        year: Long? = null,
        month: Long? = null,
        day: Long? = null,
        block: ExprDateBuilder.() -> Unit = {}
    ): Expr.Date {
        val builder = ExprDateBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprTime(
        hour: Long? = null,
        minute: Long? = null,
        second: Long? = null,
        nano: Long? = null,
        precision: Long? = null,
        tzOffsetMinutes: Long? = null,
        block: ExprTimeBuilder.() -> Unit = {}
    ): Expr.Time {
        val builder = ExprTimeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprLike(
        `value`: Expr? = null,
        pattern: Expr? = null,
        escape: Expr? = null,
        block: ExprLikeBuilder.() -> Unit = {}
    ): Expr.Like {
        val builder = ExprLikeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprBetween(
        `value`: Expr? = null,
        from: Expr? = null,
        to: Expr? = null,
        block: ExprBetweenBuilder.() -> Unit = {}
    ): Expr.Between {
        val builder = ExprBetweenBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprInCollection(
        lhs: Expr? = null,
        rhs: Expr? = null,
        block: ExprInCollectionBuilder.() -> Unit = {}
    ): Expr.InCollection {
        val builder = ExprInCollectionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprIsType(
        `value`: Expr? = null,
        type: Expr.Collection.Type? = null,
        block: ExprIsTypeBuilder.() -> Unit = {}
    ): Expr.IsType {
        val builder = ExprIsTypeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCase(
        expr: Expr? = null,
        branches: MutableList<Expr.Case.Branch> = mutableListOf(),
        default: Expr? = null,
        block: ExprCaseBuilder.() -> Unit = {}
    ): Expr.Case {
        val builder = ExprCaseBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCaseBranch(
        condition: Expr? = null,
        expr: Expr? = null,
        block: ExprCaseBranchBuilder.() -> Unit = {}
    ): Expr.Case.Branch {
        val builder = ExprCaseBranchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCoalesce(
        args: MutableList<Expr> = mutableListOf(),
        block: ExprCoalesceBuilder.() -> Unit = {}
    ): Expr.Coalesce {
        val builder = ExprCoalesceBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprNullIf(
        expr1: Expr? = null,
        expr2: Expr? = null,
        block: ExprNullIfBuilder.() -> Unit = {}
    ): Expr.NullIf {
        val builder = ExprNullIfBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCast(
        `value`: Expr? = null,
        asType: Expr.Collection.Type? = null,
        block: ExprCastBuilder.() -> Unit = {}
    ): Expr.Cast {
        val builder = ExprCastBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCanCast(
        `value`: Expr? = null,
        asType: Expr.Collection.Type? = null,
        block: ExprCanCastBuilder.() -> Unit = {}
    ): Expr.CanCast {
        val builder = ExprCanCastBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCanLosslessCast(
        `value`: Expr? = null,
        asType: Expr.Collection.Type? = null,
        block: ExprCanLosslessCastBuilder.() -> Unit = {}
    ): Expr.CanLosslessCast {
        val builder = ExprCanLosslessCastBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprOuterBagOp(
        op: Expr.OuterBagOp.Op? = null,
        quantifier: SetQuantifier? = null,
        lhs: Expr? = null,
        rhs: Expr? = null,
        block: ExprOuterBagOpBuilder.() -> Unit = {}
    ): Expr.OuterBagOp {
        val builder = ExprOuterBagOpBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprSfw(
        select: Select? = null,
        from: From? = null,
        let: Let? = null,
        `where`: Expr? = null,
        groupBy: GroupBy? = null,
        having: Expr? = null,
        orderBy: OrderBy? = null,
        limit: Expr? = null,
        offset: Expr? = null,
        block: ExprSfwBuilder.() -> Unit = {}
    ): Expr.Sfw {
        val builder = ExprSfwBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprMatch(
        expr: Expr? = null,
        pattern: GraphMatch? = null,
        block: ExprMatchBuilder.() -> Unit = {}
    ): Expr.Match {
        val builder = ExprMatchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprWindow(
        function: String? = null,
        over: Over? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: ExprWindowBuilder.() -> Unit = {}
    ): Expr.Window {
        val builder = ExprWindowBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectStar(block: SelectStarBuilder.() -> Unit = {}): Select.Star {
        val builder = SelectStarBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectProject(
        items: MutableList<Select.Project.Item> = mutableListOf(),
        block: SelectProjectBuilder.() -> Unit = {}
    ): Select.Project {
        val builder = SelectProjectBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectProjectItemAll(
        expr: Expr? = null,
        block: SelectProjectItemAllBuilder.() -> Unit = {}
    ): Select.Project.Item.All {
        val builder = SelectProjectItemAllBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectProjectItemVar(
        expr: Expr? = null,
        asAlias: String? = null,
        block: SelectProjectItemVarBuilder.() -> Unit = {}
    ): Select.Project.Item.Var {
        val builder = SelectProjectItemVarBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectPivot(
        `value`: Expr? = null,
        key: Expr? = null,
        block: SelectPivotBuilder.() -> Unit = {}
    ): Select.Pivot {
        val builder = SelectPivotBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectValue(`constructor`: Expr? = null, block: SelectValueBuilder.() -> Unit = {}): Select.Value {
        val builder = SelectValueBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun fromCollection(
        expr: Expr? = null,
        unpivot: Boolean? = null,
        asAlias: String? = null,
        atAlias: String? = null,
        byAlias: String? = null,
        block: FromCollectionBuilder.() -> Unit = {}
    ): From.Collection {
        val builder = FromCollectionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun fromJoin(
        type: From.Join.Type? = null,
        condition: Expr? = null,
        lhs: From? = null,
        rhs: From? = null,
        block: FromJoinBuilder.() -> Unit = {}
    ): From.Join {
        val builder = FromJoinBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun let(block: LetBuilder.() -> Unit = {}): Let {
        val builder = LetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun groupBy(
        strategy: GroupBy.Strategy? = null,
        keys: MutableList<GroupBy.Key> = mutableListOf(),
        asAlias: String? = null,
        block: GroupByBuilder.() -> Unit = {}
    ): GroupBy {
        val builder = GroupByBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun groupByKey(
        expr: Expr? = null,
        asAlias: String? = null,
        block: GroupByKeyBuilder.() -> Unit = {}
    ): GroupBy.Key {
        val builder = GroupByKeyBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun orderBy(
        sorts: MutableList<OrderBy.Sort> = mutableListOf(),
        block: OrderByBuilder.() -> Unit = {}
    ): OrderBy {
        val builder = OrderByBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun orderBySort(
        expr: Expr? = null,
        dir: OrderBy.Sort.Dir? = null,
        nulls: OrderBy.Sort.Nulls? = null,
        block: OrderBySortBuilder.() -> Unit = {}
    ): OrderBy.Sort {
        val builder = OrderBySortBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatch(
        patterns: MutableList<GraphMatch.Pattern> = mutableListOf(),
        selector: GraphMatch.Selector? = null,
        block: GraphMatchBuilder.() -> Unit = {}
    ): GraphMatch {
        val builder = GraphMatchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchPattern(
        restrictor: GraphMatch.Restrictor? = null,
        prefilter: Expr? = null,
        variable: String? = null,
        quantifier: GraphMatch.Quantifier? = null,
        parts: MutableList<GraphMatch.Pattern.Part> = mutableListOf(),
        block: GraphMatchPatternBuilder.() -> Unit = {}
    ): GraphMatch.Pattern {
        val builder = GraphMatchPatternBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchPatternPartNode(
        prefilter: Expr? = null,
        variable: String? = null,
        label: MutableList<String> = mutableListOf(),
        block: GraphMatchPatternPartNodeBuilder.() -> Unit = {}
    ): GraphMatch.Pattern.Part.Node {
        val builder = GraphMatchPatternPartNodeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchPatternPartEdge(
        direction: GraphMatch.Direction? = null,
        quantifier: GraphMatch.Quantifier? = null,
        prefilter: Expr? = null,
        variable: String? = null,
        label: MutableList<String> = mutableListOf(),
        block: GraphMatchPatternPartEdgeBuilder.() -> Unit = {}
    ): GraphMatch.Pattern.Part.Edge {
        val builder = GraphMatchPatternPartEdgeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchQuantifier(
        lower: Long? = null,
        upper: Int? = null,
        block: GraphMatchQuantifierBuilder.() -> Unit = {}
    ): GraphMatch.Quantifier {
        val builder = GraphMatchQuantifierBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAnyShortest(
        block: GraphMatchSelectorAnyShortestBuilder.() -> Unit = {}
    ): GraphMatch.Selector.AnyShortest {
        val builder = GraphMatchSelectorAnyShortestBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAllShortest(
        block: GraphMatchSelectorAllShortestBuilder.() -> Unit = {}
    ): GraphMatch.Selector.AllShortest {
        val builder = GraphMatchSelectorAllShortestBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAny(block: GraphMatchSelectorAnyBuilder.() -> Unit = {}): GraphMatch.Selector.Any {
        val builder = GraphMatchSelectorAnyBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAnyK(
        k: Long? = null,
        block: GraphMatchSelectorAnyKBuilder.() -> Unit = {}
    ): GraphMatch.Selector.AnyK {
        val builder = GraphMatchSelectorAnyKBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorShortestK(
        k: Long? = null,
        block: GraphMatchSelectorShortestKBuilder.() -> Unit = {}
    ): GraphMatch.Selector.ShortestK {
        val builder = GraphMatchSelectorShortestKBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorShortestKGroup(
        k: Long? = null,
        block: GraphMatchSelectorShortestKGroupBuilder.() -> Unit = {}
    ): GraphMatch.Selector.ShortestKGroup {
        val builder = GraphMatchSelectorShortestKGroupBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun over(
        partitions: MutableList<Expr> = mutableListOf(),
        sorts: MutableList<OrderBy.Sort> = mutableListOf(),
        block: OverBuilder.() -> Unit = {}
    ): Over {
        val builder = OverBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflict(
        expr: Expr? = null,
        action: OnConflict.Action? = null,
        block: OnConflictBuilder.() -> Unit = {}
    ): OnConflict {
        val builder = OnConflictBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictActionDoReplace(
        `value`: OnConflict.Value? = null,
        block: OnConflictActionDoReplaceBuilder.() -> Unit = {}
    ): OnConflict.Action.DoReplace {
        val builder = OnConflictActionDoReplaceBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictActionDoUpdate(
        `value`: OnConflict.Value? = null,
        block: OnConflictActionDoUpdateBuilder.() -> Unit = {}
    ): OnConflict.Action.DoUpdate {
        val builder = OnConflictActionDoUpdateBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictActionDoNothing(block: OnConflictActionDoNothingBuilder.() -> Unit = {}): OnConflict.Action.DoNothing {
        val builder = OnConflictActionDoNothingBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returning(
        columns: MutableList<Returning.Column> = mutableListOf(),
        block: ReturningBuilder.() -> Unit = {}
    ): Returning {
        val builder = ReturningBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returningColumn(
        status: Returning.Column.Status? = null,
        age: Returning.Column.Age? = null,
        `value`: Returning.Column.Value? = null,
        block: ReturningColumnBuilder.() -> Unit = {}
    ): Returning.Column {
        val builder = ReturningColumnBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returningColumnValueWildcard(
        block: ReturningColumnValueWildcardBuilder.() -> Unit = {}
    ): Returning.Column.Value.Wildcard {
        val builder = ReturningColumnValueWildcardBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returningColumnValueExpression(
        expr: Expr? = null,
        block: ReturningColumnValueExpressionBuilder.() -> Unit = {}
    ): Returning.Column.Value.Expression {
        val builder = ReturningColumnValueExpressionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinition(
        columns: MutableList<TableDefinition.Column> = mutableListOf(),
        block: TableDefinitionBuilder.() -> Unit = {}
    ): TableDefinition {
        val builder = TableDefinitionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumn(
        name: String? = null,
        type: Expr.Collection.Type? = null,
        constraints: MutableList<TableDefinition.Column.Constraint> = mutableListOf(),
        block: TableDefinitionColumnBuilder.() -> Unit = {}
    ): TableDefinition.Column {
        val builder = TableDefinitionColumnBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraintNullable(
        block: TableDefinitionColumnConstraintNullableBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint.Nullable {
        val builder = TableDefinitionColumnConstraintNullableBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraintNotNull(
        block: TableDefinitionColumnConstraintNotNullBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint.NotNull {
        val builder = TableDefinitionColumnConstraintNotNullBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraintCheck(
        expr: Expr? = null,
        block: TableDefinitionColumnConstraintCheckBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint.Check {
        val builder = TableDefinitionColumnConstraintCheckBuilder()
        builder.block()
        return builder.build(factory)
    }
}

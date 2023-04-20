package org.partiql.ast.builder

import com.amazon.ionelement.api.IonElement
import org.partiql.ast.AstNode
import org.partiql.ast.Case
import org.partiql.ast.Except
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GraphMatch
import org.partiql.ast.GroupBy
import org.partiql.ast.Intersect
import org.partiql.ast.Let
import org.partiql.ast.OnConflict
import org.partiql.ast.OrderBy
import org.partiql.ast.Over
import org.partiql.ast.Returning
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.Type
import org.partiql.ast.Union

public fun <T : AstNode> ast(factory: AstFactory = AstFactory.DEFAULT, block: AstBuilder.() -> T) =
    AstBuilder(factory).block()

public class AstBuilder(
    private val factory: AstFactory = AstFactory.DEFAULT
) {
    public fun statementQuery(
        id: Int? = null,
        expr: Expr? = null,
        block: StatementQueryBuilder.() -> Unit = {}
    ): Statement.Query {
        val builder = StatementQueryBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLInsert(
        id: Int? = null,
        target: Statement.DML.Target? = null,
        `value`: Expr? = null,
        onConflict: OnConflict? = null,
        block: StatementDmlInsertBuilder.() -> Unit = {}
    ): Statement.DML.Insert {
        val builder = StatementDmlInsertBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLInsertValue(
        id: Int? = null,
        target: Statement.DML.Target? = null,
        `value`: Expr? = null,
        index: Expr? = null,
        onConflict: OnConflict? = null,
        returning: Returning? = null,
        block: StatementDmlInsertValueBuilder.() -> Unit = {}
    ): Statement.DML.InsertValue {
        val builder = StatementDmlInsertValueBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLUpsert(
        id: Int? = null,
        target: Expr.Identifier? = null,
        `value`: Expr? = null,
        block: StatementDmlUpsertBuilder.() -> Unit = {}
    ): Statement.DML.Upsert {
        val builder = StatementDmlUpsertBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLReplace(
        id: Int? = null,
        target: Expr.Identifier? = null,
        `value`: Expr? = null,
        block: StatementDmlReplaceBuilder.() -> Unit = {}
    ): Statement.DML.Replace {
        val builder = StatementDmlReplaceBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLUpdate(
        id: Int? = null,
        target: Statement.DML.Target? = null,
        assignments: MutableList<Statement.DML.Update.Assignment> = mutableListOf(),
        block: StatementDmlUpdateBuilder.() -> Unit = {}
    ): Statement.DML.Update {
        val builder = StatementDmlUpdateBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLUpdateAssignment(
        id: Int? = null,
        target: Expr.Path? = null,
        `value`: Expr? = null,
        block: StatementDmlUpdateAssignmentBuilder.() -> Unit = {}
    ): Statement.DML.Update.Assignment {
        val builder = StatementDmlUpdateAssignmentBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLRemove(
        id: Int? = null,
        target: Expr.Path? = null,
        block: StatementDmlRemoveBuilder.() -> Unit = {}
    ): Statement.DML.Remove {
        val builder = StatementDmlRemoveBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLDelete(
        id: Int? = null,
        from: From? = null,
        `where`: Expr? = null,
        returning: Returning? = null,
        block: StatementDmlDeleteBuilder.() -> Unit = {}
    ): Statement.DML.Delete {
        val builder = StatementDmlDeleteBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLBatch(
        id: Int? = null,
        from: From? = null,
        ops: MutableList<Statement.DML.Batch.Op> = mutableListOf(),
        `where`: Expr? = null,
        returning: Returning? = null,
        block: StatementDmlBatchBuilder.() -> Unit = {}
    ): Statement.DML.Batch {
        val builder = StatementDmlBatchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLBatchOpInsert(
        id: Int? = null,
        target: Statement.DML.Target? = null,
        `value`: Expr? = null,
        onConflict: OnConflict? = null,
        block: StatementDmlBatchOpInsertBuilder.() -> Unit = {}
    ): Statement.DML.Batch.Op.Insert {
        val builder = StatementDmlBatchOpInsertBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLBatchOpInsertValue(
        id: Int? = null,
        target: Statement.DML.Target? = null,
        `value`: Expr? = null,
        index: Expr? = null,
        onConflict: OnConflict? = null,
        block: StatementDmlBatchOpInsertValueBuilder.() -> Unit = {}
    ): Statement.DML.Batch.Op.InsertValue {
        val builder = StatementDmlBatchOpInsertValueBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLBatchOpSet(
        id: Int? = null,
        assignments: MutableList<Statement.DML.Update.Assignment> = mutableListOf(),
        block: StatementDmlBatchOpSetBuilder.() -> Unit = {}
    ): Statement.DML.Batch.Op.Set {
        val builder = StatementDmlBatchOpSetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLBatchOpRemove(
        id: Int? = null,
        target: Expr.Path? = null,
        block: StatementDmlBatchOpRemoveBuilder.() -> Unit = {}
    ): Statement.DML.Batch.Op.Remove {
        val builder = StatementDmlBatchOpRemoveBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLBatchOpDelete(
        id: Int? = null,
        block: StatementDmlBatchOpDeleteBuilder.() -> Unit = {}
    ): Statement.DML.Batch.Op.Delete {
        val builder = StatementDmlBatchOpDeleteBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDMLTarget(
        id: Int? = null,
        table: Expr? = null,
        block: StatementDmlTargetBuilder.() -> Unit = {}
    ): Statement.DML.Target {
        val builder = StatementDmlTargetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDDLCreateTable(
        id: Int? = null,
        name: Expr.Identifier? = null,
        definition: TableDefinition? = null,
        block: StatementDdlCreateTableBuilder.() -> Unit = {}
    ): Statement.DDL.CreateTable {
        val builder = StatementDdlCreateTableBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDDLCreateIndex(
        id: Int? = null,
        name: Expr.Identifier? = null,
        fields: MutableList<Expr> = mutableListOf(),
        block: StatementDdlCreateIndexBuilder.() -> Unit = {}
    ): Statement.DDL.CreateIndex {
        val builder = StatementDdlCreateIndexBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDDLDropTable(
        id: Int? = null,
        identifier: Expr.Identifier? = null,
        block: StatementDdlDropTableBuilder.() -> Unit = {}
    ): Statement.DDL.DropTable {
        val builder = StatementDdlDropTableBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementDDLDropIndex(
        id: Int? = null,
        table: Expr.Identifier? = null,
        keys: Expr.Identifier? = null,
        block: StatementDdlDropIndexBuilder.() -> Unit = {}
    ): Statement.DDL.DropIndex {
        val builder = StatementDdlDropIndexBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementExec(
        id: Int? = null,
        procedure: String? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: StatementExecBuilder.() -> Unit = {}
    ): Statement.Exec {
        val builder = StatementExecBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementExplain(
        id: Int? = null,
        target: Statement.Explain.Target? = null,
        block: StatementExplainBuilder.() -> Unit = {}
    ): Statement.Explain {
        val builder = StatementExplainBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun statementExplainTargetDomain(
        id: Int? = null,
        statement: Statement? = null,
        type: String? = null,
        format: String? = null,
        block: StatementExplainTargetDomainBuilder.() -> Unit = {}
    ): Statement.Explain.Target.Domain {
        val builder = StatementExplainTargetDomainBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun type(
        id: Int? = null,
        identifier: String? = null,
        parameters: MutableList<IonElement> = mutableListOf(),
        block: TypeBuilder.() -> Unit = {}
    ): Type {
        val builder = TypeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprMissing(id: Int? = null, block: ExprMissingBuilder.() -> Unit = {}): Expr.Missing {
        val builder = ExprMissingBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprLit(
        id: Int? = null,
        `value`: IonElement? = null,
        block: ExprLitBuilder.() -> Unit = {}
    ): Expr.Lit {
        val builder = ExprLitBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprIdentifier(
        id: Int? = null,
        name: String? = null,
        case: Case? = null,
        scope: Expr.Identifier.Scope? = null,
        block: ExprIdentifierBuilder.() -> Unit = {}
    ): Expr.Identifier {
        val builder = ExprIdentifierBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPath(
        id: Int? = null,
        root: Expr? = null,
        steps: MutableList<Expr.Path.Step> = mutableListOf(),
        block: ExprPathBuilder.() -> Unit = {}
    ): Expr.Path {
        val builder = ExprPathBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPathStepIndex(
        id: Int? = null,
        key: Expr? = null,
        case: Case? = null,
        block: ExprPathStepIndexBuilder.() -> Unit = {}
    ): Expr.Path.Step.Index {
        val builder = ExprPathStepIndexBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPathStepWildcard(
        id: Int? = null,
        block: ExprPathStepWildcardBuilder.() -> Unit = {}
    ): Expr.Path.Step.Wildcard {
        val builder = ExprPathStepWildcardBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprPathStepUnpivot(
        id: Int? = null,
        block: ExprPathStepUnpivotBuilder.() -> Unit = {}
    ): Expr.Path.Step.Unpivot {
        val builder = ExprPathStepUnpivotBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCall(
        id: Int? = null,
        function: String? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: ExprCallBuilder.() -> Unit = {}
    ): Expr.Call {
        val builder = ExprCallBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprAgg(
        id: Int? = null,
        function: String? = null,
        args: MutableList<Expr> = mutableListOf(),
        quantifier: SetQuantifier? = null,
        block: ExprAggBuilder.() -> Unit = {}
    ): Expr.Agg {
        val builder = ExprAggBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprParameter(
        id: Int? = null,
        index: Int? = null,
        block: ExprParameterBuilder.() -> Unit = {}
    ): Expr.Parameter {
        val builder = ExprParameterBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprUnary(
        id: Int? = null,
        op: Expr.Unary.Op? = null,
        expr: Expr? = null,
        block: ExprUnaryBuilder.() -> Unit = {}
    ): Expr.Unary {
        val builder = ExprUnaryBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprBinary(
        id: Int? = null,
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
        id: Int? = null,
        type: Expr.Collection.Type? = null,
        values: MutableList<Expr> = mutableListOf(),
        block: ExprCollectionBuilder.() -> Unit = {}
    ): Expr.Collection {
        val builder = ExprCollectionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprTuple(
        id: Int? = null,
        fields: MutableList<Expr.Tuple.Field> = mutableListOf(),
        block: ExprTupleBuilder.() -> Unit = {}
    ): Expr.Tuple {
        val builder = ExprTupleBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprTupleField(
        id: Int? = null,
        name: Expr? = null,
        `value`: Expr? = null,
        block: ExprTupleFieldBuilder.() -> Unit = {}
    ): Expr.Tuple.Field {
        val builder = ExprTupleFieldBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprDate(
        id: Int? = null,
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
        id: Int? = null,
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
        id: Int? = null,
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
        id: Int? = null,
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
        id: Int? = null,
        lhs: Expr? = null,
        rhs: Expr? = null,
        block: ExprInCollectionBuilder.() -> Unit = {}
    ): Expr.InCollection {
        val builder = ExprInCollectionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprIsType(
        id: Int? = null,
        `value`: Expr? = null,
        type: Type? = null,
        block: ExprIsTypeBuilder.() -> Unit = {}
    ): Expr.IsType {
        val builder = ExprIsTypeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprSwitch(
        id: Int? = null,
        expr: Expr? = null,
        branches: MutableList<Expr.Switch.Branch> = mutableListOf(),
        default: Expr? = null,
        block: ExprSwitchBuilder.() -> Unit = {}
    ): Expr.Switch {
        val builder = ExprSwitchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprSwitchBranch(
        id: Int? = null,
        condition: Expr? = null,
        expr: Expr? = null,
        block: ExprSwitchBranchBuilder.() -> Unit = {}
    ): Expr.Switch.Branch {
        val builder = ExprSwitchBranchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCoalesce(
        id: Int? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: ExprCoalesceBuilder.() -> Unit = {}
    ): Expr.Coalesce {
        val builder = ExprCoalesceBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprNullIf(
        id: Int? = null,
        expr0: Expr? = null,
        expr1: Expr? = null,
        block: ExprNullIfBuilder.() -> Unit = {}
    ): Expr.NullIf {
        val builder = ExprNullIfBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCast(
        id: Int? = null,
        `value`: Expr? = null,
        asType: Type? = null,
        block: ExprCastBuilder.() -> Unit = {}
    ): Expr.Cast {
        val builder = ExprCastBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCanCast(
        id: Int? = null,
        `value`: Expr? = null,
        asType: Type? = null,
        block: ExprCanCastBuilder.() -> Unit = {}
    ): Expr.CanCast {
        val builder = ExprCanCastBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprCanLosslessCast(
        id: Int? = null,
        `value`: Expr? = null,
        asType: Type? = null,
        block: ExprCanLosslessCastBuilder.() -> Unit = {}
    ): Expr.CanLosslessCast {
        val builder = ExprCanLosslessCastBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprSet(
        id: Int? = null,
        op: Expr.Set.Op? = null,
        quantifier: SetQuantifier? = null,
        outer: Boolean? = null,
        lhs: Expr? = null,
        rhs: Expr? = null,
        block: ExprSetBuilder.() -> Unit = {}
    ): Expr.Set {
        val builder = ExprSetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprSFW(
        id: Int? = null,
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
    ): Expr.SFW {
        val builder = ExprSfwBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprMatch(
        id: Int? = null,
        expr: Expr? = null,
        pattern: GraphMatch? = null,
        block: ExprMatchBuilder.() -> Unit = {}
    ): Expr.Match {
        val builder = ExprMatchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun exprWindow(
        id: Int? = null,
        function: String? = null,
        over: Over? = null,
        args: MutableList<Expr> = mutableListOf(),
        block: ExprWindowBuilder.() -> Unit = {}
    ): Expr.Window {
        val builder = ExprWindowBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectStar(
        id: Int? = null,
        quantifier: SetQuantifier? = null,
        block: SelectStarBuilder.() -> Unit = {}
    ): Select.Star {
        val builder = SelectStarBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectProject(
        id: Int? = null,
        quantifier: SetQuantifier? = null,
        items: MutableList<Select.Project.Item> = mutableListOf(),
        block: SelectProjectBuilder.() -> Unit = {}
    ): Select.Project {
        val builder = SelectProjectBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectProjectItemAll(
        id: Int? = null,
        expr: Expr? = null,
        block: SelectProjectItemAllBuilder.() -> Unit = {}
    ): Select.Project.Item.All {
        val builder = SelectProjectItemAllBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectProjectItemVar(
        id: Int? = null,
        expr: Expr? = null,
        asAlias: String? = null,
        block: SelectProjectItemVarBuilder.() -> Unit = {}
    ): Select.Project.Item.Var {
        val builder = SelectProjectItemVarBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectPivot(
        id: Int? = null,
        `value`: Expr? = null,
        key: Expr? = null,
        block: SelectPivotBuilder.() -> Unit = {}
    ): Select.Pivot {
        val builder = SelectPivotBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun selectValue(
        id: Int? = null,
        quantifier: SetQuantifier? = null,
        `constructor`: Expr? = null,
        block: SelectValueBuilder.() -> Unit = {}
    ): Select.Value {
        val builder = SelectValueBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun fromCollection(
        id: Int? = null,
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
        id: Int? = null,
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

    public fun let(
        id: Int? = null,
        bindings: MutableList<Let.Binding> = mutableListOf(),
        block: LetBuilder.() -> Unit = {}
    ): Let {
        val builder = LetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun letBinding(
        id: Int? = null,
        expr: Expr? = null,
        asAlias: String? = null,
        block: LetBindingBuilder.() -> Unit = {}
    ): Let.Binding {
        val builder = LetBindingBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun groupBy(
        id: Int? = null,
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
        id: Int? = null,
        expr: Expr? = null,
        asAlias: String? = null,
        block: GroupByKeyBuilder.() -> Unit = {}
    ): GroupBy.Key {
        val builder = GroupByKeyBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun orderBy(
        id: Int? = null,
        sorts: MutableList<OrderBy.Sort> = mutableListOf(),
        block: OrderByBuilder.() -> Unit = {}
    ): OrderBy {
        val builder = OrderByBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun orderBySort(
        id: Int? = null,
        expr: Expr? = null,
        dir: OrderBy.Sort.Dir? = null,
        nulls: OrderBy.Sort.Nulls? = null,
        block: OrderBySortBuilder.() -> Unit = {}
    ): OrderBy.Sort {
        val builder = OrderBySortBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun union(
        id: Int? = null,
        quantifier: SetQuantifier? = null,
        lhs: Expr.SFW? = null,
        rhs: Expr.SFW? = null,
        block: UnionBuilder.() -> Unit = {}
    ): Union {
        val builder = UnionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun intersect(
        id: Int? = null,
        quantifier: SetQuantifier? = null,
        lhs: Expr.SFW? = null,
        rhs: Expr.SFW? = null,
        block: IntersectBuilder.() -> Unit = {}
    ): Intersect {
        val builder = IntersectBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun except(
        id: Int? = null,
        quantifier: SetQuantifier? = null,
        lhs: Expr.SFW? = null,
        rhs: Expr.SFW? = null,
        block: ExceptBuilder.() -> Unit = {}
    ): Except {
        val builder = ExceptBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatch(
        id: Int? = null,
        patterns: MutableList<GraphMatch.Pattern> = mutableListOf(),
        selector: GraphMatch.Selector? = null,
        block: GraphMatchBuilder.() -> Unit = {}
    ): GraphMatch {
        val builder = GraphMatchBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchPattern(
        id: Int? = null,
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
        id: Int? = null,
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
        id: Int? = null,
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
        id: Int? = null,
        lower: Long? = null,
        upper: Long? = null,
        block: GraphMatchQuantifierBuilder.() -> Unit = {}
    ): GraphMatch.Quantifier {
        val builder = GraphMatchQuantifierBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAnyShortest(
        id: Int? = null,
        block: GraphMatchSelectorAnyShortestBuilder.() -> Unit = {}
    ): GraphMatch.Selector.AnyShortest {
        val builder = GraphMatchSelectorAnyShortestBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAllShortest(
        id: Int? = null,
        block: GraphMatchSelectorAllShortestBuilder.() -> Unit = {}
    ): GraphMatch.Selector.AllShortest {
        val builder = GraphMatchSelectorAllShortestBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAny(
        id: Int? = null,
        block: GraphMatchSelectorAnyBuilder.() -> Unit = {}
    ): GraphMatch.Selector.Any {
        val builder = GraphMatchSelectorAnyBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorAnyK(
        id: Int? = null,
        k: Long? = null,
        block: GraphMatchSelectorAnyKBuilder.() -> Unit = {}
    ): GraphMatch.Selector.AnyK {
        val builder = GraphMatchSelectorAnyKBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorShortestK(
        id: Int? = null,
        k: Long? = null,
        block: GraphMatchSelectorShortestKBuilder.() -> Unit = {}
    ): GraphMatch.Selector.ShortestK {
        val builder = GraphMatchSelectorShortestKBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun graphMatchSelectorShortestKGroup(
        id: Int? = null,
        k: Long? = null,
        block: GraphMatchSelectorShortestKGroupBuilder.() -> Unit = {}
    ): GraphMatch.Selector.ShortestKGroup {
        val builder = GraphMatchSelectorShortestKGroupBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun over(
        id: Int? = null,
        partitions: MutableList<Expr> = mutableListOf(),
        sorts: MutableList<OrderBy.Sort> = mutableListOf(),
        block: OverBuilder.() -> Unit = {}
    ): Over {
        val builder = OverBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflict(
        id: Int? = null,
        target: OnConflict.Target? = null,
        action: OnConflict.Action? = null,
        block: OnConflictBuilder.() -> Unit = {}
    ): OnConflict {
        val builder = OnConflictBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictActionDoReplace(
        id: Int? = null,
        `value`: OnConflict.Value? = null,
        block: OnConflictActionDoReplaceBuilder.() -> Unit = {}
    ): OnConflict.Action.DoReplace {
        val builder = OnConflictActionDoReplaceBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictActionDoUpdate(
        id: Int? = null,
        `value`: OnConflict.Value? = null,
        block: OnConflictActionDoUpdateBuilder.() -> Unit = {}
    ): OnConflict.Action.DoUpdate {
        val builder = OnConflictActionDoUpdateBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictActionDoNothing(
        id: Int? = null,
        block: OnConflictActionDoNothingBuilder.() -> Unit = {}
    ): OnConflict.Action.DoNothing {
        val builder = OnConflictActionDoNothingBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictTargetCondition(
        id: Int? = null,
        condition: Expr? = null,
        block: OnConflictTargetConditionBuilder.() -> Unit = {}
    ): OnConflict.Target.Condition {
        val builder = OnConflictTargetConditionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictTargetSymbols(
        id: Int? = null,
        symbols: MutableList<String> = mutableListOf(),
        block: OnConflictTargetSymbolsBuilder.() -> Unit = {}
    ): OnConflict.Target.Symbols {
        val builder = OnConflictTargetSymbolsBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun onConflictTargetConstraint(
        id: Int? = null,
        constraint: String? = null,
        block: OnConflictTargetConstraintBuilder.() -> Unit = {}
    ): OnConflict.Target.Constraint {
        val builder = OnConflictTargetConstraintBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returning(
        id: Int? = null,
        columns: MutableList<Returning.Column> = mutableListOf(),
        block: ReturningBuilder.() -> Unit = {}
    ): Returning {
        val builder = ReturningBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returningColumn(
        id: Int? = null,
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
        id: Int? = null,
        block: ReturningColumnValueWildcardBuilder.() -> Unit = {}
    ): Returning.Column.Value.Wildcard {
        val builder = ReturningColumnValueWildcardBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun returningColumnValueExpression(
        id: Int? = null,
        expr: Expr? = null,
        block: ReturningColumnValueExpressionBuilder.() -> Unit = {}
    ): Returning.Column.Value.Expression {
        val builder = ReturningColumnValueExpressionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinition(
        id: Int? = null,
        columns: MutableList<TableDefinition.Column> = mutableListOf(),
        block: TableDefinitionBuilder.() -> Unit = {}
    ): TableDefinition {
        val builder = TableDefinitionBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumn(
        id: Int? = null,
        name: String? = null,
        type: Type? = null,
        constraints: MutableList<TableDefinition.Column.Constraint> = mutableListOf(),
        block: TableDefinitionColumnBuilder.() -> Unit = {}
    ): TableDefinition.Column {
        val builder = TableDefinitionColumnBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraint(
        id: Int? = null,
        name: String? = null,
        body: TableDefinition.Column.Constraint.Body? = null,
        block: TableDefinitionColumnConstraintBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint {
        val builder = TableDefinitionColumnConstraintBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraintBodyNullable(
        id: Int? = null,
        block: TableDefinitionColumnConstraintBodyNullableBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint.Body.Nullable {
        val builder = TableDefinitionColumnConstraintBodyNullableBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraintBodyNotNull(
        id: Int? = null,
        block: TableDefinitionColumnConstraintBodyNotNullBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint.Body.NotNull {
        val builder = TableDefinitionColumnConstraintBodyNotNullBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun tableDefinitionColumnConstraintBodyCheck(
        id: Int? = null,
        expr: Expr? = null,
        block: TableDefinitionColumnConstraintBodyCheckBuilder.() -> Unit = {}
    ): TableDefinition.Column.Constraint.Body.Check {
        val builder = TableDefinitionColumnConstraintBodyCheckBuilder()
        builder.block()
        return builder.build(factory)
    }
}

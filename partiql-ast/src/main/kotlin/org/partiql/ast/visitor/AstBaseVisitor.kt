package org.partiql.ast.visitor

import org.partiql.ast.AstNode
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
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.Type
import org.partiql.ast.Union

public abstract class AstBaseVisitor<R, C> : AstVisitor<R, C> {
    public override fun visit(node: AstNode, ctx: C): R = node.accept(this, ctx)

    public override fun visitStatement(node: Statement, ctx: C): R = when (node) {
        is Statement.Query -> visitStatementQuery(node, ctx)
        is Statement.DML -> visitStatementDML(node, ctx)
        is Statement.DDL -> visitStatementDDL(node, ctx)
        is Statement.Exec -> visitStatementExec(node, ctx)
        is Statement.Explain -> visitStatementExplain(node, ctx)
    }

    public override fun visitStatementQuery(node: Statement.Query, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitStatementDML(node: Statement.DML, ctx: C): R = when (node) {
        is Statement.DML.Insert -> visitStatementDMLInsert(node, ctx)
        is Statement.DML.InsertValue -> visitStatementDMLInsertValue(node, ctx)
        is Statement.DML.Upsert -> visitStatementDMLUpsert(node, ctx)
        is Statement.DML.Replace -> visitStatementDMLReplace(node, ctx)
        is Statement.DML.Update -> visitStatementDMLUpdate(node, ctx)
        is Statement.DML.Remove -> visitStatementDMLRemove(node, ctx)
        is Statement.DML.Delete -> visitStatementDMLDelete(node, ctx)
        is Statement.DML.Batch -> visitStatementDMLBatch(node, ctx)
    }

    public override fun visitStatementDMLInsert(node: Statement.DML.Insert, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLInsertValue(node: Statement.DML.InsertValue, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLUpsert(node: Statement.DML.Upsert, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLReplace(node: Statement.DML.Replace, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLUpdate(node: Statement.DML.Update, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLUpdateAssignment(
        node: Statement.DML.Update.Assignment,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitStatementDMLRemove(node: Statement.DML.Remove, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLDelete(node: Statement.DML.Delete, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLBatch(node: Statement.DML.Batch, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLBatchOp(node: Statement.DML.Batch.Op, ctx: C): R = when
    (node) {
        is Statement.DML.Batch.Op.Insert -> visitStatementDMLBatchOpInsert(node, ctx)
        is Statement.DML.Batch.Op.InsertValue -> visitStatementDMLBatchOpInsertValue(node, ctx)
        is Statement.DML.Batch.Op.Set -> visitStatementDMLBatchOpSet(node, ctx)
        is Statement.DML.Batch.Op.Remove -> visitStatementDMLBatchOpRemove(node, ctx)
        is Statement.DML.Batch.Op.Delete -> visitStatementDMLBatchOpDelete(node, ctx)
    }

    public override fun visitStatementDMLBatchOpInsert(node: Statement.DML.Batch.Op.Insert, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLBatchOpInsertValue(
        node: Statement.DML.Batch.Op.InsertValue,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitStatementDMLBatchOpSet(node: Statement.DML.Batch.Op.Set, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLBatchOpRemove(node: Statement.DML.Batch.Op.Remove, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLBatchOpDelete(node: Statement.DML.Batch.Op.Delete, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDMLTarget(node: Statement.DML.Target, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDDL(node: Statement.DDL, ctx: C): R = when (node) {
        is Statement.DDL.CreateTable -> visitStatementDDLCreateTable(node, ctx)
        is Statement.DDL.CreateIndex -> visitStatementDDLCreateIndex(node, ctx)
        is Statement.DDL.DropTable -> visitStatementDDLDropTable(node, ctx)
        is Statement.DDL.DropIndex -> visitStatementDDLDropIndex(node, ctx)
    }

    public override fun visitStatementDDLCreateTable(node: Statement.DDL.CreateTable, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDDLCreateIndex(node: Statement.DDL.CreateIndex, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDDLDropTable(node: Statement.DDL.DropTable, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementDDLDropIndex(node: Statement.DDL.DropIndex, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitStatementExec(node: Statement.Exec, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitStatementExplain(node: Statement.Explain, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitStatementExplainTarget(node: Statement.Explain.Target, ctx: C): R = when
    (node) {
        is Statement.Explain.Target.Domain -> visitStatementExplainTargetDomain(node, ctx)
    }

    public override fun visitStatementExplainTargetDomain(
        node: Statement.Explain.Target.Domain,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitType(node: Type, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExpr(node: Expr, ctx: C): R = when (node) {
        is Expr.Missing -> visitExprMissing(node, ctx)
        is Expr.Lit -> visitExprLit(node, ctx)
        is Expr.Identifier -> visitExprIdentifier(node, ctx)
        is Expr.Path -> visitExprPath(node, ctx)
        is Expr.Call -> visitExprCall(node, ctx)
        is Expr.Agg -> visitExprAgg(node, ctx)
        is Expr.Parameter -> visitExprParameter(node, ctx)
        is Expr.Unary -> visitExprUnary(node, ctx)
        is Expr.Binary -> visitExprBinary(node, ctx)
        is Expr.Collection -> visitExprCollection(node, ctx)
        is Expr.Tuple -> visitExprTuple(node, ctx)
        is Expr.Date -> visitExprDate(node, ctx)
        is Expr.Time -> visitExprTime(node, ctx)
        is Expr.Like -> visitExprLike(node, ctx)
        is Expr.Between -> visitExprBetween(node, ctx)
        is Expr.InCollection -> visitExprInCollection(node, ctx)
        is Expr.IsType -> visitExprIsType(node, ctx)
        is Expr.Switch -> visitExprSwitch(node, ctx)
        is Expr.Coalesce -> visitExprCoalesce(node, ctx)
        is Expr.NullIf -> visitExprNullIf(node, ctx)
        is Expr.Cast -> visitExprCast(node, ctx)
        is Expr.CanCast -> visitExprCanCast(node, ctx)
        is Expr.CanLosslessCast -> visitExprCanLosslessCast(node, ctx)
        is Expr.Set -> visitExprSet(node, ctx)
        is Expr.SFW -> visitExprSFW(node, ctx)
        is Expr.Match -> visitExprMatch(node, ctx)
        is Expr.Window -> visitExprWindow(node, ctx)
    }

    public override fun visitExprMissing(node: Expr.Missing, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprLit(node: Expr.Lit, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprIdentifier(node: Expr.Identifier, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitExprPath(node: Expr.Path, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprPathStep(node: Expr.Path.Step, ctx: C): R = when (node) {
        is Expr.Path.Step.Index -> visitExprPathStepIndex(node, ctx)
        is Expr.Path.Step.Wildcard -> visitExprPathStepWildcard(node, ctx)
        is Expr.Path.Step.Unpivot -> visitExprPathStepUnpivot(node, ctx)
    }

    public override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitExprCall(node: Expr.Call, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprAgg(node: Expr.Agg, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprParameter(node: Expr.Parameter, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprUnary(node: Expr.Unary, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprBinary(node: Expr.Binary, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprCollection(node: Expr.Collection, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitExprTuple(node: Expr.Tuple, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprTupleField(node: Expr.Tuple.Field, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitExprDate(node: Expr.Date, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprTime(node: Expr.Time, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprLike(node: Expr.Like, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprBetween(node: Expr.Between, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprInCollection(node: Expr.InCollection, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitExprIsType(node: Expr.IsType, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprSwitch(node: Expr.Switch, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprSwitchBranch(node: Expr.Switch.Branch, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitExprCoalesce(node: Expr.Coalesce, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprNullIf(node: Expr.NullIf, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprCast(node: Expr.Cast, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprCanCast(node: Expr.CanCast, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitExprSet(node: Expr.Set, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprSFW(node: Expr.SFW, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprMatch(node: Expr.Match, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprWindow(node: Expr.Window, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitSelect(node: Select, ctx: C): R = when (node) {
        is Select.Star -> visitSelectStar(node, ctx)
        is Select.Project -> visitSelectProject(node, ctx)
        is Select.Pivot -> visitSelectPivot(node, ctx)
        is Select.Value -> visitSelectValue(node, ctx)
    }

    public override fun visitSelectStar(node: Select.Star, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitSelectProject(node: Select.Project, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitSelectProjectItem(node: Select.Project.Item, ctx: C): R = when (node) {
        is Select.Project.Item.All -> visitSelectProjectItemAll(node, ctx)
        is Select.Project.Item.Var -> visitSelectProjectItemVar(node, ctx)
    }

    public override fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitSelectProjectItemVar(node: Select.Project.Item.Var, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitSelectPivot(node: Select.Pivot, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitSelectValue(node: Select.Value, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitFrom(node: From, ctx: C): R = when (node) {
        is From.Collection -> visitFromCollection(node, ctx)
        is From.Join -> visitFromJoin(node, ctx)
    }

    public override fun visitFromCollection(node: From.Collection, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitFromJoin(node: From.Join, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitLet(node: Let, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitLetBinding(node: Let.Binding, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitGroupBy(node: GroupBy, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitGroupByKey(node: GroupBy.Key, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitOrderBy(node: OrderBy, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitOrderBySort(node: OrderBy.Sort, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitUnion(node: Union, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitIntersect(node: Intersect, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExcept(node: Except, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitGraphMatch(node: GraphMatch, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitGraphMatchPattern(node: GraphMatch.Pattern, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitGraphMatchPatternPart(node: GraphMatch.Pattern.Part, ctx: C): R = when
    (node) {
        is GraphMatch.Pattern.Part.Node -> visitGraphMatchPatternPartNode(node, ctx)
        is GraphMatch.Pattern.Part.Edge -> visitGraphMatchPatternPartEdge(node, ctx)
    }

    public override fun visitGraphMatchPatternPartNode(node: GraphMatch.Pattern.Part.Node, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitGraphMatchPatternPartEdge(node: GraphMatch.Pattern.Part.Edge, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitGraphMatchQuantifier(node: GraphMatch.Quantifier, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitGraphMatchSelector(node: GraphMatch.Selector, ctx: C): R = when (node) {
        is GraphMatch.Selector.AnyShortest -> visitGraphMatchSelectorAnyShortest(node, ctx)
        is GraphMatch.Selector.AllShortest -> visitGraphMatchSelectorAllShortest(node, ctx)
        is GraphMatch.Selector.Any -> visitGraphMatchSelectorAny(node, ctx)
        is GraphMatch.Selector.AnyK -> visitGraphMatchSelectorAnyK(node, ctx)
        is GraphMatch.Selector.ShortestK -> visitGraphMatchSelectorShortestK(node, ctx)
        is GraphMatch.Selector.ShortestKGroup -> visitGraphMatchSelectorShortestKGroup(node, ctx)
    }

    public override fun visitGraphMatchSelectorAnyShortest(
        node: GraphMatch.Selector.AnyShortest,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitGraphMatchSelectorAllShortest(
        node: GraphMatch.Selector.AllShortest,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitGraphMatchSelectorAny(node: GraphMatch.Selector.Any, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitGraphMatchSelectorAnyK(node: GraphMatch.Selector.AnyK, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitGraphMatchSelectorShortestK(node: GraphMatch.Selector.ShortestK, ctx: C):
        R = defaultVisit(node, ctx)

    public override
    fun visitGraphMatchSelectorShortestKGroup(node: GraphMatch.Selector.ShortestKGroup, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitOver(node: Over, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitOnConflict(node: OnConflict, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitOnConflictAction(node: OnConflict.Action, ctx: C): R = when (node) {
        is OnConflict.Action.DoReplace -> visitOnConflictActionDoReplace(node, ctx)
        is OnConflict.Action.DoUpdate -> visitOnConflictActionDoUpdate(node, ctx)
        is OnConflict.Action.DoNothing -> visitOnConflictActionDoNothing(node, ctx)
    }

    public override fun visitOnConflictActionDoReplace(node: OnConflict.Action.DoReplace, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitOnConflictActionDoUpdate(node: OnConflict.Action.DoUpdate, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitOnConflictActionDoNothing(node: OnConflict.Action.DoNothing, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitOnConflictTarget(node: OnConflict.Target, ctx: C): R = when (node) {
        is OnConflict.Target.Condition -> visitOnConflictTargetCondition(node, ctx)
        is OnConflict.Target.Symbols -> visitOnConflictTargetSymbols(node, ctx)
        is OnConflict.Target.Constraint -> visitOnConflictTargetConstraint(node, ctx)
    }

    public override fun visitOnConflictTargetCondition(node: OnConflict.Target.Condition, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitOnConflictTargetSymbols(node: OnConflict.Target.Symbols, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitOnConflictTargetConstraint(node: OnConflict.Target.Constraint, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitReturning(node: Returning, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitReturningColumn(node: Returning.Column, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitReturningColumnValue(node: Returning.Column.Value, ctx: C): R = when
    (node) {
        is Returning.Column.Value.Wildcard -> visitReturningColumnValueWildcard(node, ctx)
        is Returning.Column.Value.Expression -> visitReturningColumnValueExpression(node, ctx)
    }

    public override fun visitReturningColumnValueWildcard(
        node: Returning.Column.Value.Wildcard,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitReturningColumnValueExpression(
        node: Returning.Column.Value.Expression,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override fun visitTableDefinition(node: TableDefinition, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitTableDefinitionColumn(node: TableDefinition.Column, ctx: C): R =
        defaultVisit(node, ctx)

    public override fun visitTableDefinitionColumnConstraint(
        node: TableDefinition.Column.Constraint,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override
    fun visitTableDefinitionColumnConstraintBody(
        node: TableDefinition.Column.Constraint.Body,
        ctx: C
    ): R = when (node) {
        is TableDefinition.Column.Constraint.Body.Nullable ->
            visitTableDefinitionColumnConstraintBodyNullable(node, ctx)
        is TableDefinition.Column.Constraint.Body.NotNull ->
            visitTableDefinitionColumnConstraintBodyNotNull(node, ctx)
        is TableDefinition.Column.Constraint.Body.Check ->
            visitTableDefinitionColumnConstraintBodyCheck(node, ctx)
    }

    public override
    fun visitTableDefinitionColumnConstraintBodyNullable(
        node: TableDefinition.Column.Constraint.Body.Nullable,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override
    fun visitTableDefinitionColumnConstraintBodyNotNull(
        node: TableDefinition.Column.Constraint.Body.NotNull,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public override
    fun visitTableDefinitionColumnConstraintBodyCheck(
        node: TableDefinition.Column.Constraint.Body.Check,
        ctx: C
    ): R = defaultVisit(node, ctx)

    public open fun defaultVisit(node: AstNode, ctx: C): R {
        for (child in node.children) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    public abstract fun defaultReturn(node: AstNode, ctx: C): R
}

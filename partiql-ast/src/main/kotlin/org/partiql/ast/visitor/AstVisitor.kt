package org.partiql.ast.visitor

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
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition

public interface AstVisitor<R, C> {
    public fun visit(node: AstNode, ctx: C): R

    public fun visitStatement(node: Statement, ctx: C): R

    public fun visitStatementQuery(node: Statement.Query, ctx: C): R

    public fun visitStatementDML(node: Statement.DML, ctx: C): R

    public fun visitStatementDMLInsert(node: Statement.DML.Insert, ctx: C): R

    public fun visitStatementDMLInsertValue(node: Statement.DML.InsertValue, ctx: C): R

    public fun visitStatementDMLSet(node: Statement.DML.Set, ctx: C): R

    public fun visitStatementDMLSetAssignment(node: Statement.DML.Set.Assignment, ctx: C): R

    public fun visitStatementDMLRemove(node: Statement.DML.Remove, ctx: C): R

    public fun visitStatementDMLDelete(node: Statement.DML.Delete, ctx: C): R

    public fun visitStatementDDL(node: Statement.DDL, ctx: C): R

    public fun visitStatementDDLCreateTable(node: Statement.DDL.CreateTable, ctx: C): R

    public fun visitStatementDDLCreateIndex(node: Statement.DDL.CreateIndex, ctx: C): R

    public fun visitStatementDDLDropTable(node: Statement.DDL.DropTable, ctx: C): R

    public fun visitStatementDDLDropIndex(node: Statement.DDL.DropIndex, ctx: C): R

    public fun visitStatementExec(node: Statement.Exec, ctx: C): R

    public fun visitStatementExplain(node: Statement.Explain, ctx: C): R

    public fun visitStatementExplainTarget(node: Statement.Explain.Target, ctx: C): R

    public fun visitStatementExplainTargetDomain(node: Statement.Explain.Target.Domain, ctx: C): R

    public fun visitExpr(node: Expr, ctx: C): R

    public fun visitExprMissing(node: Expr.Missing, ctx: C): R

    public fun visitExprLit(node: Expr.Lit, ctx: C): R

    public fun visitExprIdentifier(node: Expr.Identifier, ctx: C): R

    public fun visitExprPath(node: Expr.Path, ctx: C): R

    public fun visitExprPathStep(node: Expr.Path.Step, ctx: C): R

    public fun visitExprPathStepKey(node: Expr.Path.Step.Key, ctx: C): R

    public fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, ctx: C): R

    public fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, ctx: C): R

    public fun visitExprCall(node: Expr.Call, ctx: C): R

    public fun visitExprAgg(node: Expr.Agg, ctx: C): R

    public fun visitExprParameter(node: Expr.Parameter, ctx: C): R

    public fun visitExprUnary(node: Expr.Unary, ctx: C): R

    public fun visitExprBinary(node: Expr.Binary, ctx: C): R

    public fun visitExprCollection(node: Expr.Collection, ctx: C): R

    public fun visitExprTuple(node: Expr.Tuple, ctx: C): R

    public fun visitExprTupleField(node: Expr.Tuple.Field, ctx: C): R

    public fun visitExprDate(node: Expr.Date, ctx: C): R

    public fun visitExprTime(node: Expr.Time, ctx: C): R

    public fun visitExprLike(node: Expr.Like, ctx: C): R

    public fun visitExprBetween(node: Expr.Between, ctx: C): R

    public fun visitExprInCollection(node: Expr.InCollection, ctx: C): R

    public fun visitExprIsType(node: Expr.IsType, ctx: C): R

    public fun visitExprSwitch(node: Expr.Switch, ctx: C): R

    public fun visitExprSwitchBranch(node: Expr.Switch.Branch, ctx: C): R

    public fun visitExprCoalesce(node: Expr.Coalesce, ctx: C): R

    public fun visitExprNullIf(node: Expr.NullIf, ctx: C): R

    public fun visitExprCast(node: Expr.Cast, ctx: C): R

    public fun visitExprCanCast(node: Expr.CanCast, ctx: C): R

    public fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: C): R

    public fun visitExprOuterBagOp(node: Expr.OuterBagOp, ctx: C): R

    public fun visitExprSFW(node: Expr.SFW, ctx: C): R

    public fun visitExprMatch(node: Expr.Match, ctx: C): R

    public fun visitExprWindow(node: Expr.Window, ctx: C): R

    public fun visitSelect(node: Select, ctx: C): R

    public fun visitSelectStar(node: Select.Star, ctx: C): R

    public fun visitSelectProject(node: Select.Project, ctx: C): R

    public fun visitSelectProjectItem(node: Select.Project.Item, ctx: C): R

    public fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: C): R

    public fun visitSelectProjectItemVar(node: Select.Project.Item.Var, ctx: C): R

    public fun visitSelectPivot(node: Select.Pivot, ctx: C): R

    public fun visitSelectValue(node: Select.Value, ctx: C): R

    public fun visitFrom(node: From, ctx: C): R

    public fun visitFromCollection(node: From.Collection, ctx: C): R

    public fun visitFromJoin(node: From.Join, ctx: C): R

    public fun visitLet(node: Let, ctx: C): R

    public fun visitLetBinding(node: Let.Binding, ctx: C): R

    public fun visitGroupBy(node: GroupBy, ctx: C): R

    public fun visitGroupByKey(node: GroupBy.Key, ctx: C): R

    public fun visitOrderBy(node: OrderBy, ctx: C): R

    public fun visitOrderBySort(node: OrderBy.Sort, ctx: C): R

    public fun visitGraphMatch(node: GraphMatch, ctx: C): R

    public fun visitGraphMatchPattern(node: GraphMatch.Pattern, ctx: C): R

    public fun visitGraphMatchPatternPart(node: GraphMatch.Pattern.Part, ctx: C): R

    public fun visitGraphMatchPatternPartNode(node: GraphMatch.Pattern.Part.Node, ctx: C): R

    public fun visitGraphMatchPatternPartEdge(node: GraphMatch.Pattern.Part.Edge, ctx: C): R

    public fun visitGraphMatchQuantifier(node: GraphMatch.Quantifier, ctx: C): R

    public fun visitGraphMatchSelector(node: GraphMatch.Selector, ctx: C): R

    public fun visitGraphMatchSelectorAnyShortest(node: GraphMatch.Selector.AnyShortest, ctx: C): R

    public fun visitGraphMatchSelectorAllShortest(node: GraphMatch.Selector.AllShortest, ctx: C): R

    public fun visitGraphMatchSelectorAny(node: GraphMatch.Selector.Any, ctx: C): R

    public fun visitGraphMatchSelectorAnyK(node: GraphMatch.Selector.AnyK, ctx: C): R

    public fun visitGraphMatchSelectorShortestK(node: GraphMatch.Selector.ShortestK, ctx: C): R

    public fun visitGraphMatchSelectorShortestKGroup(
        node: GraphMatch.Selector.ShortestKGroup,
        ctx: C
    ): R

    public fun visitOver(node: Over, ctx: C): R

    public fun visitOnConflict(node: OnConflict, ctx: C): R

    public fun visitOnConflictAction(node: OnConflict.Action, ctx: C): R

    public fun visitOnConflictActionDoReplace(node: OnConflict.Action.DoReplace, ctx: C): R

    public fun visitOnConflictActionDoUpdate(node: OnConflict.Action.DoUpdate, ctx: C): R

    public fun visitOnConflictActionDoNothing(node: OnConflict.Action.DoNothing, ctx: C): R

    public fun visitReturning(node: Returning, ctx: C): R

    public fun visitReturningColumn(node: Returning.Column, ctx: C): R

    public fun visitReturningColumnValue(node: Returning.Column.Value, ctx: C): R

    public fun visitReturningColumnValueWildcard(node: Returning.Column.Value.Wildcard, ctx: C): R

    public fun visitReturningColumnValueExpression(node: Returning.Column.Value.Expression, ctx: C): R

    public fun visitTableDefinition(node: TableDefinition, ctx: C): R

    public fun visitTableDefinitionColumn(node: TableDefinition.Column, ctx: C): R

    public fun visitTableDefinitionColumnConstraint(node: TableDefinition.Column.Constraint, ctx: C):
        R

    public fun visitTableDefinitionColumnConstraintBody(
        node: TableDefinition.Column.Constraint.Body,
        ctx: C
    ): R

    public
    fun visitTableDefinitionColumnConstraintBodyNullable(
        node: TableDefinition.Column.Constraint.Body.Nullable,
        ctx: C
    ): R

    public
    fun visitTableDefinitionColumnConstraintBodyNotNull(
        node: TableDefinition.Column.Constraint.Body.NotNull,
        ctx: C
    ): R

    public
    fun visitTableDefinitionColumnConstraintBodyCheck(
        node: TableDefinition.Column.Constraint.Body.Check,
        ctx: C
    ): R
}

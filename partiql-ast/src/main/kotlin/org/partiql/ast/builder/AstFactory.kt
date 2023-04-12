package org.partiql.ast.builder

import com.amazon.ionelement.api.IonElement
import org.partiql.ast.AstNode
import org.partiql.ast.Case
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
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmStatic

public abstract class AstFactory {
    public open fun statementQuery(expr: Expr) = Statement.Query(expr)

    public open fun statementDmlInsert(
        target: Expr,
        values: Expr,
        onConflict: OnConflict.Action
    ) = Statement.Dml.Insert(target, values, onConflict)

    public open fun statementDmlInsertValue(
        target: Expr,
        `value`: Expr,
        atAlias: Expr,
        index: Expr?,
        onConflict: OnConflict
    ) = Statement.Dml.InsertValue(target, value, atAlias, index, onConflict)

    public open fun statementDmlSet(assignments: List<Statement.Dml.Set.Assignment>) =
        Statement.Dml.Set(assignments)

    public open fun statementDmlSetAssignment(target: Expr.Path, `value`: Expr) =
        Statement.Dml.Set.Assignment(target, value)

    public open fun statementDmlRemove(target: Expr.Path) = Statement.Dml.Remove(target)

    public open fun statementDmlDelete(
        from: From,
        `where`: Expr?,
        returning: Returning
    ) = Statement.Dml.Delete(from, where, returning)

    public open fun statementDdlCreateTable(name: String, definition: TableDefinition?) =
        Statement.Ddl.CreateTable(name, definition)

    public open fun statementDdlCreateIndex(name: String, fields: List<Expr>) =
        Statement.Ddl.CreateIndex(name, fields)

    public open fun statementDdlDropTable(identifier: Expr.Identifier) =
        Statement.Ddl.DropTable(identifier)

    public open fun statementDdlDropIndex(table: Expr.Identifier, keys: Expr.Identifier) =
        Statement.Ddl.DropIndex(table, keys)

    public open fun statementExec(procedure: String, args: List<Expr>) = Statement.Exec(
        procedure,
        args
    )

    public open fun statementExplain(target: Statement.Explain.Target) = Statement.Explain(target)

    public open fun statementExplainTargetDomain(
        statement: Statement,
        type: String?,
        format: String?
    ) = Statement.Explain.Target.Domain(statement, type, format)

    public open fun exprMissing() = Expr.Missing()

    public open fun exprLit(`value`: IonElement) = Expr.Lit(value)

    public open fun exprIdentifier(
        name: String,
        case: Case,
        scope: Expr.Identifier.Scope
    ) = Expr.Identifier(name, case, scope)

    public open fun exprPath(root: Expr, steps: List<Expr.Path.Step>) = Expr.Path(root, steps)

    public open fun exprPathStepKey(`value`: Expr) = Expr.Path.Step.Key(value)

    public open fun exprPathStepWildcard() = Expr.Path.Step.Wildcard()

    public open fun exprPathStepUnpivot() = Expr.Path.Step.Unpivot()

    public open fun exprCall(function: String, args: List<Expr>) = Expr.Call(function, args)

    public open fun exprAgg(
        function: String,
        args: List<Expr>,
        quantifier: SetQuantifier
    ) = Expr.Agg(function, args, quantifier)

    public open fun exprParameter(index: Int) = Expr.Parameter(index)

    public open fun exprUnary(op: Expr.Unary.Op, expr: Expr) = Expr.Unary(op, expr)

    public open fun exprBinary(
        op: Expr.Binary.Op,
        lhs: Expr,
        rhs: Expr
    ) = Expr.Binary(op, lhs, rhs)

    public open fun exprCollection(type: Expr.Collection.Type, values: List<Expr>) =
        Expr.Collection(type, values)

    public open fun exprTuple(fields: List<Expr.Tuple.Field>) = Expr.Tuple(fields)

    public open fun exprTupleField(name: Expr, `value`: Expr) = Expr.Tuple.Field(name, value)

    public open fun exprDate(
        year: Long,
        month: Long,
        day: Long
    ) = Expr.Date(year, month, day)

    public open fun exprTime(
        hour: Long,
        minute: Long,
        second: Long,
        nano: Long,
        precision: Long,
        tzOffsetMinutes: Long?
    ) = Expr.Time(hour, minute, second, nano, precision, tzOffsetMinutes)

    public open fun exprLike(
        `value`: Expr,
        pattern: Expr,
        escape: Expr?
    ) = Expr.Like(value, pattern, escape)

    public open fun exprBetween(
        `value`: Expr,
        from: Expr,
        to: Expr
    ) = Expr.Between(value, from, to)

    public open fun exprInCollection(lhs: Expr, rhs: Expr) = Expr.InCollection(lhs, rhs)

    public open fun exprIsType(`value`: Expr, type: Expr.Collection.Type) = Expr.IsType(value, type)

    public open fun exprSwitch(
        expr: Expr?,
        branches: List<Expr.Switch.Branch>,
        default: Expr?
    ) = Expr.Switch(expr, branches, default)

    public open fun exprSwitchBranch(condition: Expr, expr: Expr) = Expr.Switch.Branch(
        condition,
        expr
    )

    public open fun exprCoalesce(args: List<Expr>) = Expr.Coalesce(args)

    public open fun exprNullIf(expr1: Expr, expr2: Expr) = Expr.NullIf(expr1, expr2)

    public open fun exprCast(`value`: Expr, asType: Expr.Collection.Type) = Expr.Cast(value, asType)

    public open fun exprCanCast(`value`: Expr, asType: Expr.Collection.Type) = Expr.CanCast(
        value,
        asType
    )

    public open fun exprCanLosslessCast(`value`: Expr, asType: Expr.Collection.Type) =
        Expr.CanLosslessCast(value, asType)

    public open fun exprOuterBagOp(
        op: Expr.OuterBagOp.Op,
        quantifier: SetQuantifier,
        lhs: Expr,
        rhs: Expr
    ) = Expr.OuterBagOp(op, quantifier, lhs, rhs)

    public open fun exprSfw(
        select: Select,
        from: From,
        let: Let?,
        `where`: Expr?,
        groupBy: GroupBy?,
        having: Expr?,
        orderBy: OrderBy?,
        limit: Expr?,
        offset: Expr?
    ) = Expr.Sfw(select, from, let, where, groupBy, having, orderBy, limit, offset)

    public open fun exprMatch(expr: Expr, pattern: GraphMatch) = Expr.Match(expr, pattern)

    public open fun exprWindow(
        function: String,
        over: Over,
        args: List<Expr>
    ) = Expr.Window(function, over, args)

    public open fun selectStar() = Select.Star()

    public open fun selectProject(items: List<Select.Project.Item>) = Select.Project(items)

    public open fun selectProjectItemAll(expr: Expr) = Select.Project.Item.All(expr)

    public open fun selectProjectItemVar(expr: Expr, asAlias: String?) = Select.Project.Item.Var(
        expr,
        asAlias
    )

    public open fun selectPivot(`value`: Expr, key: Expr) = Select.Pivot(value, key)

    public open fun selectValue(`constructor`: Expr) = Select.Value(constructor)

    public open fun fromCollection(
        expr: Expr,
        unpivot: Boolean?,
        asAlias: String?,
        atAlias: String?,
        byAlias: String?
    ) = From.Collection(expr, unpivot, asAlias, atAlias, byAlias)

    public open fun fromJoin(
        type: From.Join.Type,
        condition: Expr?,
        lhs: From,
        rhs: From
    ) = From.Join(type, condition, lhs, rhs)

    public open fun let() = Let()

    public open fun groupBy(
        strategy: GroupBy.Strategy,
        keys: List<GroupBy.Key>,
        asAlias: String?
    ) = GroupBy(strategy, keys, asAlias)

    public open fun groupByKey(expr: Expr, asAlias: String) = GroupBy.Key(expr, asAlias)

    public open fun orderBy(sorts: List<OrderBy.Sort>) = OrderBy(sorts)

    public open fun orderBySort(
        expr: Expr,
        dir: OrderBy.Sort.Dir,
        nulls: OrderBy.Sort.Nulls
    ) = OrderBy.Sort(expr, dir, nulls)

    public open fun graphMatch(patterns: List<GraphMatch.Pattern>, selector: GraphMatch.Selector?) =
        GraphMatch(patterns, selector)

    public open fun graphMatchPattern(
        restrictor: GraphMatch.Restrictor?,
        prefilter: Expr?,
        variable: String?,
        quantifier: GraphMatch.Quantifier?,
        parts: List<GraphMatch.Pattern.Part>
    ) = GraphMatch.Pattern(restrictor, prefilter, variable, quantifier, parts)

    public open fun graphMatchPatternPartNode(
        prefilter: Expr?,
        variable: String?,
        label: List<String>
    ) = GraphMatch.Pattern.Part.Node(prefilter, variable, label)

    public open fun graphMatchPatternPartEdge(
        direction: GraphMatch.Direction,
        quantifier: GraphMatch.Quantifier?,
        prefilter: Expr?,
        variable: String?,
        label: List<String>
    ) = GraphMatch.Pattern.Part.Edge(direction, quantifier, prefilter, variable, label)

    public open fun graphMatchQuantifier(lower: Long, upper: Int?) = GraphMatch.Quantifier(
        lower,
        upper
    )

    public open fun graphMatchSelectorAnyShortest() = GraphMatch.Selector.AnyShortest()

    public open fun graphMatchSelectorAllShortest() = GraphMatch.Selector.AllShortest()

    public open fun graphMatchSelectorAny() = GraphMatch.Selector.Any()

    public open fun graphMatchSelectorAnyK(k: Long) = GraphMatch.Selector.AnyK(k)

    public open fun graphMatchSelectorShortestK(k: Long) = GraphMatch.Selector.ShortestK(k)

    public open fun graphMatchSelectorShortestKGroup(k: Long) = GraphMatch.Selector.ShortestKGroup(k)

    public open fun over(partitions: List<Expr>, sorts: List<OrderBy.Sort>) = Over(partitions, sorts)

    public open fun onConflict(expr: Expr, action: OnConflict.Action) = OnConflict(expr, action)

    public open fun onConflictActionDoReplace(`value`: OnConflict.Value) =
        OnConflict.Action.DoReplace(value)

    public open fun onConflictActionDoUpdate(`value`: OnConflict.Value) =
        OnConflict.Action.DoUpdate(value)

    public open fun onConflictActionDoNothing() = OnConflict.Action.DoNothing()

    public open fun returning(columns: List<Returning.Column>) = Returning(columns)

    public open fun returningColumn(
        status: Returning.Column.Status,
        age: Returning.Column.Age,
        `value`: Returning.Column.Value
    ) = Returning.Column(status, age, value)

    public open fun returningColumnValueWildcard() = Returning.Column.Value.Wildcard()

    public open fun returningColumnValueExpression(expr: Expr) =
        Returning.Column.Value.Expression(expr)

    public open fun tableDefinition(columns: List<TableDefinition.Column>) = TableDefinition(columns)

    public open fun tableDefinitionColumn(
        name: String,
        type: Expr.Collection.Type,
        constraints: List<TableDefinition.Column.Constraint>
    ) = TableDefinition.Column(name, type, constraints)

    public open fun tableDefinitionColumnConstraintNullable() =
        TableDefinition.Column.Constraint.Nullable()

    public open fun tableDefinitionColumnConstraintNotNull() =
        TableDefinition.Column.Constraint.NotNull()

    public open fun tableDefinitionColumnConstraintCheck(expr: Expr) =
        TableDefinition.Column.Constraint.Check(expr)

    public companion object {
        public val DEFAULT: AstFactory = object : AstFactory() {}

        @JvmStatic
        public fun <T : AstNode> create(block: AstFactory.() -> T) = AstFactory.DEFAULT.block()
    }
}

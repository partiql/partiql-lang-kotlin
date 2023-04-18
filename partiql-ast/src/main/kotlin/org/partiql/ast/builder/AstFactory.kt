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
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmStatic

public abstract class AstFactory {
    public open fun statementQuery(id: Int, expr: Expr) = Statement.Query(id, expr)

    public open fun statementDMLInsert(
        id: Int,
        target: Expr,
        values: Expr,
        onConflict: OnConflict.Action
    ) = Statement.DML.Insert(id, target, values, onConflict)

    public open fun statementDMLInsertValue(
        id: Int,
        target: Expr,
        `value`: Expr,
        atAlias: Expr,
        index: Expr?,
        onConflict: OnConflict
    ) = Statement.DML.InsertValue(id, target, value, atAlias, index, onConflict)

    public open fun statementDMLSet(id: Int, assignments: List<Statement.DML.Set.Assignment>) =
        Statement.DML.Set(id, assignments)

    public open fun statementDMLSetAssignment(
        id: Int,
        target: Expr.Path,
        `value`: Expr
    ) = Statement.DML.Set.Assignment(id, target, value)

    public open fun statementDMLRemove(id: Int, target: Expr.Path) = Statement.DML.Remove(id, target)

    public open fun statementDMLDelete(
        id: Int,
        from: From,
        `where`: Expr?,
        returning: Returning
    ) = Statement.DML.Delete(id, from, where, returning)

    public open fun statementDDLCreateTable(
        id: Int,
        name: String,
        definition: TableDefinition?
    ) = Statement.DDL.CreateTable(id, name, definition)

    public open fun statementDDLCreateIndex(
        id: Int,
        name: String,
        fields: List<Expr>
    ) = Statement.DDL.CreateIndex(id, name, fields)

    public open fun statementDDLDropTable(id: Int, identifier: Expr.Identifier) =
        Statement.DDL.DropTable(id, identifier)

    public open fun statementDDLDropIndex(
        id: Int,
        table: Expr.Identifier,
        keys: Expr.Identifier
    ) = Statement.DDL.DropIndex(id, table, keys)

    public open fun statementExec(
        id: Int,
        procedure: String,
        args: List<Expr>
    ) = Statement.Exec(id, procedure, args)

    public open fun statementExplain(id: Int, target: Statement.Explain.Target) =
        Statement.Explain(id, target)

    public open fun statementExplainTargetDomain(
        id: Int,
        statement: Statement,
        type: String?,
        format: String?
    ) = Statement.Explain.Target.Domain(id, statement, type, format)

    public open fun type(
        id: Int,
        identifier: String,
        parameters: List<IonElement>
    ) = Type(id, identifier, parameters)

    public open fun exprMissing(id: Int) = Expr.Missing(id)

    public open fun exprLit(id: Int, `value`: IonElement) = Expr.Lit(id, value)

    public open fun exprIdentifier(
        id: Int,
        name: String,
        case: Case,
        scope: Expr.Identifier.Scope
    ) = Expr.Identifier(id, name, case, scope)

    public open fun exprPath(
        id: Int,
        root: Expr,
        steps: List<Expr.Path.Step>
    ) = Expr.Path(id, root, steps)

    public open fun exprPathStepIndex(
        id: Int,
        key: Expr,
        case: Case
    ) = Expr.Path.Step.Index(id, key, case)

    public open fun exprPathStepWildcard(id: Int) = Expr.Path.Step.Wildcard(id)

    public open fun exprPathStepUnpivot(id: Int) = Expr.Path.Step.Unpivot(id)

    public open fun exprCall(
        id: Int,
        function: String,
        args: List<Expr>
    ) = Expr.Call(id, function, args)

    public open fun exprAgg(
        id: Int,
        function: String,
        args: List<Expr>,
        quantifier: SetQuantifier
    ) = Expr.Agg(id, function, args, quantifier)

    public open fun exprParameter(id: Int, index: Int) = Expr.Parameter(id, index)

    public open fun exprUnary(
        id: Int,
        op: Expr.Unary.Op,
        expr: Expr
    ) = Expr.Unary(id, op, expr)

    public open fun exprBinary(
        id: Int,
        op: Expr.Binary.Op,
        lhs: Expr,
        rhs: Expr
    ) = Expr.Binary(id, op, lhs, rhs)

    public open fun exprCollection(
        id: Int,
        type: Expr.Collection.Type,
        values: List<Expr>
    ) = Expr.Collection(id, type, values)

    public open fun exprTuple(id: Int, fields: List<Expr.Tuple.Field>) = Expr.Tuple(id, fields)

    public open fun exprTupleField(
        id: Int,
        name: Expr,
        `value`: Expr
    ) = Expr.Tuple.Field(id, name, value)

    public open fun exprDate(
        id: Int,
        year: Long,
        month: Long,
        day: Long
    ) = Expr.Date(id, year, month, day)

    public open fun exprTime(
        id: Int,
        hour: Long,
        minute: Long,
        second: Long,
        nano: Long,
        precision: Long,
        tzOffsetMinutes: Long?
    ) = Expr.Time(id, hour, minute, second, nano, precision, tzOffsetMinutes)

    public open fun exprLike(
        id: Int,
        `value`: Expr,
        pattern: Expr,
        escape: Expr?
    ) = Expr.Like(id, value, pattern, escape)

    public open fun exprBetween(
        id: Int,
        `value`: Expr,
        from: Expr,
        to: Expr
    ) = Expr.Between(id, value, from, to)

    public open fun exprInCollection(
        id: Int,
        lhs: Expr,
        rhs: Expr
    ) = Expr.InCollection(id, lhs, rhs)

    public open fun exprIsType(
        id: Int,
        `value`: Expr,
        type: Type
    ) = Expr.IsType(id, value, type)

    public open fun exprSwitch(
        id: Int,
        expr: Expr?,
        branches: List<Expr.Switch.Branch>,
        default: Expr?
    ) = Expr.Switch(id, expr, branches, default)

    public open fun exprSwitchBranch(
        id: Int,
        condition: Expr,
        expr: Expr
    ) = Expr.Switch.Branch(id, condition, expr)

    public open fun exprCoalesce(id: Int, args: List<Expr>) = Expr.Coalesce(id, args)

    public open fun exprNullIf(
        id: Int,
        expr0: Expr,
        expr1: Expr
    ) = Expr.NullIf(id, expr0, expr1)

    public open fun exprCast(
        id: Int,
        `value`: Expr,
        asType: Type
    ) = Expr.Cast(id, value, asType)

    public open fun exprCanCast(
        id: Int,
        `value`: Expr,
        asType: Type
    ) = Expr.CanCast(id, value, asType)

    public open fun exprCanLosslessCast(
        id: Int,
        `value`: Expr,
        asType: Type
    ) = Expr.CanLosslessCast(id, value, asType)

    public open fun exprSet(
        id: Int,
        op: Expr.Set.Op,
        quantifier: SetQuantifier,
        outer: Boolean,
        lhs: Expr,
        rhs: Expr
    ) = Expr.Set(id, op, quantifier, outer, lhs, rhs)

    public open fun exprSFW(
        id: Int,
        select: Select,
        from: From,
        let: Let?,
        `where`: Expr?,
        groupBy: GroupBy?,
        having: Expr?,
        orderBy: OrderBy?,
        limit: Expr?,
        offset: Expr?
    ) = Expr.SFW(id, select, from, let, where, groupBy, having, orderBy, limit, offset)

    public open fun exprMatch(
        id: Int,
        expr: Expr,
        pattern: GraphMatch
    ) = Expr.Match(id, expr, pattern)

    public open fun exprWindow(
        id: Int,
        function: String,
        over: Over,
        args: List<Expr>
    ) = Expr.Window(id, function, over, args)

    public open fun selectStar(id: Int, quantifier: SetQuantifier) = Select.Star(id, quantifier)

    public open fun selectProject(
        id: Int,
        quantifier: SetQuantifier,
        items: List<Select.Project.Item>
    ) = Select.Project(id, quantifier, items)

    public open fun selectProjectItemAll(id: Int, expr: Expr) = Select.Project.Item.All(id, expr)

    public open fun selectProjectItemVar(
        id: Int,
        expr: Expr,
        asAlias: String?
    ) = Select.Project.Item.Var(id, expr, asAlias)

    public open fun selectPivot(
        id: Int,
        `value`: Expr,
        key: Expr
    ) = Select.Pivot(id, value, key)

    public open fun selectValue(
        id: Int,
        quantifier: SetQuantifier,
        `constructor`: Expr
    ) = Select.Value(id, quantifier, constructor)

    public open fun fromCollection(
        id: Int,
        expr: Expr,
        unpivot: Boolean?,
        asAlias: String?,
        atAlias: String?,
        byAlias: String?
    ) = From.Collection(id, expr, unpivot, asAlias, atAlias, byAlias)

    public open fun fromJoin(
        id: Int,
        type: From.Join.Type,
        condition: Expr?,
        lhs: From,
        rhs: From
    ) = From.Join(id, type, condition, lhs, rhs)

    public open fun let(id: Int, bindings: List<Let.Binding>) = Let(id, bindings)

    public open fun letBinding(
        id: Int,
        expr: Expr,
        asAlias: String
    ) = Let.Binding(id, expr, asAlias)

    public open fun groupBy(
        id: Int,
        strategy: GroupBy.Strategy,
        keys: List<GroupBy.Key>,
        asAlias: String?
    ) = GroupBy(id, strategy, keys, asAlias)

    public open fun groupByKey(
        id: Int,
        expr: Expr,
        asAlias: String?
    ) = GroupBy.Key(id, expr, asAlias)

    public open fun orderBy(id: Int, sorts: List<OrderBy.Sort>) = OrderBy(id, sorts)

    public open fun orderBySort(
        id: Int,
        expr: Expr,
        dir: OrderBy.Sort.Dir,
        nulls: OrderBy.Sort.Nulls
    ) = OrderBy.Sort(id, expr, dir, nulls)

    public open fun union(
        id: Int,
        quantifier: SetQuantifier,
        lhs: Expr.SFW,
        rhs: Expr.SFW
    ) = Union(id, quantifier, lhs, rhs)

    public open fun intersect(
        id: Int,
        quantifier: SetQuantifier,
        lhs: Expr.SFW,
        rhs: Expr.SFW
    ) = Intersect(id, quantifier, lhs, rhs)

    public open fun except(
        id: Int,
        quantifier: SetQuantifier,
        lhs: Expr.SFW,
        rhs: Expr.SFW
    ) = Except(id, quantifier, lhs, rhs)

    public open fun graphMatch(
        id: Int,
        patterns: List<GraphMatch.Pattern>,
        selector: GraphMatch.Selector?
    ) = GraphMatch(id, patterns, selector)

    public open fun graphMatchPattern(
        id: Int,
        restrictor: GraphMatch.Restrictor?,
        prefilter: Expr?,
        variable: String?,
        quantifier: GraphMatch.Quantifier?,
        parts: List<GraphMatch.Pattern.Part>
    ) = GraphMatch.Pattern(id, restrictor, prefilter, variable, quantifier, parts)

    public open fun graphMatchPatternPartNode(
        id: Int,
        prefilter: Expr?,
        variable: String?,
        label: List<String>
    ) = GraphMatch.Pattern.Part.Node(id, prefilter, variable, label)

    public open fun graphMatchPatternPartEdge(
        id: Int,
        direction: GraphMatch.Direction,
        quantifier: GraphMatch.Quantifier?,
        prefilter: Expr?,
        variable: String?,
        label: List<String>
    ) = GraphMatch.Pattern.Part.Edge(id, direction, quantifier, prefilter, variable, label)

    public open fun graphMatchQuantifier(
        id: Int,
        lower: Long,
        upper: Long?
    ) = GraphMatch.Quantifier(id, lower, upper)

    public open fun graphMatchSelectorAnyShortest(id: Int) = GraphMatch.Selector.AnyShortest(id)

    public open fun graphMatchSelectorAllShortest(id: Int) = GraphMatch.Selector.AllShortest(id)

    public open fun graphMatchSelectorAny(id: Int) = GraphMatch.Selector.Any(id)

    public open fun graphMatchSelectorAnyK(id: Int, k: Long) = GraphMatch.Selector.AnyK(id, k)

    public open fun graphMatchSelectorShortestK(id: Int, k: Long) = GraphMatch.Selector.ShortestK(
        id,
        k
    )

    public open fun graphMatchSelectorShortestKGroup(id: Int, k: Long) =
        GraphMatch.Selector.ShortestKGroup(id, k)

    public open fun over(
        id: Int,
        partitions: List<Expr>,
        sorts: List<OrderBy.Sort>
    ) = Over(id, partitions, sorts)

    public open fun onConflict(
        id: Int,
        expr: Expr,
        action: OnConflict.Action
    ) = OnConflict(id, expr, action)

    public open fun onConflictActionDoReplace(id: Int, `value`: OnConflict.Value) =
        OnConflict.Action.DoReplace(id, value)

    public open fun onConflictActionDoUpdate(id: Int, `value`: OnConflict.Value) =
        OnConflict.Action.DoUpdate(id, value)

    public open fun onConflictActionDoNothing(id: Int) = OnConflict.Action.DoNothing(id)

    public open fun returning(id: Int, columns: List<Returning.Column>) = Returning(id, columns)

    public open fun returningColumn(
        id: Int,
        status: Returning.Column.Status,
        age: Returning.Column.Age,
        `value`: Returning.Column.Value
    ) = Returning.Column(id, status, age, value)

    public open fun returningColumnValueWildcard(id: Int) = Returning.Column.Value.Wildcard(id)

    public open fun returningColumnValueExpression(id: Int, expr: Expr) =
        Returning.Column.Value.Expression(id, expr)

    public open fun tableDefinition(id: Int, columns: List<TableDefinition.Column>) =
        TableDefinition(id, columns)

    public open fun tableDefinitionColumn(
        id: Int,
        name: String,
        type: Type,
        constraints: List<TableDefinition.Column.Constraint>
    ) = TableDefinition.Column(id, name, type, constraints)

    public open fun tableDefinitionColumnConstraint(
        id: Int,
        name: String?,
        body: TableDefinition.Column.Constraint.Body
    ) = TableDefinition.Column.Constraint(id, name, body)

    public open fun tableDefinitionColumnConstraintBodyNullable(id: Int) =
        TableDefinition.Column.Constraint.Body.Nullable(id)

    public open fun tableDefinitionColumnConstraintBodyNotNull(id: Int) =
        TableDefinition.Column.Constraint.Body.NotNull(id)

    public open fun tableDefinitionColumnConstraintBodyCheck(id: Int, expr: Expr) =
        TableDefinition.Column.Constraint.Body.Check(id, expr)

    public companion object {
        public val DEFAULT: AstFactory = object : AstFactory() {}

        @JvmStatic
        public fun <T : AstNode> create(block: AstFactory.() -> T) = AstFactory.DEFAULT.block()
    }
}

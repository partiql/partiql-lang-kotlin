package org.partiql.ast.builder

import com.amazon.ionelement.api.IonElement
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
import kotlin.collections.MutableList

public class StatementQueryBuilder {
    public var expr: Expr? = null

    public fun expr(expr: Expr?): StatementQueryBuilder = this.apply {
        this.expr = expr
    }

    public fun build(): Statement.Query = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Query =
        factory.statementQuery(expr = expr!!)
}

public class StatementDmlInsertBuilder {
    public var target: Expr? = null

    public var values: Expr? = null

    public var onConflict: OnConflict.Action? = null

    public fun target(target: Expr?): StatementDmlInsertBuilder = this.apply {
        this.target = target
    }

    public fun values(values: Expr?): StatementDmlInsertBuilder = this.apply {
        this.values = values
    }

    public fun onConflict(onConflict: OnConflict.Action?): StatementDmlInsertBuilder = this.apply {
        this.onConflict = onConflict
    }

    public fun build(): Statement.Dml.Insert = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Dml.Insert =
        factory.statementDmlInsert(target = target!!, values = values!!, onConflict = onConflict!!)
}

public class StatementDmlInsertValueBuilder {
    public var target: Expr? = null

    public var `value`: Expr? = null

    public var atAlias: Expr? = null

    public var index: Expr? = null

    public var onConflict: OnConflict? = null

    public fun target(target: Expr?): StatementDmlInsertValueBuilder = this.apply {
        this.target = target
    }

    public fun `value`(`value`: Expr?): StatementDmlInsertValueBuilder = this.apply {
        this.`value` = `value`
    }

    public fun atAlias(atAlias: Expr?): StatementDmlInsertValueBuilder = this.apply {
        this.atAlias = atAlias
    }

    public fun index(index: Expr?): StatementDmlInsertValueBuilder = this.apply {
        this.index = index
    }

    public fun onConflict(onConflict: OnConflict?): StatementDmlInsertValueBuilder = this.apply {
        this.onConflict = onConflict
    }

    public fun build(): Statement.Dml.InsertValue = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Dml.InsertValue =
        factory.statementDmlInsertValue(
            target = target!!, value = value!!, atAlias = atAlias!!,
            index =
            index,
            onConflict = onConflict!!
        )
}

public class StatementDmlSetBuilder {
    public var assignments: MutableList<Statement.Dml.Set.Assignment> = mutableListOf()

    public fun assignments(assignments: MutableList<Statement.Dml.Set.Assignment>):
        StatementDmlSetBuilder = this.apply {
        this.assignments = assignments
    }

    public fun build(): Statement.Dml.Set = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Dml.Set =
        factory.statementDmlSet(assignments = assignments)
}

public class StatementDmlSetAssignmentBuilder {
    public var target: Expr.Path? = null

    public var `value`: Expr? = null

    public fun target(target: Expr.Path?): StatementDmlSetAssignmentBuilder = this.apply {
        this.target = target
    }

    public fun `value`(`value`: Expr?): StatementDmlSetAssignmentBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): Statement.Dml.Set.Assignment = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Dml.Set.Assignment =
        factory.statementDmlSetAssignment(target = target!!, value = value!!)
}

public class StatementDmlRemoveBuilder {
    public var target: Expr.Path? = null

    public fun target(target: Expr.Path?): StatementDmlRemoveBuilder = this.apply {
        this.target = target
    }

    public fun build(): Statement.Dml.Remove = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Dml.Remove =
        factory.statementDmlRemove(target = target!!)
}

public class StatementDmlDeleteBuilder {
    public var from: From? = null

    public var `where`: Expr? = null

    public var returning: Returning? = null

    public fun from(from: From?): StatementDmlDeleteBuilder = this.apply {
        this.from = from
    }

    public fun `where`(`where`: Expr?): StatementDmlDeleteBuilder = this.apply {
        this.`where` = `where`
    }

    public fun returning(returning: Returning?): StatementDmlDeleteBuilder = this.apply {
        this.returning = returning
    }

    public fun build(): Statement.Dml.Delete = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Dml.Delete =
        factory.statementDmlDelete(from = from!!, where = where, returning = returning!!)
}

public class StatementDdlCreateTableBuilder {
    public var name: String? = null

    public var definition: TableDefinition? = null

    public fun name(name: String?): StatementDdlCreateTableBuilder = this.apply {
        this.name = name
    }

    public fun definition(definition: TableDefinition?): StatementDdlCreateTableBuilder = this.apply {
        this.definition = definition
    }

    public fun build(): Statement.Ddl.CreateTable = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Ddl.CreateTable =
        factory.statementDdlCreateTable(name = name!!, definition = definition)
}

public class StatementDdlCreateIndexBuilder {
    public var name: String? = null

    public var fields: MutableList<Expr> = mutableListOf()

    public fun name(name: String?): StatementDdlCreateIndexBuilder = this.apply {
        this.name = name
    }

    public fun fields(fields: MutableList<Expr>): StatementDdlCreateIndexBuilder = this.apply {
        this.fields = fields
    }

    public fun build(): Statement.Ddl.CreateIndex = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Ddl.CreateIndex =
        factory.statementDdlCreateIndex(name = name!!, fields = fields)
}

public class StatementDdlDropTableBuilder {
    public var identifier: Expr.Identifier? = null

    public fun identifier(identifier: Expr.Identifier?): StatementDdlDropTableBuilder = this.apply {
        this.identifier = identifier
    }

    public fun build(): Statement.Ddl.DropTable = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Ddl.DropTable =
        factory.statementDdlDropTable(identifier = identifier!!)
}

public class StatementDdlDropIndexBuilder {
    public var table: Expr.Identifier? = null

    public var keys: Expr.Identifier? = null

    public fun table(table: Expr.Identifier?): StatementDdlDropIndexBuilder = this.apply {
        this.table = table
    }

    public fun keys(keys: Expr.Identifier?): StatementDdlDropIndexBuilder = this.apply {
        this.keys = keys
    }

    public fun build(): Statement.Ddl.DropIndex = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Ddl.DropIndex =
        factory.statementDdlDropIndex(table = table!!, keys = keys!!)
}

public class StatementExecBuilder {
    public var procedure: String? = null

    public var args: MutableList<Expr> = mutableListOf()

    public fun procedure(procedure: String?): StatementExecBuilder = this.apply {
        this.procedure = procedure
    }

    public fun args(args: MutableList<Expr>): StatementExecBuilder = this.apply {
        this.args = args
    }

    public fun build(): Statement.Exec = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Exec =
        factory.statementExec(procedure = procedure!!, args = args)
}

public class StatementExplainBuilder {
    public var target: Statement.Explain.Target? = null

    public fun target(target: Statement.Explain.Target?): StatementExplainBuilder = this.apply {
        this.target = target
    }

    public fun build(): Statement.Explain = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Explain =
        factory.statementExplain(target = target!!)
}

public class StatementExplainTargetDomainBuilder {
    public var statement: Statement? = null

    public var type: String? = null

    public var format: String? = null

    public fun statement(statement: Statement?): StatementExplainTargetDomainBuilder = this.apply {
        this.statement = statement
    }

    public fun type(type: String?): StatementExplainTargetDomainBuilder = this.apply {
        this.type = type
    }

    public fun format(format: String?): StatementExplainTargetDomainBuilder = this.apply {
        this.format = format
    }

    public fun build(): Statement.Explain.Target.Domain = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Explain.Target.Domain =
        factory.statementExplainTargetDomain(statement = statement!!, type = type, format = format)
}

public class ExprMissingBuilder {
    public fun build(): Expr.Missing = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Missing = factory.exprMissing()
}

public class ExprLitBuilder {
    public var `value`: IonElement? = null

    public fun `value`(`value`: IonElement?): ExprLitBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): Expr.Lit = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Lit = factory.exprLit(
        value =
        value!!
    )
}

public class ExprIdentifierBuilder {
    public var name: String? = null

    public var case: Case? = null

    public var scope: Expr.Identifier.Scope? = null

    public fun name(name: String?): ExprIdentifierBuilder = this.apply {
        this.name = name
    }

    public fun case(case: Case?): ExprIdentifierBuilder = this.apply {
        this.case = case
    }

    public fun scope(scope: Expr.Identifier.Scope?): ExprIdentifierBuilder = this.apply {
        this.scope = scope
    }

    public fun build(): Expr.Identifier = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Identifier =
        factory.exprIdentifier(name = name!!, case = case!!, scope = scope!!)
}

public class ExprPathBuilder {
    public var root: Expr? = null

    public var steps: MutableList<Expr.Path.Step> = mutableListOf()

    public fun root(root: Expr?): ExprPathBuilder = this.apply {
        this.root = root
    }

    public fun steps(steps: MutableList<Expr.Path.Step>): ExprPathBuilder = this.apply {
        this.steps = steps
    }

    public fun build(): Expr.Path = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path = factory.exprPath(
        root =
        root!!,
        steps = steps
    )
}

public class ExprPathStepKeyBuilder {
    public var `value`: Expr? = null

    public fun `value`(`value`: Expr?): ExprPathStepKeyBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): Expr.Path.Step.Key = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path.Step.Key =
        factory.exprPathStepKey(value = value!!)
}

public class ExprPathStepWildcardBuilder {
    public fun build(): Expr.Path.Step.Wildcard = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path.Step.Wildcard =
        factory.exprPathStepWildcard()
}

public class ExprPathStepUnpivotBuilder {
    public fun build(): Expr.Path.Step.Unpivot = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path.Step.Unpivot =
        factory.exprPathStepUnpivot()
}

public class ExprCallBuilder {
    public var function: String? = null

    public var args: MutableList<Expr> = mutableListOf()

    public fun function(function: String?): ExprCallBuilder = this.apply {
        this.function = function
    }

    public fun args(args: MutableList<Expr>): ExprCallBuilder = this.apply {
        this.args = args
    }

    public fun build(): Expr.Call = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Call = factory.exprCall(
        function =
        function!!,
        args = args
    )
}

public class ExprAggBuilder {
    public var function: String? = null

    public var args: MutableList<Expr> = mutableListOf()

    public var quantifier: SetQuantifier? = null

    public fun function(function: String?): ExprAggBuilder = this.apply {
        this.function = function
    }

    public fun args(args: MutableList<Expr>): ExprAggBuilder = this.apply {
        this.args = args
    }

    public fun quantifier(quantifier: SetQuantifier?): ExprAggBuilder = this.apply {
        this.quantifier = quantifier
    }

    public fun build(): Expr.Agg = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Agg = factory.exprAgg(
        function =
        function!!,
        args = args, quantifier = quantifier!!
    )
}

public class ExprParameterBuilder {
    public var index: Int? = null

    public fun index(index: Int?): ExprParameterBuilder = this.apply {
        this.index = index
    }

    public fun build(): Expr.Parameter = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Parameter =
        factory.exprParameter(index = index!!)
}

public class ExprUnaryBuilder {
    public var op: Expr.Unary.Op? = null

    public var expr: Expr? = null

    public fun op(op: Expr.Unary.Op?): ExprUnaryBuilder = this.apply {
        this.op = op
    }

    public fun expr(expr: Expr?): ExprUnaryBuilder = this.apply {
        this.expr = expr
    }

    public fun build(): Expr.Unary = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Unary = factory.exprUnary(
        op =
        op!!,
        expr = expr!!
    )
}

public class ExprBinaryBuilder {
    public var op: Expr.Binary.Op? = null

    public var lhs: Expr? = null

    public var rhs: Expr? = null

    public fun op(op: Expr.Binary.Op?): ExprBinaryBuilder = this.apply {
        this.op = op
    }

    public fun lhs(lhs: Expr?): ExprBinaryBuilder = this.apply {
        this.lhs = lhs
    }

    public fun rhs(rhs: Expr?): ExprBinaryBuilder = this.apply {
        this.rhs = rhs
    }

    public fun build(): Expr.Binary = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Binary = factory.exprBinary(
        op =
        op!!,
        lhs = lhs!!, rhs = rhs!!
    )
}

public class ExprCollectionBuilder {
    public var type: Expr.Collection.Type? = null

    public var values: MutableList<Expr> = mutableListOf()

    public fun type(type: Expr.Collection.Type?): ExprCollectionBuilder = this.apply {
        this.type = type
    }

    public fun values(values: MutableList<Expr>): ExprCollectionBuilder = this.apply {
        this.values = values
    }

    public fun build(): Expr.Collection = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Collection =
        factory.exprCollection(type = type!!, values = values)
}

public class ExprTupleBuilder {
    public var fields: MutableList<Expr.Tuple.Field> = mutableListOf()

    public fun fields(fields: MutableList<Expr.Tuple.Field>): ExprTupleBuilder = this.apply {
        this.fields = fields
    }

    public fun build(): Expr.Tuple = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Tuple = factory.exprTuple(
        fields =
        fields
    )
}

public class ExprTupleFieldBuilder {
    public var name: Expr? = null

    public var `value`: Expr? = null

    public fun name(name: Expr?): ExprTupleFieldBuilder = this.apply {
        this.name = name
    }

    public fun `value`(`value`: Expr?): ExprTupleFieldBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): Expr.Tuple.Field = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Tuple.Field =
        factory.exprTupleField(name = name!!, value = value!!)
}

public class ExprDateBuilder {
    public var year: Long? = null

    public var month: Long? = null

    public var day: Long? = null

    public fun year(year: Long?): ExprDateBuilder = this.apply {
        this.year = year
    }

    public fun month(month: Long?): ExprDateBuilder = this.apply {
        this.month = month
    }

    public fun day(day: Long?): ExprDateBuilder = this.apply {
        this.day = day
    }

    public fun build(): Expr.Date = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Date = factory.exprDate(
        year =
        year!!,
        month = month!!, day = day!!
    )
}

public class ExprTimeBuilder {
    public var hour: Long? = null

    public var minute: Long? = null

    public var second: Long? = null

    public var nano: Long? = null

    public var precision: Long? = null

    public var tzOffsetMinutes: Long? = null

    public fun hour(hour: Long?): ExprTimeBuilder = this.apply {
        this.hour = hour
    }

    public fun minute(minute: Long?): ExprTimeBuilder = this.apply {
        this.minute = minute
    }

    public fun second(second: Long?): ExprTimeBuilder = this.apply {
        this.second = second
    }

    public fun nano(nano: Long?): ExprTimeBuilder = this.apply {
        this.nano = nano
    }

    public fun precision(precision: Long?): ExprTimeBuilder = this.apply {
        this.precision = precision
    }

    public fun tzOffsetMinutes(tzOffsetMinutes: Long?): ExprTimeBuilder = this.apply {
        this.tzOffsetMinutes = tzOffsetMinutes
    }

    public fun build(): Expr.Time = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Time = factory.exprTime(
        hour =
        hour!!,
        minute = minute!!, second = second!!, nano = nano!!, precision = precision!!,
        tzOffsetMinutes = tzOffsetMinutes
    )
}

public class ExprLikeBuilder {
    public var `value`: Expr? = null

    public var pattern: Expr? = null

    public var escape: Expr? = null

    public fun `value`(`value`: Expr?): ExprLikeBuilder = this.apply {
        this.`value` = `value`
    }

    public fun pattern(pattern: Expr?): ExprLikeBuilder = this.apply {
        this.pattern = pattern
    }

    public fun escape(escape: Expr?): ExprLikeBuilder = this.apply {
        this.escape = escape
    }

    public fun build(): Expr.Like = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Like = factory.exprLike(
        value =
        value!!,
        pattern = pattern!!, escape = escape
    )
}

public class ExprBetweenBuilder {
    public var `value`: Expr? = null

    public var from: Expr? = null

    public var to: Expr? = null

    public fun `value`(`value`: Expr?): ExprBetweenBuilder = this.apply {
        this.`value` = `value`
    }

    public fun from(from: Expr?): ExprBetweenBuilder = this.apply {
        this.from = from
    }

    public fun to(to: Expr?): ExprBetweenBuilder = this.apply {
        this.to = to
    }

    public fun build(): Expr.Between = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Between =
        factory.exprBetween(value = value!!, from = from!!, to = to!!)
}

public class ExprInCollectionBuilder {
    public var lhs: Expr? = null

    public var rhs: Expr? = null

    public fun lhs(lhs: Expr?): ExprInCollectionBuilder = this.apply {
        this.lhs = lhs
    }

    public fun rhs(rhs: Expr?): ExprInCollectionBuilder = this.apply {
        this.rhs = rhs
    }

    public fun build(): Expr.InCollection = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.InCollection =
        factory.exprInCollection(lhs = lhs!!, rhs = rhs!!)
}

public class ExprIsTypeBuilder {
    public var `value`: Expr? = null

    public var type: Expr.Collection.Type? = null

    public fun `value`(`value`: Expr?): ExprIsTypeBuilder = this.apply {
        this.`value` = `value`
    }

    public fun type(type: Expr.Collection.Type?): ExprIsTypeBuilder = this.apply {
        this.type = type
    }

    public fun build(): Expr.IsType = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.IsType = factory.exprIsType(
        value =
        value!!,
        type = type!!
    )
}

public class ExprSwitchBuilder {
    public var expr: Expr? = null

    public var branches: MutableList<Expr.Switch.Branch> = mutableListOf()

    public var default: Expr? = null

    public fun expr(expr: Expr?): ExprSwitchBuilder = this.apply {
        this.expr = expr
    }

    public fun branches(branches: MutableList<Expr.Switch.Branch>): ExprSwitchBuilder = this.apply {
        this.branches = branches
    }

    public fun default(default: Expr?): ExprSwitchBuilder = this.apply {
        this.default = default
    }

    public fun build(): Expr.Switch = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Switch = factory.exprSwitch(
        expr =
        expr,
        branches = branches, default = default
    )
}

public class ExprSwitchBranchBuilder {
    public var condition: Expr? = null

    public var expr: Expr? = null

    public fun condition(condition: Expr?): ExprSwitchBranchBuilder = this.apply {
        this.condition = condition
    }

    public fun expr(expr: Expr?): ExprSwitchBranchBuilder = this.apply {
        this.expr = expr
    }

    public fun build(): Expr.Switch.Branch = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Switch.Branch =
        factory.exprSwitchBranch(condition = condition!!, expr = expr!!)
}

public class ExprCoalesceBuilder {
    public var args: MutableList<Expr> = mutableListOf()

    public fun args(args: MutableList<Expr>): ExprCoalesceBuilder = this.apply {
        this.args = args
    }

    public fun build(): Expr.Coalesce = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Coalesce =
        factory.exprCoalesce(args = args)
}

public class ExprNullIfBuilder {
    public var expr1: Expr? = null

    public var expr2: Expr? = null

    public fun expr1(expr1: Expr?): ExprNullIfBuilder = this.apply {
        this.expr1 = expr1
    }

    public fun expr2(expr2: Expr?): ExprNullIfBuilder = this.apply {
        this.expr2 = expr2
    }

    public fun build(): Expr.NullIf = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.NullIf = factory.exprNullIf(
        expr1 =
        expr1!!,
        expr2 = expr2!!
    )
}

public class ExprCastBuilder {
    public var `value`: Expr? = null

    public var asType: Expr.Collection.Type? = null

    public fun `value`(`value`: Expr?): ExprCastBuilder = this.apply {
        this.`value` = `value`
    }

    public fun asType(asType: Expr.Collection.Type?): ExprCastBuilder = this.apply {
        this.asType = asType
    }

    public fun build(): Expr.Cast = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Cast = factory.exprCast(
        value =
        value!!,
        asType = asType!!
    )
}

public class ExprCanCastBuilder {
    public var `value`: Expr? = null

    public var asType: Expr.Collection.Type? = null

    public fun `value`(`value`: Expr?): ExprCanCastBuilder = this.apply {
        this.`value` = `value`
    }

    public fun asType(asType: Expr.Collection.Type?): ExprCanCastBuilder = this.apply {
        this.asType = asType
    }

    public fun build(): Expr.CanCast = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.CanCast =
        factory.exprCanCast(value = value!!, asType = asType!!)
}

public class ExprCanLosslessCastBuilder {
    public var `value`: Expr? = null

    public var asType: Expr.Collection.Type? = null

    public fun `value`(`value`: Expr?): ExprCanLosslessCastBuilder = this.apply {
        this.`value` = `value`
    }

    public fun asType(asType: Expr.Collection.Type?): ExprCanLosslessCastBuilder = this.apply {
        this.asType = asType
    }

    public fun build(): Expr.CanLosslessCast = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.CanLosslessCast =
        factory.exprCanLosslessCast(value = value!!, asType = asType!!)
}

public class ExprOuterBagOpBuilder {
    public var op: Expr.OuterBagOp.Op? = null

    public var quantifier: SetQuantifier? = null

    public var lhs: Expr? = null

    public var rhs: Expr? = null

    public fun op(op: Expr.OuterBagOp.Op?): ExprOuterBagOpBuilder = this.apply {
        this.op = op
    }

    public fun quantifier(quantifier: SetQuantifier?): ExprOuterBagOpBuilder = this.apply {
        this.quantifier = quantifier
    }

    public fun lhs(lhs: Expr?): ExprOuterBagOpBuilder = this.apply {
        this.lhs = lhs
    }

    public fun rhs(rhs: Expr?): ExprOuterBagOpBuilder = this.apply {
        this.rhs = rhs
    }

    public fun build(): Expr.OuterBagOp = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.OuterBagOp =
        factory.exprOuterBagOp(op = op!!, quantifier = quantifier!!, lhs = lhs!!, rhs = rhs!!)
}

public class ExprSfwBuilder {
    public var select: Select? = null

    public var from: From? = null

    public var let: Let? = null

    public var `where`: Expr? = null

    public var groupBy: GroupBy? = null

    public var having: Expr? = null

    public var orderBy: OrderBy? = null

    public var limit: Expr? = null

    public var offset: Expr? = null

    public fun select(select: Select?): ExprSfwBuilder = this.apply {
        this.select = select
    }

    public fun from(from: From?): ExprSfwBuilder = this.apply {
        this.from = from
    }

    public fun let(let: Let?): ExprSfwBuilder = this.apply {
        this.let = let
    }

    public fun `where`(`where`: Expr?): ExprSfwBuilder = this.apply {
        this.`where` = `where`
    }

    public fun groupBy(groupBy: GroupBy?): ExprSfwBuilder = this.apply {
        this.groupBy = groupBy
    }

    public fun having(having: Expr?): ExprSfwBuilder = this.apply {
        this.having = having
    }

    public fun orderBy(orderBy: OrderBy?): ExprSfwBuilder = this.apply {
        this.orderBy = orderBy
    }

    public fun limit(limit: Expr?): ExprSfwBuilder = this.apply {
        this.limit = limit
    }

    public fun offset(offset: Expr?): ExprSfwBuilder = this.apply {
        this.offset = offset
    }

    public fun build(): Expr.Sfw = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Sfw = factory.exprSfw(
        select =
        select!!,
        from = from!!, let = let, where = where, groupBy = groupBy, having = having,
        orderBy =
        orderBy,
        limit = limit, offset = offset
    )
}

public class ExprMatchBuilder {
    public var expr: Expr? = null

    public var pattern: GraphMatch? = null

    public fun expr(expr: Expr?): ExprMatchBuilder = this.apply {
        this.expr = expr
    }

    public fun pattern(pattern: GraphMatch?): ExprMatchBuilder = this.apply {
        this.pattern = pattern
    }

    public fun build(): Expr.Match = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Match = factory.exprMatch(
        expr =
        expr!!,
        pattern = pattern!!
    )
}

public class ExprWindowBuilder {
    public var function: String? = null

    public var over: Over? = null

    public var args: MutableList<Expr> = mutableListOf()

    public fun function(function: String?): ExprWindowBuilder = this.apply {
        this.function = function
    }

    public fun over(over: Over?): ExprWindowBuilder = this.apply {
        this.over = over
    }

    public fun args(args: MutableList<Expr>): ExprWindowBuilder = this.apply {
        this.args = args
    }

    public fun build(): Expr.Window = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Window =
        factory.exprWindow(function = function!!, over = over!!, args = args)
}

public class SelectStarBuilder {
    public fun build(): Select.Star = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Star = factory.selectStar()
}

public class SelectProjectBuilder {
    public var items: MutableList<Select.Project.Item> = mutableListOf()

    public fun items(items: MutableList<Select.Project.Item>): SelectProjectBuilder = this.apply {
        this.items = items
    }

    public fun build(): Select.Project = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Project =
        factory.selectProject(items = items)
}

public class SelectProjectItemAllBuilder {
    public var expr: Expr? = null

    public fun expr(expr: Expr?): SelectProjectItemAllBuilder = this.apply {
        this.expr = expr
    }

    public fun build(): Select.Project.Item.All = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Project.Item.All =
        factory.selectProjectItemAll(expr = expr!!)
}

public class SelectProjectItemVarBuilder {
    public var expr: Expr? = null

    public var asAlias: String? = null

    public fun expr(expr: Expr?): SelectProjectItemVarBuilder = this.apply {
        this.expr = expr
    }

    public fun asAlias(asAlias: String?): SelectProjectItemVarBuilder = this.apply {
        this.asAlias = asAlias
    }

    public fun build(): Select.Project.Item.Var = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Project.Item.Var =
        factory.selectProjectItemVar(expr = expr!!, asAlias = asAlias)
}

public class SelectPivotBuilder {
    public var `value`: Expr? = null

    public var key: Expr? = null

    public fun `value`(`value`: Expr?): SelectPivotBuilder = this.apply {
        this.`value` = `value`
    }

    public fun key(key: Expr?): SelectPivotBuilder = this.apply {
        this.key = key
    }

    public fun build(): Select.Pivot = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Pivot =
        factory.selectPivot(value = value!!, key = key!!)
}

public class SelectValueBuilder {
    public var `constructor`: Expr? = null

    public fun `constructor`(`constructor`: Expr?): SelectValueBuilder = this.apply {
        this.`constructor` = `constructor`
    }

    public fun build(): Select.Value = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Value =
        factory.selectValue(constructor = constructor!!)
}

public class FromCollectionBuilder {
    public var expr: Expr? = null

    public var unpivot: Boolean? = null

    public var asAlias: String? = null

    public var atAlias: String? = null

    public var byAlias: String? = null

    public fun expr(expr: Expr?): FromCollectionBuilder = this.apply {
        this.expr = expr
    }

    public fun unpivot(unpivot: Boolean?): FromCollectionBuilder = this.apply {
        this.unpivot = unpivot
    }

    public fun asAlias(asAlias: String?): FromCollectionBuilder = this.apply {
        this.asAlias = asAlias
    }

    public fun atAlias(atAlias: String?): FromCollectionBuilder = this.apply {
        this.atAlias = atAlias
    }

    public fun byAlias(byAlias: String?): FromCollectionBuilder = this.apply {
        this.byAlias = byAlias
    }

    public fun build(): From.Collection = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): From.Collection =
        factory.fromCollection(
            expr = expr!!, unpivot = unpivot, asAlias = asAlias, atAlias = atAlias,
            byAlias = byAlias
        )
}

public class FromJoinBuilder {
    public var type: From.Join.Type? = null

    public var condition: Expr? = null

    public var lhs: From? = null

    public var rhs: From? = null

    public fun type(type: From.Join.Type?): FromJoinBuilder = this.apply {
        this.type = type
    }

    public fun condition(condition: Expr?): FromJoinBuilder = this.apply {
        this.condition = condition
    }

    public fun lhs(lhs: From?): FromJoinBuilder = this.apply {
        this.lhs = lhs
    }

    public fun rhs(rhs: From?): FromJoinBuilder = this.apply {
        this.rhs = rhs
    }

    public fun build(): From.Join = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): From.Join = factory.fromJoin(
        type =
        type!!,
        condition = condition, lhs = lhs!!, rhs = rhs!!
    )
}

public class LetBuilder {
    public fun build(): Let = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Let = factory.let()
}

public class GroupByBuilder {
    public var strategy: GroupBy.Strategy? = null

    public var keys: MutableList<GroupBy.Key> = mutableListOf()

    public var asAlias: String? = null

    public fun strategy(strategy: GroupBy.Strategy?): GroupByBuilder = this.apply {
        this.strategy = strategy
    }

    public fun keys(keys: MutableList<GroupBy.Key>): GroupByBuilder = this.apply {
        this.keys = keys
    }

    public fun asAlias(asAlias: String?): GroupByBuilder = this.apply {
        this.asAlias = asAlias
    }

    public fun build(): GroupBy = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GroupBy = factory.groupBy(
        strategy =
        strategy!!,
        keys = keys, asAlias = asAlias
    )
}

public class GroupByKeyBuilder {
    public var expr: Expr? = null

    public var asAlias: String? = null

    public fun expr(expr: Expr?): GroupByKeyBuilder = this.apply {
        this.expr = expr
    }

    public fun asAlias(asAlias: String?): GroupByKeyBuilder = this.apply {
        this.asAlias = asAlias
    }

    public fun build(): GroupBy.Key = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GroupBy.Key = factory.groupByKey(
        expr =
        expr!!,
        asAlias = asAlias!!
    )
}

public class OrderByBuilder {
    public var sorts: MutableList<OrderBy.Sort> = mutableListOf()

    public fun sorts(sorts: MutableList<OrderBy.Sort>): OrderByBuilder = this.apply {
        this.sorts = sorts
    }

    public fun build(): OrderBy = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): OrderBy = factory.orderBy(
        sorts =
        sorts
    )
}

public class OrderBySortBuilder {
    public var expr: Expr? = null

    public var dir: OrderBy.Sort.Dir? = null

    public var nulls: OrderBy.Sort.Nulls? = null

    public fun expr(expr: Expr?): OrderBySortBuilder = this.apply {
        this.expr = expr
    }

    public fun dir(dir: OrderBy.Sort.Dir?): OrderBySortBuilder = this.apply {
        this.dir = dir
    }

    public fun nulls(nulls: OrderBy.Sort.Nulls?): OrderBySortBuilder = this.apply {
        this.nulls = nulls
    }

    public fun build(): OrderBy.Sort = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): OrderBy.Sort =
        factory.orderBySort(expr = expr!!, dir = dir!!, nulls = nulls!!)
}

public class GraphMatchBuilder {
    public var patterns: MutableList<GraphMatch.Pattern> = mutableListOf()

    public var selector: GraphMatch.Selector? = null

    public fun patterns(patterns: MutableList<GraphMatch.Pattern>): GraphMatchBuilder = this.apply {
        this.patterns = patterns
    }

    public fun selector(selector: GraphMatch.Selector?): GraphMatchBuilder = this.apply {
        this.selector = selector
    }

    public fun build(): GraphMatch = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch =
        factory.graphMatch(patterns = patterns, selector = selector)
}

public class GraphMatchPatternBuilder {
    public var restrictor: GraphMatch.Restrictor? = null

    public var prefilter: Expr? = null

    public var variable: String? = null

    public var quantifier: GraphMatch.Quantifier? = null

    public var parts: MutableList<GraphMatch.Pattern.Part> = mutableListOf()

    public fun restrictor(restrictor: GraphMatch.Restrictor?): GraphMatchPatternBuilder = this.apply {
        this.restrictor = restrictor
    }

    public fun prefilter(prefilter: Expr?): GraphMatchPatternBuilder = this.apply {
        this.prefilter = prefilter
    }

    public fun variable(variable: String?): GraphMatchPatternBuilder = this.apply {
        this.variable = variable
    }

    public fun quantifier(quantifier: GraphMatch.Quantifier?): GraphMatchPatternBuilder = this.apply {
        this.quantifier = quantifier
    }

    public fun parts(parts: MutableList<GraphMatch.Pattern.Part>): GraphMatchPatternBuilder =
        this.apply {
            this.parts = parts
        }

    public fun build(): GraphMatch.Pattern = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Pattern =
        factory.graphMatchPattern(
            restrictor = restrictor, prefilter = prefilter, variable = variable,
            quantifier = quantifier, parts = parts
        )
}

public class GraphMatchPatternPartNodeBuilder {
    public var prefilter: Expr? = null

    public var variable: String? = null

    public var label: MutableList<String> = mutableListOf()

    public fun prefilter(prefilter: Expr?): GraphMatchPatternPartNodeBuilder = this.apply {
        this.prefilter = prefilter
    }

    public fun variable(variable: String?): GraphMatchPatternPartNodeBuilder = this.apply {
        this.variable = variable
    }

    public fun label(label: MutableList<String>): GraphMatchPatternPartNodeBuilder = this.apply {
        this.label = label
    }

    public fun build(): GraphMatch.Pattern.Part.Node = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Pattern.Part.Node =
        factory.graphMatchPatternPartNode(prefilter = prefilter, variable = variable, label = label)
}

public class GraphMatchPatternPartEdgeBuilder {
    public var direction: GraphMatch.Direction? = null

    public var quantifier: GraphMatch.Quantifier? = null

    public var prefilter: Expr? = null

    public var variable: String? = null

    public var label: MutableList<String> = mutableListOf()

    public fun direction(direction: GraphMatch.Direction?): GraphMatchPatternPartEdgeBuilder =
        this.apply {
            this.direction = direction
        }

    public fun quantifier(quantifier: GraphMatch.Quantifier?): GraphMatchPatternPartEdgeBuilder =
        this.apply {
            this.quantifier = quantifier
        }

    public fun prefilter(prefilter: Expr?): GraphMatchPatternPartEdgeBuilder = this.apply {
        this.prefilter = prefilter
    }

    public fun variable(variable: String?): GraphMatchPatternPartEdgeBuilder = this.apply {
        this.variable = variable
    }

    public fun label(label: MutableList<String>): GraphMatchPatternPartEdgeBuilder = this.apply {
        this.label = label
    }

    public fun build(): GraphMatch.Pattern.Part.Edge = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Pattern.Part.Edge =
        factory.graphMatchPatternPartEdge(
            direction = direction!!, quantifier = quantifier,
            prefilter =
            prefilter,
            variable = variable, label = label
        )
}

public class GraphMatchQuantifierBuilder {
    public var lower: Long? = null

    public var upper: Int? = null

    public fun lower(lower: Long?): GraphMatchQuantifierBuilder = this.apply {
        this.lower = lower
    }

    public fun upper(upper: Int?): GraphMatchQuantifierBuilder = this.apply {
        this.upper = upper
    }

    public fun build(): GraphMatch.Quantifier = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Quantifier =
        factory.graphMatchQuantifier(lower = lower!!, upper = upper)
}

public class GraphMatchSelectorAnyShortestBuilder {
    public fun build(): GraphMatch.Selector.AnyShortest = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.AnyShortest =
        factory.graphMatchSelectorAnyShortest()
}

public class GraphMatchSelectorAllShortestBuilder {
    public fun build(): GraphMatch.Selector.AllShortest = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.AllShortest =
        factory.graphMatchSelectorAllShortest()
}

public class GraphMatchSelectorAnyBuilder {
    public fun build(): GraphMatch.Selector.Any = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.Any =
        factory.graphMatchSelectorAny()
}

public class GraphMatchSelectorAnyKBuilder {
    public var k: Long? = null

    public fun k(k: Long?): GraphMatchSelectorAnyKBuilder = this.apply {
        this.k = k
    }

    public fun build(): GraphMatch.Selector.AnyK = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.AnyK =
        factory.graphMatchSelectorAnyK(k = k!!)
}

public class GraphMatchSelectorShortestKBuilder {
    public var k: Long? = null

    public fun k(k: Long?): GraphMatchSelectorShortestKBuilder = this.apply {
        this.k = k
    }

    public fun build(): GraphMatch.Selector.ShortestK = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.ShortestK =
        factory.graphMatchSelectorShortestK(k = k!!)
}

public class GraphMatchSelectorShortestKGroupBuilder {
    public var k: Long? = null

    public fun k(k: Long?): GraphMatchSelectorShortestKGroupBuilder = this.apply {
        this.k = k
    }

    public fun build(): GraphMatch.Selector.ShortestKGroup = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.ShortestKGroup =
        factory.graphMatchSelectorShortestKGroup(k = k!!)
}

public class OverBuilder {
    public var partitions: MutableList<Expr> = mutableListOf()

    public var sorts: MutableList<OrderBy.Sort> = mutableListOf()

    public fun partitions(partitions: MutableList<Expr>): OverBuilder = this.apply {
        this.partitions = partitions
    }

    public fun sorts(sorts: MutableList<OrderBy.Sort>): OverBuilder = this.apply {
        this.sorts = sorts
    }

    public fun build(): Over = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Over = factory.over(
        partitions =
        partitions,
        sorts = sorts
    )
}

public class OnConflictBuilder {
    public var expr: Expr? = null

    public var action: OnConflict.Action? = null

    public fun expr(expr: Expr?): OnConflictBuilder = this.apply {
        this.expr = expr
    }

    public fun action(action: OnConflict.Action?): OnConflictBuilder = this.apply {
        this.action = action
    }

    public fun build(): OnConflict = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict = factory.onConflict(
        expr =
        expr!!,
        action = action!!
    )
}

public class OnConflictActionDoReplaceBuilder {
    public var `value`: OnConflict.Value? = null

    public fun `value`(`value`: OnConflict.Value?): OnConflictActionDoReplaceBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): OnConflict.Action.DoReplace = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict.Action.DoReplace =
        factory.onConflictActionDoReplace(value = value!!)
}

public class OnConflictActionDoUpdateBuilder {
    public var `value`: OnConflict.Value? = null

    public fun `value`(`value`: OnConflict.Value?): OnConflictActionDoUpdateBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): OnConflict.Action.DoUpdate = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict.Action.DoUpdate =
        factory.onConflictActionDoUpdate(value = value!!)
}

public class OnConflictActionDoNothingBuilder {
    public fun build(): OnConflict.Action.DoNothing = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict.Action.DoNothing =
        factory.onConflictActionDoNothing()
}

public class ReturningBuilder {
    public var columns: MutableList<Returning.Column> = mutableListOf()

    public fun columns(columns: MutableList<Returning.Column>): ReturningBuilder = this.apply {
        this.columns = columns
    }

    public fun build(): Returning = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning = factory.returning(
        columns =
        columns
    )
}

public class ReturningColumnBuilder {
    public var status: Returning.Column.Status? = null

    public var age: Returning.Column.Age? = null

    public var `value`: Returning.Column.Value? = null

    public fun status(status: Returning.Column.Status?): ReturningColumnBuilder = this.apply {
        this.status = status
    }

    public fun age(age: Returning.Column.Age?): ReturningColumnBuilder = this.apply {
        this.age = age
    }

    public fun `value`(`value`: Returning.Column.Value?): ReturningColumnBuilder = this.apply {
        this.`value` = `value`
    }

    public fun build(): Returning.Column = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning.Column =
        factory.returningColumn(status = status!!, age = age!!, value = value!!)
}

public class ReturningColumnValueWildcardBuilder {
    public fun build(): Returning.Column.Value.Wildcard = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning.Column.Value.Wildcard =
        factory.returningColumnValueWildcard()
}

public class ReturningColumnValueExpressionBuilder {
    public var expr: Expr? = null

    public fun expr(expr: Expr?): ReturningColumnValueExpressionBuilder = this.apply {
        this.expr = expr
    }

    public fun build(): Returning.Column.Value.Expression = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning.Column.Value.Expression =
        factory.returningColumnValueExpression(expr = expr!!)
}

public class TableDefinitionBuilder {
    public var columns: MutableList<TableDefinition.Column> = mutableListOf()

    public fun columns(columns: MutableList<TableDefinition.Column>): TableDefinitionBuilder =
        this.apply {
            this.columns = columns
        }

    public fun build(): TableDefinition = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): TableDefinition =
        factory.tableDefinition(columns = columns)
}

public class TableDefinitionColumnBuilder {
    public var name: String? = null

    public var type: Expr.Collection.Type? = null

    public var constraints: MutableList<TableDefinition.Column.Constraint> = mutableListOf()

    public fun name(name: String?): TableDefinitionColumnBuilder = this.apply {
        this.name = name
    }

    public fun type(type: Expr.Collection.Type?): TableDefinitionColumnBuilder = this.apply {
        this.type = type
    }

    public fun constraints(constraints: MutableList<TableDefinition.Column.Constraint>):
        TableDefinitionColumnBuilder = this.apply {
        this.constraints = constraints
    }

    public fun build(): TableDefinition.Column = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT): TableDefinition.Column =
        factory.tableDefinitionColumn(name = name!!, type = type!!, constraints = constraints)
}

public class TableDefinitionColumnConstraintNullableBuilder {
    public fun build(): TableDefinition.Column.Constraint.Nullable = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT):
        TableDefinition.Column.Constraint.Nullable = factory.tableDefinitionColumnConstraintNullable()
}

public class TableDefinitionColumnConstraintNotNullBuilder {
    public fun build(): TableDefinition.Column.Constraint.NotNull = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT):
        TableDefinition.Column.Constraint.NotNull = factory.tableDefinitionColumnConstraintNotNull()
}

public class TableDefinitionColumnConstraintCheckBuilder {
    public var expr: Expr? = null

    public fun expr(expr: Expr?): TableDefinitionColumnConstraintCheckBuilder = this.apply {
        this.expr = expr
    }

    public fun build(): TableDefinition.Column.Constraint.Check = build(AstFactory.DEFAULT)

    public fun build(factory: AstFactory = AstFactory.DEFAULT):
        TableDefinition.Column.Constraint.Check = factory.tableDefinitionColumnConstraintCheck(
        expr =
        expr!!
    )
}

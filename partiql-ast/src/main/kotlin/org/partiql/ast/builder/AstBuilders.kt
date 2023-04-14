package org.partiql.ast.builder

import com.amazon.ionelement.api.IonElement
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.MutableList
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
import org.partiql.types.StaticType

public class StatementQueryBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public fun id(id: Int?): StatementQueryBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): StatementQueryBuilder = this.apply {
    this.expr = expr
  }

  public fun build(): Statement.Query = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Query =
      factory.statementQuery(id = id!!, expr = expr!!)
}

public class StatementDmlInsertBuilder {
  public var id: Int? = null

  public var target: Expr? = null

  public var values: Expr? = null

  public var onConflict: OnConflict.Action? = null

  public fun id(id: Int?): StatementDmlInsertBuilder = this.apply {
    this.id = id
  }

  public fun target(target: Expr?): StatementDmlInsertBuilder = this.apply {
    this.target = target
  }

  public fun values(values: Expr?): StatementDmlInsertBuilder = this.apply {
    this.values = values
  }

  public fun onConflict(onConflict: OnConflict.Action?): StatementDmlInsertBuilder = this.apply {
    this.onConflict = onConflict
  }

  public fun build(): Statement.DML.Insert = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DML.Insert =
      factory.statementDMLInsert(id = id!!, target = target!!, values = values!!, onConflict =
      onConflict!!)
}

public class StatementDmlInsertValueBuilder {
  public var id: Int? = null

  public var target: Expr? = null

  public var `value`: Expr? = null

  public var atAlias: Expr? = null

  public var index: Expr? = null

  public var onConflict: OnConflict? = null

  public fun id(id: Int?): StatementDmlInsertValueBuilder = this.apply {
    this.id = id
  }

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

  public fun build(): Statement.DML.InsertValue = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DML.InsertValue =
      factory.statementDMLInsertValue(id = id!!, target = target!!, value = value!!, atAlias =
      atAlias!!, index = index, onConflict = onConflict!!)
}

public class StatementDmlSetBuilder {
  public var id: Int? = null

  public var assignments: MutableList<Statement.DML.Set.Assignment> = mutableListOf()

  public fun id(id: Int?): StatementDmlSetBuilder = this.apply {
    this.id = id
  }

  public fun assignments(assignments: MutableList<Statement.DML.Set.Assignment>):
      StatementDmlSetBuilder = this.apply {
    this.assignments = assignments
  }

  public fun build(): Statement.DML.Set = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DML.Set =
      factory.statementDMLSet(id = id!!, assignments = assignments)
}

public class StatementDmlSetAssignmentBuilder {
  public var id: Int? = null

  public var target: Expr.Path? = null

  public var `value`: Expr? = null

  public fun id(id: Int?): StatementDmlSetAssignmentBuilder = this.apply {
    this.id = id
  }

  public fun target(target: Expr.Path?): StatementDmlSetAssignmentBuilder = this.apply {
    this.target = target
  }

  public fun `value`(`value`: Expr?): StatementDmlSetAssignmentBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Statement.DML.Set.Assignment = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DML.Set.Assignment =
      factory.statementDMLSetAssignment(id = id!!, target = target!!, value = value!!)
}

public class StatementDmlRemoveBuilder {
  public var id: Int? = null

  public var target: Expr.Path? = null

  public fun id(id: Int?): StatementDmlRemoveBuilder = this.apply {
    this.id = id
  }

  public fun target(target: Expr.Path?): StatementDmlRemoveBuilder = this.apply {
    this.target = target
  }

  public fun build(): Statement.DML.Remove = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DML.Remove =
      factory.statementDMLRemove(id = id!!, target = target!!)
}

public class StatementDmlDeleteBuilder {
  public var id: Int? = null

  public var from: From? = null

  public var `where`: Expr? = null

  public var returning: Returning? = null

  public fun id(id: Int?): StatementDmlDeleteBuilder = this.apply {
    this.id = id
  }

  public fun from(from: From?): StatementDmlDeleteBuilder = this.apply {
    this.from = from
  }

  public fun `where`(`where`: Expr?): StatementDmlDeleteBuilder = this.apply {
    this.`where` = `where`
  }

  public fun returning(returning: Returning?): StatementDmlDeleteBuilder = this.apply {
    this.returning = returning
  }

  public fun build(): Statement.DML.Delete = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DML.Delete =
      factory.statementDMLDelete(id = id!!, from = from!!, where = where, returning = returning!!)
}

public class StatementDdlCreateTableBuilder {
  public var id: Int? = null

  public var name: String? = null

  public var definition: TableDefinition? = null

  public fun id(id: Int?): StatementDdlCreateTableBuilder = this.apply {
    this.id = id
  }

  public fun name(name: String?): StatementDdlCreateTableBuilder = this.apply {
    this.name = name
  }

  public fun definition(definition: TableDefinition?): StatementDdlCreateTableBuilder = this.apply {
    this.definition = definition
  }

  public fun build(): Statement.DDL.CreateTable = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DDL.CreateTable =
      factory.statementDDLCreateTable(id = id!!, name = name!!, definition = definition)
}

public class StatementDdlCreateIndexBuilder {
  public var id: Int? = null

  public var name: String? = null

  public var fields: MutableList<Expr> = mutableListOf()

  public fun id(id: Int?): StatementDdlCreateIndexBuilder = this.apply {
    this.id = id
  }

  public fun name(name: String?): StatementDdlCreateIndexBuilder = this.apply {
    this.name = name
  }

  public fun fields(fields: MutableList<Expr>): StatementDdlCreateIndexBuilder = this.apply {
    this.fields = fields
  }

  public fun build(): Statement.DDL.CreateIndex = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DDL.CreateIndex =
      factory.statementDDLCreateIndex(id = id!!, name = name!!, fields = fields)
}

public class StatementDdlDropTableBuilder {
  public var id: Int? = null

  public var identifier: Expr.Identifier? = null

  public fun id(id: Int?): StatementDdlDropTableBuilder = this.apply {
    this.id = id
  }

  public fun identifier(identifier: Expr.Identifier?): StatementDdlDropTableBuilder = this.apply {
    this.identifier = identifier
  }

  public fun build(): Statement.DDL.DropTable = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DDL.DropTable =
      factory.statementDDLDropTable(id = id!!, identifier = identifier!!)
}

public class StatementDdlDropIndexBuilder {
  public var id: Int? = null

  public var table: Expr.Identifier? = null

  public var keys: Expr.Identifier? = null

  public fun id(id: Int?): StatementDdlDropIndexBuilder = this.apply {
    this.id = id
  }

  public fun table(table: Expr.Identifier?): StatementDdlDropIndexBuilder = this.apply {
    this.table = table
  }

  public fun keys(keys: Expr.Identifier?): StatementDdlDropIndexBuilder = this.apply {
    this.keys = keys
  }

  public fun build(): Statement.DDL.DropIndex = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.DDL.DropIndex =
      factory.statementDDLDropIndex(id = id!!, table = table!!, keys = keys!!)
}

public class StatementExecBuilder {
  public var id: Int? = null

  public var procedure: String? = null

  public var args: MutableList<Expr> = mutableListOf()

  public fun id(id: Int?): StatementExecBuilder = this.apply {
    this.id = id
  }

  public fun procedure(procedure: String?): StatementExecBuilder = this.apply {
    this.procedure = procedure
  }

  public fun args(args: MutableList<Expr>): StatementExecBuilder = this.apply {
    this.args = args
  }

  public fun build(): Statement.Exec = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Exec =
      factory.statementExec(id = id!!, procedure = procedure!!, args = args)
}

public class StatementExplainBuilder {
  public var id: Int? = null

  public var target: Statement.Explain.Target? = null

  public fun id(id: Int?): StatementExplainBuilder = this.apply {
    this.id = id
  }

  public fun target(target: Statement.Explain.Target?): StatementExplainBuilder = this.apply {
    this.target = target
  }

  public fun build(): Statement.Explain = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Statement.Explain =
      factory.statementExplain(id = id!!, target = target!!)
}

public class StatementExplainTargetDomainBuilder {
  public var id: Int? = null

  public var statement: Statement? = null

  public var type: String? = null

  public var format: String? = null

  public fun id(id: Int?): StatementExplainTargetDomainBuilder = this.apply {
    this.id = id
  }

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
      factory.statementExplainTargetDomain(id = id!!, statement = statement!!, type = type, format =
      format)
}

public class ExprMissingBuilder {
  public var id: Int? = null

  public fun id(id: Int?): ExprMissingBuilder = this.apply {
    this.id = id
  }

  public fun build(): Expr.Missing = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Missing = factory.exprMissing(id
      = id!!)
}

public class ExprLitBuilder {
  public var id: Int? = null

  public var `value`: IonElement? = null

  public fun id(id: Int?): ExprLitBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: IonElement?): ExprLitBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Expr.Lit = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Lit = factory.exprLit(id = id!!,
      value = value!!)
}

public class ExprIdentifierBuilder {
  public var id: Int? = null

  public var name: String? = null

  public var case: Case? = null

  public var scope: Expr.Identifier.Scope? = null

  public fun id(id: Int?): ExprIdentifierBuilder = this.apply {
    this.id = id
  }

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
      factory.exprIdentifier(id = id!!, name = name!!, case = case!!, scope = scope!!)
}

public class ExprPathBuilder {
  public var id: Int? = null

  public var root: Expr? = null

  public var steps: MutableList<Expr.Path.Step> = mutableListOf()

  public fun id(id: Int?): ExprPathBuilder = this.apply {
    this.id = id
  }

  public fun root(root: Expr?): ExprPathBuilder = this.apply {
    this.root = root
  }

  public fun steps(steps: MutableList<Expr.Path.Step>): ExprPathBuilder = this.apply {
    this.steps = steps
  }

  public fun build(): Expr.Path = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path = factory.exprPath(id =
      id!!, root = root!!, steps = steps)
}

public class ExprPathStepKeyBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public fun id(id: Int?): ExprPathStepKeyBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: Expr?): ExprPathStepKeyBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Expr.Path.Step.Key = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path.Step.Key =
      factory.exprPathStepKey(id = id!!, value = value!!)
}

public class ExprPathStepWildcardBuilder {
  public var id: Int? = null

  public fun id(id: Int?): ExprPathStepWildcardBuilder = this.apply {
    this.id = id
  }

  public fun build(): Expr.Path.Step.Wildcard = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path.Step.Wildcard =
      factory.exprPathStepWildcard(id = id!!)
}

public class ExprPathStepUnpivotBuilder {
  public var id: Int? = null

  public fun id(id: Int?): ExprPathStepUnpivotBuilder = this.apply {
    this.id = id
  }

  public fun build(): Expr.Path.Step.Unpivot = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Path.Step.Unpivot =
      factory.exprPathStepUnpivot(id = id!!)
}

public class ExprCallBuilder {
  public var id: Int? = null

  public var function: String? = null

  public var args: MutableList<Expr> = mutableListOf()

  public fun id(id: Int?): ExprCallBuilder = this.apply {
    this.id = id
  }

  public fun function(function: String?): ExprCallBuilder = this.apply {
    this.function = function
  }

  public fun args(args: MutableList<Expr>): ExprCallBuilder = this.apply {
    this.args = args
  }

  public fun build(): Expr.Call = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Call = factory.exprCall(id =
      id!!, function = function!!, args = args)
}

public class ExprAggBuilder {
  public var id: Int? = null

  public var function: String? = null

  public var args: MutableList<Expr> = mutableListOf()

  public var quantifier: SetQuantifier? = null

  public fun id(id: Int?): ExprAggBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Agg = factory.exprAgg(id = id!!,
      function = function!!, args = args, quantifier = quantifier!!)
}

public class ExprParameterBuilder {
  public var id: Int? = null

  public var index: Int? = null

  public fun id(id: Int?): ExprParameterBuilder = this.apply {
    this.id = id
  }

  public fun index(index: Int?): ExprParameterBuilder = this.apply {
    this.index = index
  }

  public fun build(): Expr.Parameter = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Parameter =
      factory.exprParameter(id = id!!, index = index!!)
}

public class ExprUnaryBuilder {
  public var id: Int? = null

  public var op: Expr.Unary.Op? = null

  public var expr: Expr? = null

  public fun id(id: Int?): ExprUnaryBuilder = this.apply {
    this.id = id
  }

  public fun op(op: Expr.Unary.Op?): ExprUnaryBuilder = this.apply {
    this.op = op
  }

  public fun expr(expr: Expr?): ExprUnaryBuilder = this.apply {
    this.expr = expr
  }

  public fun build(): Expr.Unary = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Unary = factory.exprUnary(id =
      id!!, op = op!!, expr = expr!!)
}

public class ExprBinaryBuilder {
  public var id: Int? = null

  public var op: Expr.Binary.Op? = null

  public var lhs: Expr? = null

  public var rhs: Expr? = null

  public fun id(id: Int?): ExprBinaryBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Binary = factory.exprBinary(id =
      id!!, op = op!!, lhs = lhs!!, rhs = rhs!!)
}

public class ExprCollectionBuilder {
  public var id: Int? = null

  public var type: Expr.Collection.Type? = null

  public var values: MutableList<Expr> = mutableListOf()

  public fun id(id: Int?): ExprCollectionBuilder = this.apply {
    this.id = id
  }

  public fun type(type: Expr.Collection.Type?): ExprCollectionBuilder = this.apply {
    this.type = type
  }

  public fun values(values: MutableList<Expr>): ExprCollectionBuilder = this.apply {
    this.values = values
  }

  public fun build(): Expr.Collection = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Collection =
      factory.exprCollection(id = id!!, type = type!!, values = values)
}

public class ExprTupleBuilder {
  public var id: Int? = null

  public var fields: MutableList<Expr.Tuple.Field> = mutableListOf()

  public fun id(id: Int?): ExprTupleBuilder = this.apply {
    this.id = id
  }

  public fun fields(fields: MutableList<Expr.Tuple.Field>): ExprTupleBuilder = this.apply {
    this.fields = fields
  }

  public fun build(): Expr.Tuple = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Tuple = factory.exprTuple(id =
      id!!, fields = fields)
}

public class ExprTupleFieldBuilder {
  public var id: Int? = null

  public var name: Expr? = null

  public var `value`: Expr? = null

  public fun id(id: Int?): ExprTupleFieldBuilder = this.apply {
    this.id = id
  }

  public fun name(name: Expr?): ExprTupleFieldBuilder = this.apply {
    this.name = name
  }

  public fun `value`(`value`: Expr?): ExprTupleFieldBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Expr.Tuple.Field = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Tuple.Field =
      factory.exprTupleField(id = id!!, name = name!!, value = value!!)
}

public class ExprDateBuilder {
  public var id: Int? = null

  public var year: Long? = null

  public var month: Long? = null

  public var day: Long? = null

  public fun id(id: Int?): ExprDateBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Date = factory.exprDate(id =
      id!!, year = year!!, month = month!!, day = day!!)
}

public class ExprTimeBuilder {
  public var id: Int? = null

  public var hour: Long? = null

  public var minute: Long? = null

  public var second: Long? = null

  public var nano: Long? = null

  public var precision: Long? = null

  public var tzOffsetMinutes: Long? = null

  public fun id(id: Int?): ExprTimeBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Time = factory.exprTime(id =
      id!!, hour = hour!!, minute = minute!!, second = second!!, nano = nano!!, precision =
      precision!!, tzOffsetMinutes = tzOffsetMinutes)
}

public class ExprLikeBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var pattern: Expr? = null

  public var escape: Expr? = null

  public fun id(id: Int?): ExprLikeBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Like = factory.exprLike(id =
      id!!, value = value!!, pattern = pattern!!, escape = escape)
}

public class ExprBetweenBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var from: Expr? = null

  public var to: Expr? = null

  public fun id(id: Int?): ExprBetweenBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Between = factory.exprBetween(id
      = id!!, value = value!!, from = from!!, to = to!!)
}

public class ExprInCollectionBuilder {
  public var id: Int? = null

  public var lhs: Expr? = null

  public var rhs: Expr? = null

  public fun id(id: Int?): ExprInCollectionBuilder = this.apply {
    this.id = id
  }

  public fun lhs(lhs: Expr?): ExprInCollectionBuilder = this.apply {
    this.lhs = lhs
  }

  public fun rhs(rhs: Expr?): ExprInCollectionBuilder = this.apply {
    this.rhs = rhs
  }

  public fun build(): Expr.InCollection = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.InCollection =
      factory.exprInCollection(id = id!!, lhs = lhs!!, rhs = rhs!!)
}

public class ExprIsTypeBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var type: StaticType? = null

  public fun id(id: Int?): ExprIsTypeBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: Expr?): ExprIsTypeBuilder = this.apply {
    this.`value` = `value`
  }

  public fun type(type: StaticType?): ExprIsTypeBuilder = this.apply {
    this.type = type
  }

  public fun build(): Expr.IsType = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.IsType = factory.exprIsType(id =
      id!!, value = value!!, type = type!!)
}

public class ExprSwitchBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var branches: MutableList<Expr.Switch.Branch> = mutableListOf()

  public var default: Expr? = null

  public fun id(id: Int?): ExprSwitchBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Switch = factory.exprSwitch(id =
      id!!, expr = expr, branches = branches, default = default)
}

public class ExprSwitchBranchBuilder {
  public var id: Int? = null

  public var condition: Expr? = null

  public var expr: Expr? = null

  public fun id(id: Int?): ExprSwitchBranchBuilder = this.apply {
    this.id = id
  }

  public fun condition(condition: Expr?): ExprSwitchBranchBuilder = this.apply {
    this.condition = condition
  }

  public fun expr(expr: Expr?): ExprSwitchBranchBuilder = this.apply {
    this.expr = expr
  }

  public fun build(): Expr.Switch.Branch = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Switch.Branch =
      factory.exprSwitchBranch(id = id!!, condition = condition!!, expr = expr!!)
}

public class ExprCoalesceBuilder {
  public var id: Int? = null

  public var args: MutableList<Expr> = mutableListOf()

  public fun id(id: Int?): ExprCoalesceBuilder = this.apply {
    this.id = id
  }

  public fun args(args: MutableList<Expr>): ExprCoalesceBuilder = this.apply {
    this.args = args
  }

  public fun build(): Expr.Coalesce = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Coalesce =
      factory.exprCoalesce(id = id!!, args = args)
}

public class ExprNullIfBuilder {
  public var id: Int? = null

  public var expr1: Expr? = null

  public var expr2: Expr? = null

  public fun id(id: Int?): ExprNullIfBuilder = this.apply {
    this.id = id
  }

  public fun expr1(expr1: Expr?): ExprNullIfBuilder = this.apply {
    this.expr1 = expr1
  }

  public fun expr2(expr2: Expr?): ExprNullIfBuilder = this.apply {
    this.expr2 = expr2
  }

  public fun build(): Expr.NullIf = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.NullIf = factory.exprNullIf(id =
      id!!, expr1 = expr1!!, expr2 = expr2!!)
}

public class ExprCastBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var asType: StaticType? = null

  public fun id(id: Int?): ExprCastBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: Expr?): ExprCastBuilder = this.apply {
    this.`value` = `value`
  }

  public fun asType(asType: StaticType?): ExprCastBuilder = this.apply {
    this.asType = asType
  }

  public fun build(): Expr.Cast = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Cast = factory.exprCast(id =
      id!!, value = value!!, asType = asType!!)
}

public class ExprCanCastBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var asType: StaticType? = null

  public fun id(id: Int?): ExprCanCastBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: Expr?): ExprCanCastBuilder = this.apply {
    this.`value` = `value`
  }

  public fun asType(asType: StaticType?): ExprCanCastBuilder = this.apply {
    this.asType = asType
  }

  public fun build(): Expr.CanCast = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.CanCast = factory.exprCanCast(id
      = id!!, value = value!!, asType = asType!!)
}

public class ExprCanLosslessCastBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var asType: StaticType? = null

  public fun id(id: Int?): ExprCanLosslessCastBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: Expr?): ExprCanLosslessCastBuilder = this.apply {
    this.`value` = `value`
  }

  public fun asType(asType: StaticType?): ExprCanLosslessCastBuilder = this.apply {
    this.asType = asType
  }

  public fun build(): Expr.CanLosslessCast = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.CanLosslessCast =
      factory.exprCanLosslessCast(id = id!!, value = value!!, asType = asType!!)
}

public class ExprOuterBagOpBuilder {
  public var id: Int? = null

  public var op: Expr.OuterBagOp.Op? = null

  public var quantifier: SetQuantifier? = null

  public var lhs: Expr? = null

  public var rhs: Expr? = null

  public fun id(id: Int?): ExprOuterBagOpBuilder = this.apply {
    this.id = id
  }

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
      factory.exprOuterBagOp(id = id!!, op = op!!, quantifier = quantifier!!, lhs = lhs!!, rhs =
      rhs!!)
}

public class ExprSfwBuilder {
  public var id: Int? = null

  public var select: Select? = null

  public var from: From? = null

  public var let: Let? = null

  public var `where`: Expr? = null

  public var groupBy: GroupBy? = null

  public var having: Expr? = null

  public var orderBy: OrderBy? = null

  public var limit: Expr? = null

  public var offset: Expr? = null

  public fun id(id: Int?): ExprSfwBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Sfw = factory.exprSfw(id = id!!,
      select = select!!, from = from!!, let = let, where = where, groupBy = groupBy, having =
      having, orderBy = orderBy, limit = limit, offset = offset)
}

public class ExprMatchBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var pattern: GraphMatch? = null

  public fun id(id: Int?): ExprMatchBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): ExprMatchBuilder = this.apply {
    this.expr = expr
  }

  public fun pattern(pattern: GraphMatch?): ExprMatchBuilder = this.apply {
    this.pattern = pattern
  }

  public fun build(): Expr.Match = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Match = factory.exprMatch(id =
      id!!, expr = expr!!, pattern = pattern!!)
}

public class ExprWindowBuilder {
  public var id: Int? = null

  public var function: String? = null

  public var over: Over? = null

  public var args: MutableList<Expr> = mutableListOf()

  public fun id(id: Int?): ExprWindowBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Expr.Window = factory.exprWindow(id =
      id!!, function = function!!, over = over!!, args = args)
}

public class SelectStarBuilder {
  public var id: Int? = null

  public fun id(id: Int?): SelectStarBuilder = this.apply {
    this.id = id
  }

  public fun build(): Select.Star = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Star = factory.selectStar(id =
      id!!)
}

public class SelectProjectBuilder {
  public var id: Int? = null

  public var items: MutableList<Select.Project.Item> = mutableListOf()

  public fun id(id: Int?): SelectProjectBuilder = this.apply {
    this.id = id
  }

  public fun items(items: MutableList<Select.Project.Item>): SelectProjectBuilder = this.apply {
    this.items = items
  }

  public fun build(): Select.Project = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Project =
      factory.selectProject(id = id!!, items = items)
}

public class SelectProjectItemAllBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public fun id(id: Int?): SelectProjectItemAllBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): SelectProjectItemAllBuilder = this.apply {
    this.expr = expr
  }

  public fun build(): Select.Project.Item.All = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Project.Item.All =
      factory.selectProjectItemAll(id = id!!, expr = expr!!)
}

public class SelectProjectItemVarBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var asAlias: String? = null

  public fun id(id: Int?): SelectProjectItemVarBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): SelectProjectItemVarBuilder = this.apply {
    this.expr = expr
  }

  public fun asAlias(asAlias: String?): SelectProjectItemVarBuilder = this.apply {
    this.asAlias = asAlias
  }

  public fun build(): Select.Project.Item.Var = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Project.Item.Var =
      factory.selectProjectItemVar(id = id!!, expr = expr!!, asAlias = asAlias)
}

public class SelectPivotBuilder {
  public var id: Int? = null

  public var `value`: Expr? = null

  public var key: Expr? = null

  public fun id(id: Int?): SelectPivotBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: Expr?): SelectPivotBuilder = this.apply {
    this.`value` = `value`
  }

  public fun key(key: Expr?): SelectPivotBuilder = this.apply {
    this.key = key
  }

  public fun build(): Select.Pivot = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Pivot = factory.selectPivot(id
      = id!!, value = value!!, key = key!!)
}

public class SelectValueBuilder {
  public var id: Int? = null

  public var `constructor`: Expr? = null

  public fun id(id: Int?): SelectValueBuilder = this.apply {
    this.id = id
  }

  public fun `constructor`(`constructor`: Expr?): SelectValueBuilder = this.apply {
    this.`constructor` = `constructor`
  }

  public fun build(): Select.Value = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Select.Value = factory.selectValue(id
      = id!!, constructor = constructor!!)
}

public class FromCollectionBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var unpivot: Boolean? = null

  public var asAlias: String? = null

  public var atAlias: String? = null

  public var byAlias: String? = null

  public fun id(id: Int?): FromCollectionBuilder = this.apply {
    this.id = id
  }

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
      factory.fromCollection(id = id!!, expr = expr!!, unpivot = unpivot, asAlias = asAlias, atAlias
      = atAlias, byAlias = byAlias)
}

public class FromJoinBuilder {
  public var id: Int? = null

  public var type: From.Join.Type? = null

  public var condition: Expr? = null

  public var lhs: From? = null

  public var rhs: From? = null

  public fun id(id: Int?): FromJoinBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): From.Join = factory.fromJoin(id =
      id!!, type = type!!, condition = condition, lhs = lhs!!, rhs = rhs!!)
}

public class LetBuilder {
  public var id: Int? = null

  public fun id(id: Int?): LetBuilder = this.apply {
    this.id = id
  }

  public fun build(): Let = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Let = factory.let(id = id!!)
}

public class GroupByBuilder {
  public var id: Int? = null

  public var strategy: GroupBy.Strategy? = null

  public var keys: MutableList<GroupBy.Key> = mutableListOf()

  public var asAlias: String? = null

  public fun id(id: Int?): GroupByBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GroupBy = factory.groupBy(id = id!!,
      strategy = strategy!!, keys = keys, asAlias = asAlias)
}

public class GroupByKeyBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var asAlias: String? = null

  public fun id(id: Int?): GroupByKeyBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): GroupByKeyBuilder = this.apply {
    this.expr = expr
  }

  public fun asAlias(asAlias: String?): GroupByKeyBuilder = this.apply {
    this.asAlias = asAlias
  }

  public fun build(): GroupBy.Key = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GroupBy.Key = factory.groupByKey(id =
      id!!, expr = expr!!, asAlias = asAlias!!)
}

public class OrderByBuilder {
  public var id: Int? = null

  public var sorts: MutableList<OrderBy.Sort> = mutableListOf()

  public fun id(id: Int?): OrderByBuilder = this.apply {
    this.id = id
  }

  public fun sorts(sorts: MutableList<OrderBy.Sort>): OrderByBuilder = this.apply {
    this.sorts = sorts
  }

  public fun build(): OrderBy = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): OrderBy = factory.orderBy(id = id!!,
      sorts = sorts)
}

public class OrderBySortBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var dir: OrderBy.Sort.Dir? = null

  public var nulls: OrderBy.Sort.Nulls? = null

  public fun id(id: Int?): OrderBySortBuilder = this.apply {
    this.id = id
  }

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

  public fun build(factory: AstFactory = AstFactory.DEFAULT): OrderBy.Sort = factory.orderBySort(id
      = id!!, expr = expr!!, dir = dir!!, nulls = nulls!!)
}

public class GraphMatchBuilder {
  public var id: Int? = null

  public var patterns: MutableList<GraphMatch.Pattern> = mutableListOf()

  public var selector: GraphMatch.Selector? = null

  public fun id(id: Int?): GraphMatchBuilder = this.apply {
    this.id = id
  }

  public fun patterns(patterns: MutableList<GraphMatch.Pattern>): GraphMatchBuilder = this.apply {
    this.patterns = patterns
  }

  public fun selector(selector: GraphMatch.Selector?): GraphMatchBuilder = this.apply {
    this.selector = selector
  }

  public fun build(): GraphMatch = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch = factory.graphMatch(id =
      id!!, patterns = patterns, selector = selector)
}

public class GraphMatchPatternBuilder {
  public var id: Int? = null

  public var restrictor: GraphMatch.Restrictor? = null

  public var prefilter: Expr? = null

  public var variable: String? = null

  public var quantifier: GraphMatch.Quantifier? = null

  public var parts: MutableList<GraphMatch.Pattern.Part> = mutableListOf()

  public fun id(id: Int?): GraphMatchPatternBuilder = this.apply {
    this.id = id
  }

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
      factory.graphMatchPattern(id = id!!, restrictor = restrictor, prefilter = prefilter, variable
      = variable, quantifier = quantifier, parts = parts)
}

public class GraphMatchPatternPartNodeBuilder {
  public var id: Int? = null

  public var prefilter: Expr? = null

  public var variable: String? = null

  public var label: MutableList<String> = mutableListOf()

  public fun id(id: Int?): GraphMatchPatternPartNodeBuilder = this.apply {
    this.id = id
  }

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
      factory.graphMatchPatternPartNode(id = id!!, prefilter = prefilter, variable = variable, label
      = label)
}

public class GraphMatchPatternPartEdgeBuilder {
  public var id: Int? = null

  public var direction: GraphMatch.Direction? = null

  public var quantifier: GraphMatch.Quantifier? = null

  public var prefilter: Expr? = null

  public var variable: String? = null

  public var label: MutableList<String> = mutableListOf()

  public fun id(id: Int?): GraphMatchPatternPartEdgeBuilder = this.apply {
    this.id = id
  }

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
      factory.graphMatchPatternPartEdge(id = id!!, direction = direction!!, quantifier = quantifier,
      prefilter = prefilter, variable = variable, label = label)
}

public class GraphMatchQuantifierBuilder {
  public var id: Int? = null

  public var lower: Long? = null

  public var upper: Int? = null

  public fun id(id: Int?): GraphMatchQuantifierBuilder = this.apply {
    this.id = id
  }

  public fun lower(lower: Long?): GraphMatchQuantifierBuilder = this.apply {
    this.lower = lower
  }

  public fun upper(upper: Int?): GraphMatchQuantifierBuilder = this.apply {
    this.upper = upper
  }

  public fun build(): GraphMatch.Quantifier = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Quantifier =
      factory.graphMatchQuantifier(id = id!!, lower = lower!!, upper = upper)
}

public class GraphMatchSelectorAnyShortestBuilder {
  public var id: Int? = null

  public fun id(id: Int?): GraphMatchSelectorAnyShortestBuilder = this.apply {
    this.id = id
  }

  public fun build(): GraphMatch.Selector.AnyShortest = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.AnyShortest =
      factory.graphMatchSelectorAnyShortest(id = id!!)
}

public class GraphMatchSelectorAllShortestBuilder {
  public var id: Int? = null

  public fun id(id: Int?): GraphMatchSelectorAllShortestBuilder = this.apply {
    this.id = id
  }

  public fun build(): GraphMatch.Selector.AllShortest = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.AllShortest =
      factory.graphMatchSelectorAllShortest(id = id!!)
}

public class GraphMatchSelectorAnyBuilder {
  public var id: Int? = null

  public fun id(id: Int?): GraphMatchSelectorAnyBuilder = this.apply {
    this.id = id
  }

  public fun build(): GraphMatch.Selector.Any = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.Any =
      factory.graphMatchSelectorAny(id = id!!)
}

public class GraphMatchSelectorAnyKBuilder {
  public var id: Int? = null

  public var k: Long? = null

  public fun id(id: Int?): GraphMatchSelectorAnyKBuilder = this.apply {
    this.id = id
  }

  public fun k(k: Long?): GraphMatchSelectorAnyKBuilder = this.apply {
    this.k = k
  }

  public fun build(): GraphMatch.Selector.AnyK = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.AnyK =
      factory.graphMatchSelectorAnyK(id = id!!, k = k!!)
}

public class GraphMatchSelectorShortestKBuilder {
  public var id: Int? = null

  public var k: Long? = null

  public fun id(id: Int?): GraphMatchSelectorShortestKBuilder = this.apply {
    this.id = id
  }

  public fun k(k: Long?): GraphMatchSelectorShortestKBuilder = this.apply {
    this.k = k
  }

  public fun build(): GraphMatch.Selector.ShortestK = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.ShortestK =
      factory.graphMatchSelectorShortestK(id = id!!, k = k!!)
}

public class GraphMatchSelectorShortestKGroupBuilder {
  public var id: Int? = null

  public var k: Long? = null

  public fun id(id: Int?): GraphMatchSelectorShortestKGroupBuilder = this.apply {
    this.id = id
  }

  public fun k(k: Long?): GraphMatchSelectorShortestKGroupBuilder = this.apply {
    this.k = k
  }

  public fun build(): GraphMatch.Selector.ShortestKGroup = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): GraphMatch.Selector.ShortestKGroup =
      factory.graphMatchSelectorShortestKGroup(id = id!!, k = k!!)
}

public class OverBuilder {
  public var id: Int? = null

  public var partitions: MutableList<Expr> = mutableListOf()

  public var sorts: MutableList<OrderBy.Sort> = mutableListOf()

  public fun id(id: Int?): OverBuilder = this.apply {
    this.id = id
  }

  public fun partitions(partitions: MutableList<Expr>): OverBuilder = this.apply {
    this.partitions = partitions
  }

  public fun sorts(sorts: MutableList<OrderBy.Sort>): OverBuilder = this.apply {
    this.sorts = sorts
  }

  public fun build(): Over = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Over = factory.over(id = id!!,
      partitions = partitions, sorts = sorts)
}

public class OnConflictBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public var action: OnConflict.Action? = null

  public fun id(id: Int?): OnConflictBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): OnConflictBuilder = this.apply {
    this.expr = expr
  }

  public fun action(action: OnConflict.Action?): OnConflictBuilder = this.apply {
    this.action = action
  }

  public fun build(): OnConflict = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict = factory.onConflict(id =
      id!!, expr = expr!!, action = action!!)
}

public class OnConflictActionDoReplaceBuilder {
  public var id: Int? = null

  public var `value`: OnConflict.Value? = null

  public fun id(id: Int?): OnConflictActionDoReplaceBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: OnConflict.Value?): OnConflictActionDoReplaceBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): OnConflict.Action.DoReplace = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict.Action.DoReplace =
      factory.onConflictActionDoReplace(id = id!!, value = value!!)
}

public class OnConflictActionDoUpdateBuilder {
  public var id: Int? = null

  public var `value`: OnConflict.Value? = null

  public fun id(id: Int?): OnConflictActionDoUpdateBuilder = this.apply {
    this.id = id
  }

  public fun `value`(`value`: OnConflict.Value?): OnConflictActionDoUpdateBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): OnConflict.Action.DoUpdate = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict.Action.DoUpdate =
      factory.onConflictActionDoUpdate(id = id!!, value = value!!)
}

public class OnConflictActionDoNothingBuilder {
  public var id: Int? = null

  public fun id(id: Int?): OnConflictActionDoNothingBuilder = this.apply {
    this.id = id
  }

  public fun build(): OnConflict.Action.DoNothing = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): OnConflict.Action.DoNothing =
      factory.onConflictActionDoNothing(id = id!!)
}

public class ReturningBuilder {
  public var id: Int? = null

  public var columns: MutableList<Returning.Column> = mutableListOf()

  public fun id(id: Int?): ReturningBuilder = this.apply {
    this.id = id
  }

  public fun columns(columns: MutableList<Returning.Column>): ReturningBuilder = this.apply {
    this.columns = columns
  }

  public fun build(): Returning = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning = factory.returning(id =
      id!!, columns = columns)
}

public class ReturningColumnBuilder {
  public var id: Int? = null

  public var status: Returning.Column.Status? = null

  public var age: Returning.Column.Age? = null

  public var `value`: Returning.Column.Value? = null

  public fun id(id: Int?): ReturningColumnBuilder = this.apply {
    this.id = id
  }

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
      factory.returningColumn(id = id!!, status = status!!, age = age!!, value = value!!)
}

public class ReturningColumnValueWildcardBuilder {
  public var id: Int? = null

  public fun id(id: Int?): ReturningColumnValueWildcardBuilder = this.apply {
    this.id = id
  }

  public fun build(): Returning.Column.Value.Wildcard = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning.Column.Value.Wildcard =
      factory.returningColumnValueWildcard(id = id!!)
}

public class ReturningColumnValueExpressionBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public fun id(id: Int?): ReturningColumnValueExpressionBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): ReturningColumnValueExpressionBuilder = this.apply {
    this.expr = expr
  }

  public fun build(): Returning.Column.Value.Expression = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): Returning.Column.Value.Expression =
      factory.returningColumnValueExpression(id = id!!, expr = expr!!)
}

public class TableDefinitionBuilder {
  public var id: Int? = null

  public var columns: MutableList<TableDefinition.Column> = mutableListOf()

  public fun id(id: Int?): TableDefinitionBuilder = this.apply {
    this.id = id
  }

  public fun columns(columns: MutableList<TableDefinition.Column>): TableDefinitionBuilder =
      this.apply {
    this.columns = columns
  }

  public fun build(): TableDefinition = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): TableDefinition =
      factory.tableDefinition(id = id!!, columns = columns)
}

public class TableDefinitionColumnBuilder {
  public var id: Int? = null

  public var name: String? = null

  public var type: StaticType? = null

  public var constraints: MutableList<TableDefinition.Column.Constraint> = mutableListOf()

  public fun id(id: Int?): TableDefinitionColumnBuilder = this.apply {
    this.id = id
  }

  public fun name(name: String?): TableDefinitionColumnBuilder = this.apply {
    this.name = name
  }

  public fun type(type: StaticType?): TableDefinitionColumnBuilder = this.apply {
    this.type = type
  }

  public fun constraints(constraints: MutableList<TableDefinition.Column.Constraint>):
      TableDefinitionColumnBuilder = this.apply {
    this.constraints = constraints
  }

  public fun build(): TableDefinition.Column = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): TableDefinition.Column =
      factory.tableDefinitionColumn(id = id!!, name = name!!, type = type!!, constraints =
      constraints)
}

public class TableDefinitionColumnConstraintBuilder {
  public var id: Int? = null

  public var name: String? = null

  public var body: TableDefinition.Column.Constraint.Body? = null

  public fun id(id: Int?): TableDefinitionColumnConstraintBuilder = this.apply {
    this.id = id
  }

  public fun name(name: String?): TableDefinitionColumnConstraintBuilder = this.apply {
    this.name = name
  }

  public fun body(body: TableDefinition.Column.Constraint.Body?):
      TableDefinitionColumnConstraintBuilder = this.apply {
    this.body = body
  }

  public fun build(): TableDefinition.Column.Constraint = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT): TableDefinition.Column.Constraint =
      factory.tableDefinitionColumnConstraint(id = id!!, name = name, body = body!!)
}

public class TableDefinitionColumnConstraintBodyNullableBuilder {
  public var id: Int? = null

  public fun id(id: Int?): TableDefinitionColumnConstraintBodyNullableBuilder = this.apply {
    this.id = id
  }

  public fun build(): TableDefinition.Column.Constraint.Body.Nullable = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT):
      TableDefinition.Column.Constraint.Body.Nullable =
      factory.tableDefinitionColumnConstraintBodyNullable(id = id!!)
}

public class TableDefinitionColumnConstraintBodyNotNullBuilder {
  public var id: Int? = null

  public fun id(id: Int?): TableDefinitionColumnConstraintBodyNotNullBuilder = this.apply {
    this.id = id
  }

  public fun build(): TableDefinition.Column.Constraint.Body.NotNull = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT):
      TableDefinition.Column.Constraint.Body.NotNull =
      factory.tableDefinitionColumnConstraintBodyNotNull(id = id!!)
}

public class TableDefinitionColumnConstraintBodyCheckBuilder {
  public var id: Int? = null

  public var expr: Expr? = null

  public fun id(id: Int?): TableDefinitionColumnConstraintBodyCheckBuilder = this.apply {
    this.id = id
  }

  public fun expr(expr: Expr?): TableDefinitionColumnConstraintBodyCheckBuilder = this.apply {
    this.expr = expr
  }

  public fun build(): TableDefinition.Column.Constraint.Body.Check = build(AstFactory.DEFAULT)

  public fun build(factory: AstFactory = AstFactory.DEFAULT):
      TableDefinition.Column.Constraint.Body.Check =
      factory.tableDefinitionColumnConstraintBodyCheck(id = id!!, expr = expr!!)
}

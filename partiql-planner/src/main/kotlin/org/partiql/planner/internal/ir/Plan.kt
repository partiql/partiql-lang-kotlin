@file:JvmName("Plan")
@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir

import org.partiql.types.StaticType
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal fun partiQLPlan(
    version: PartiQLVersion,
    globals: List<Global>,
    statement: Statement,
): PartiQLPlan = PartiQLPlan(version, globals, statement)

internal fun global(path: Identifier.Qualified, type: StaticType): Global = Global(path, type)

internal fun fnResolved(signature: FunctionSignature.Scalar): Fn.Resolved = Fn.Resolved(signature)

internal fun fnUnresolved(identifier: Identifier, isHidden: Boolean): Fn.Unresolved =
    Fn.Unresolved(identifier, isHidden)

internal fun fnDynamic(signatures: List<FunctionSignature.Scalar>): Fn.Dynamic =
    Fn.Dynamic(signatures)

internal fun aggResolved(signature: FunctionSignature.Aggregation): Agg.Resolved =
    Agg.Resolved(signature)

internal fun aggUnresolved(identifier: Identifier): Agg.Unresolved = Agg.Unresolved(identifier)

internal fun statementQuery(root: Rex): Statement.Query = Statement.Query(root)

internal fun identifierSymbol(symbol: String, caseSensitivity: Identifier.CaseSensitivity):
    Identifier.Symbol = Identifier.Symbol(symbol, caseSensitivity)

internal fun identifierQualified(root: Identifier.Symbol, steps: List<Identifier.Symbol>):
    Identifier.Qualified = Identifier.Qualified(root, steps)

internal fun rex(type: StaticType, op: Rex.Op): Rex = Rex(type, op)

internal fun rexOpLit(`value`: PartiQLValue): Rex.Op.Lit = Rex.Op.Lit(value)

internal fun rexOpVarResolved(ref: Int): Rex.Op.Var.Resolved = Rex.Op.Var.Resolved(ref)

internal fun rexOpVarUnresolved(identifier: Identifier, scope: Rex.Op.Var.Scope):
    Rex.Op.Var.Unresolved = Rex.Op.Var.Unresolved(identifier, scope)

internal fun rexOpGlobal(ref: Int): Rex.Op.Global = Rex.Op.Global(ref)

internal fun rexOpPath(root: Rex, steps: List<Rex.Op.Path.Step>): Rex.Op.Path = Rex.Op.Path(
    root,
    steps
)

internal fun rexOpPathStepIndex(key: Rex): Rex.Op.Path.Step.Index = Rex.Op.Path.Step.Index(key)

internal fun rexOpPathStepSymbol(identifier: Identifier.Symbol): Rex.Op.Path.Step.Symbol =
    Rex.Op.Path.Step.Symbol(identifier)

internal fun rexOpPathStepWildcard(): Rex.Op.Path.Step.Wildcard = Rex.Op.Path.Step.Wildcard()

internal fun rexOpPathStepUnpivot(): Rex.Op.Path.Step.Unpivot = Rex.Op.Path.Step.Unpivot()

internal fun rexOpCallStatic(fn: Fn, args: List<Rex>): Rex.Op.Call.Static = Rex.Op.Call.Static(
    fn,
    args
)

internal fun rexOpCallDynamic(args: List<Rex>, candidates: List<Rex.Op.Call.Dynamic.Candidate>):
    Rex.Op.Call.Dynamic = Rex.Op.Call.Dynamic(args, candidates)

internal fun rexOpCallDynamicCandidate(fn: Fn.Resolved, coercions: List<Fn.Resolved?>):
    Rex.Op.Call.Dynamic.Candidate = Rex.Op.Call.Dynamic.Candidate(fn, coercions)

internal fun rexOpCase(branches: List<Rex.Op.Case.Branch>, default: Rex): Rex.Op.Case =
    Rex.Op.Case(branches, default)

internal fun rexOpCaseBranch(condition: Rex, rex: Rex): Rex.Op.Case.Branch =
    Rex.Op.Case.Branch(condition, rex)

internal fun rexOpCollection(values: List<Rex>): Rex.Op.Collection = Rex.Op.Collection(values)

internal fun rexOpStruct(fields: List<Rex.Op.Struct.Field>): Rex.Op.Struct = Rex.Op.Struct(fields)

internal fun rexOpStructField(k: Rex, v: Rex): Rex.Op.Struct.Field = Rex.Op.Struct.Field(k, v)

internal fun rexOpPivot(
    key: Rex,
    `value`: Rex,
    rel: Rel,
): Rex.Op.Pivot = Rex.Op.Pivot(key, value, rel)

internal fun rexOpSubquery(select: Rex.Op.Select, coercion: Rex.Op.Subquery.Coercion): Rex.Op.Subquery =
    Rex.Op.Subquery(select, coercion)

internal fun rexOpSelect(`constructor`: Rex, rel: Rel): Rex.Op.Select = Rex.Op.Select(
    constructor,
    rel
)

internal fun rexOpTupleUnion(args: List<Rex>): Rex.Op.TupleUnion = Rex.Op.TupleUnion(args)

internal fun rexOpErr(message: String): Rex.Op.Err = Rex.Op.Err(message)

internal fun rel(type: Rel.Type, op: Rel.Op): Rel = Rel(type, op)

internal fun relType(schema: List<Rel.Binding>, props: Set<Rel.Prop>): Rel.Type = Rel.Type(
    schema,
    props
)

internal fun relOpScan(rex: Rex): Rel.Op.Scan = Rel.Op.Scan(rex)

internal fun relOpScanIndexed(rex: Rex): Rel.Op.ScanIndexed = Rel.Op.ScanIndexed(rex)

internal fun relOpUnpivot(rex: Rex): Rel.Op.Unpivot = Rel.Op.Unpivot(rex)

internal fun relOpDistinct(input: Rel): Rel.Op.Distinct = Rel.Op.Distinct(input)

internal fun relOpFilter(input: Rel, predicate: Rex): Rel.Op.Filter = Rel.Op.Filter(input, predicate)

internal fun relOpSort(input: Rel, specs: List<Rel.Op.Sort.Spec>): Rel.Op.Sort = Rel.Op.Sort(
    input,
    specs
)

internal fun relOpSortSpec(rex: Rex, order: Rel.Op.Sort.Order): Rel.Op.Sort.Spec =
    Rel.Op.Sort.Spec(rex, order)

internal fun relOpUnion(lhs: Rel, rhs: Rel): Rel.Op.Union = Rel.Op.Union(lhs, rhs)

internal fun relOpIntersect(lhs: Rel, rhs: Rel): Rel.Op.Intersect = Rel.Op.Intersect(lhs, rhs)

internal fun relOpExcept(lhs: Rel, rhs: Rel): Rel.Op.Except = Rel.Op.Except(lhs, rhs)

internal fun relOpLimit(input: Rel, limit: Rex): Rel.Op.Limit = Rel.Op.Limit(input, limit)

internal fun relOpOffset(input: Rel, offset: Rex): Rel.Op.Offset = Rel.Op.Offset(input, offset)

internal fun relOpProject(input: Rel, projections: List<Rex>): Rel.Op.Project = Rel.Op.Project(
    input,
    projections
)

internal fun relOpJoin(
    lhs: Rel,
    rhs: Rel,
    rex: Rex,
    type: Rel.Op.Join.Type,
): Rel.Op.Join = Rel.Op.Join(lhs, rhs, rex, type)

internal fun relOpAggregate(
    input: Rel,
    strategy: Rel.Op.Aggregate.Strategy,
    calls: List<Rel.Op.Aggregate.Call>,
    groups: List<Rex>,
): Rel.Op.Aggregate = Rel.Op.Aggregate(input, strategy, calls, groups)

internal fun relOpAggregateCall(agg: Agg, args: List<Rex>): Rel.Op.Aggregate.Call =
    Rel.Op.Aggregate.Call(agg, args)

internal fun relOpExclude(input: Rel, items: List<Rel.Op.Exclude.Item>): Rel.Op.Exclude =
    Rel.Op.Exclude(input, items)

internal fun relOpExcludeItem(root: Identifier.Symbol, steps: List<Rel.Op.Exclude.Step>):
    Rel.Op.Exclude.Item = Rel.Op.Exclude.Item(root, steps)

internal fun relOpExcludeStepAttr(symbol: Identifier.Symbol): Rel.Op.Exclude.Step.Attr =
    Rel.Op.Exclude.Step.Attr(symbol)

internal fun relOpExcludeStepPos(index: Int): Rel.Op.Exclude.Step.Pos = Rel.Op.Exclude.Step.Pos(index)

internal fun relOpExcludeStepStructWildcard(): Rel.Op.Exclude.Step.StructWildcard =
    Rel.Op.Exclude.Step.StructWildcard()

internal fun relOpExcludeStepCollectionWildcard(): Rel.Op.Exclude.Step.CollectionWildcard =
    Rel.Op.Exclude.Step.CollectionWildcard()

internal fun relOpErr(message: String): Rel.Op.Err = Rel.Op.Err(message)

internal fun relBinding(name: String, type: StaticType): Rel.Binding = Rel.Binding(name, type)

@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir.builder

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Global
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.PartiQLVersion
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.types.StaticType
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class PartiQlPlanBuilder(
    internal var version: PartiQLVersion? = null,
    internal var globals: MutableList<Global> = mutableListOf(),
    internal var statement: Statement? = null,
) {
    internal fun version(version: PartiQLVersion?): PartiQlPlanBuilder = this.apply {
        this.version = version
    }

    internal fun globals(globals: MutableList<Global>): PartiQlPlanBuilder = this.apply {
        this.globals = globals
    }

    internal fun statement(statement: Statement?): PartiQlPlanBuilder = this.apply {
        this.statement = statement
    }

    internal fun build(): PartiQLPlan = PartiQLPlan(
        version = version!!, globals = globals,
        statement =
        statement!!
    )
}

internal class GlobalBuilder(
    internal var path: Identifier.Qualified? = null,
    internal var type: StaticType? = null,
) {
    internal fun path(path: Identifier.Qualified?): GlobalBuilder = this.apply {
        this.path = path
    }

    internal fun type(type: StaticType?): GlobalBuilder = this.apply {
        this.type = type
    }

    internal fun build(): Global = Global(path = path!!, type = type!!)
}

internal class FnResolvedBuilder(
    internal var signature: FunctionSignature.Scalar? = null,
) {
    internal fun signature(signature: FunctionSignature.Scalar?): FnResolvedBuilder = this.apply {
        this.signature = signature
    }

    internal fun build(): Fn.Resolved = Fn.Resolved(signature = signature!!)
}

internal class FnUnresolvedBuilder(
    internal var identifier: Identifier? = null,
    internal var isHidden: Boolean? = null,
) {
    internal fun identifier(identifier: Identifier?): FnUnresolvedBuilder = this.apply {
        this.identifier = identifier
    }

    internal fun isHidden(isHidden: Boolean?): FnUnresolvedBuilder = this.apply {
        this.isHidden = isHidden
    }

    internal fun build(): Fn.Unresolved = Fn.Unresolved(
        identifier = identifier!!,
        isHidden =
        isHidden!!
    )
}

internal class AggResolvedBuilder(
    internal var signature: FunctionSignature.Aggregation? = null,
) {
    internal fun signature(signature: FunctionSignature.Aggregation?): AggResolvedBuilder = this.apply {
        this.signature = signature
    }

    internal fun build(): Agg.Resolved = Agg.Resolved(signature = signature!!)
}

internal class AggUnresolvedBuilder(
    internal var identifier: Identifier? = null,
) {
    internal fun identifier(identifier: Identifier?): AggUnresolvedBuilder = this.apply {
        this.identifier = identifier
    }

    internal fun build(): Agg.Unresolved = Agg.Unresolved(identifier = identifier!!)
}

internal class StatementQueryBuilder(
    internal var root: Rex? = null,
) {
    internal fun root(root: Rex?): StatementQueryBuilder = this.apply {
        this.root = root
    }

    internal fun build(): Statement.Query = Statement.Query(root = root!!)
}

internal class IdentifierSymbolBuilder(
    internal var symbol: String? = null,
    internal var caseSensitivity: Identifier.CaseSensitivity? = null,
) {
    internal fun symbol(symbol: String?): IdentifierSymbolBuilder = this.apply {
        this.symbol = symbol
    }

    internal fun caseSensitivity(caseSensitivity: Identifier.CaseSensitivity?): IdentifierSymbolBuilder = this.apply {
        this.caseSensitivity = caseSensitivity
    }

    internal fun build(): Identifier.Symbol = Identifier.Symbol(
        symbol = symbol!!,
        caseSensitivity =
        caseSensitivity!!
    )
}

internal class IdentifierQualifiedBuilder(
    internal var root: Identifier.Symbol? = null,
    internal var steps: MutableList<Identifier.Symbol> = mutableListOf(),
) {
    internal fun root(root: Identifier.Symbol?): IdentifierQualifiedBuilder = this.apply {
        this.root = root
    }

    internal fun steps(steps: MutableList<Identifier.Symbol>): IdentifierQualifiedBuilder = this.apply {
        this.steps = steps
    }

    internal fun build(): Identifier.Qualified = Identifier.Qualified(root = root!!, steps = steps)
}

internal class RexBuilder(
    internal var type: StaticType? = null,
    internal var op: Rex.Op? = null,
) {
    internal fun type(type: StaticType?): RexBuilder = this.apply {
        this.type = type
    }

    internal fun op(op: Rex.Op?): RexBuilder = this.apply {
        this.op = op
    }

    internal fun build(): Rex = Rex(type = type!!, op = op!!)
}

internal class RexOpLitBuilder(
    internal var `value`: PartiQLValue? = null,
) {
    @OptIn(PartiQLValueExperimental::class)
    internal fun `value`(`value`: PartiQLValue?): RexOpLitBuilder = this.apply {
        this.`value` = `value`
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun build(): Rex.Op.Lit = Rex.Op.Lit(value = value!!)
}

internal class RexOpVarResolvedBuilder(
    internal var ref: Int? = null,
) {
    internal fun ref(ref: Int?): RexOpVarResolvedBuilder = this.apply {
        this.ref = ref
    }

    internal fun build(): Rex.Op.Var.Resolved = Rex.Op.Var.Resolved(ref = ref!!)
}

internal class RexOpVarUnresolvedBuilder(
    internal var identifier: Identifier? = null,
    internal var scope: Rex.Op.Var.Scope? = null,
) {
    internal fun identifier(identifier: Identifier?): RexOpVarUnresolvedBuilder = this.apply {
        this.identifier = identifier
    }

    internal fun scope(scope: Rex.Op.Var.Scope?): RexOpVarUnresolvedBuilder = this.apply {
        this.scope = scope
    }

    internal fun build(): Rex.Op.Var.Unresolved = Rex.Op.Var.Unresolved(
        identifier = identifier!!,
        scope =
        scope!!
    )
}

internal class RexOpGlobalBuilder(
    internal var ref: Int? = null,
) {
    internal fun ref(ref: Int?): RexOpGlobalBuilder = this.apply {
        this.ref = ref
    }

    internal fun build(): Rex.Op.Global = Rex.Op.Global(ref = ref!!)
}

internal class RexOpPathBuilder(
    internal var root: Rex? = null,
    internal var steps: MutableList<Rex.Op.Path.Step> = mutableListOf(),
) {
    internal fun root(root: Rex?): RexOpPathBuilder = this.apply {
        this.root = root
    }

    internal fun steps(steps: MutableList<Rex.Op.Path.Step>): RexOpPathBuilder = this.apply {
        this.steps = steps
    }

    internal fun build(): Rex.Op.Path = Rex.Op.Path(root = root!!, steps = steps)
}

internal class RexOpPathStepIndexBuilder(
    internal var key: Rex? = null,
) {
    internal fun key(key: Rex?): RexOpPathStepIndexBuilder = this.apply {
        this.key = key
    }

    internal fun build(): Rex.Op.Path.Step.Index = Rex.Op.Path.Step.Index(key = key!!)
}

internal class RexOpPathStepSymbolBuilder(
    internal var identifier: Identifier.Symbol? = null,
) {
    internal fun identifier(identifier: Identifier.Symbol?): RexOpPathStepSymbolBuilder = this.apply {
        this.identifier = identifier
    }

    internal fun build(): Rex.Op.Path.Step.Symbol = Rex.Op.Path.Step.Symbol(identifier = identifier!!)
}

internal class RexOpPathStepWildcardBuilder() {
    internal fun build(): Rex.Op.Path.Step.Wildcard = Rex.Op.Path.Step.Wildcard()
}

internal class RexOpPathStepUnpivotBuilder() {
    internal fun build(): Rex.Op.Path.Step.Unpivot = Rex.Op.Path.Step.Unpivot()
}

internal class RexOpCallStaticBuilder(
    internal var fn: Fn? = null,
    internal var args: MutableList<Rex> = mutableListOf(),
) {
    internal fun fn(fn: Fn?): RexOpCallStaticBuilder = this.apply {
        this.fn = fn
    }

    internal fun args(args: MutableList<Rex>): RexOpCallStaticBuilder = this.apply {
        this.args = args
    }

    internal fun build(): Rex.Op.Call.Static = Rex.Op.Call.Static(fn = fn!!, args = args)
}

internal class RexOpCallDynamicBuilder(
    internal var args: MutableList<Rex> = mutableListOf(),
    internal var candidates: MutableList<Rex.Op.Call.Dynamic.Candidate> = mutableListOf(),
) {
    internal fun args(args: MutableList<Rex>): RexOpCallDynamicBuilder = this.apply {
        this.args = args
    }

    internal fun candidates(candidates: MutableList<Rex.Op.Call.Dynamic.Candidate>):
        RexOpCallDynamicBuilder = this.apply {
        this.candidates = candidates
    }

    internal fun build(): Rex.Op.Call.Dynamic = Rex.Op.Call.Dynamic(
        args = args,
        candidates =
        candidates
    )
}

internal class RexOpCallDynamicCandidateBuilder(
    internal var fn: Fn.Resolved? = null,
    internal var coercions: MutableList<Fn.Resolved?> = mutableListOf(),
) {
    internal fun fn(fn: Fn.Resolved?): RexOpCallDynamicCandidateBuilder = this.apply {
        this.fn = fn
    }

    internal fun coercions(coercions: MutableList<Fn.Resolved?>): RexOpCallDynamicCandidateBuilder =
        this.apply {
            this.coercions = coercions
        }

    internal fun build(): Rex.Op.Call.Dynamic.Candidate = Rex.Op.Call.Dynamic.Candidate(
        fn = fn!!,
        coercions = coercions
    )
}

internal class RexOpCaseBuilder(
    internal var branches: MutableList<Rex.Op.Case.Branch> = mutableListOf(),
    internal var default: Rex? = null,
) {
    internal fun branches(branches: MutableList<Rex.Op.Case.Branch>): RexOpCaseBuilder = this.apply {
        this.branches = branches
    }

    internal fun default(default: Rex?): RexOpCaseBuilder = this.apply {
        this.default = default
    }

    internal fun build(): Rex.Op.Case = Rex.Op.Case(branches = branches, default = default!!)
}

internal class RexOpCaseBranchBuilder(
    internal var condition: Rex? = null,
    internal var rex: Rex? = null,
) {
    internal fun condition(condition: Rex?): RexOpCaseBranchBuilder = this.apply {
        this.condition = condition
    }

    internal fun rex(rex: Rex?): RexOpCaseBranchBuilder = this.apply {
        this.rex = rex
    }

    internal fun build(): Rex.Op.Case.Branch = Rex.Op.Case.Branch(condition = condition!!, rex = rex!!)
}

internal class RexOpCollectionBuilder(
    internal var values: MutableList<Rex> = mutableListOf(),
) {
    internal fun values(values: MutableList<Rex>): RexOpCollectionBuilder = this.apply {
        this.values = values
    }

    internal fun build(): Rex.Op.Collection = Rex.Op.Collection(values = values)
}

internal class RexOpStructBuilder(
    internal var fields: MutableList<Rex.Op.Struct.Field> = mutableListOf(),
) {
    internal fun fields(fields: MutableList<Rex.Op.Struct.Field>): RexOpStructBuilder = this.apply {
        this.fields = fields
    }

    internal fun build(): Rex.Op.Struct = Rex.Op.Struct(fields = fields)
}

internal class RexOpStructFieldBuilder(
    internal var k: Rex? = null,
    internal var v: Rex? = null,
) {
    internal fun k(k: Rex?): RexOpStructFieldBuilder = this.apply {
        this.k = k
    }

    internal fun v(v: Rex?): RexOpStructFieldBuilder = this.apply {
        this.v = v
    }

    internal fun build(): Rex.Op.Struct.Field = Rex.Op.Struct.Field(k = k!!, v = v!!)
}

internal class RexOpPivotBuilder(
    internal var key: Rex? = null,
    internal var `value`: Rex? = null,
    internal var rel: Rel? = null,
) {
    internal fun key(key: Rex?): RexOpPivotBuilder = this.apply {
        this.key = key
    }

    internal fun `value`(`value`: Rex?): RexOpPivotBuilder = this.apply {
        this.`value` = `value`
    }

    internal fun rel(rel: Rel?): RexOpPivotBuilder = this.apply {
        this.rel = rel
    }

    internal fun build(): Rex.Op.Pivot = Rex.Op.Pivot(key = key!!, value = value!!, rel = rel!!)
}

internal class RexOpSubqueryBuilder(
    internal var select: Rex.Op.Select? = null,
    internal var coercion: Rex.Op.Subquery.Coercion? = null,
) {
    internal fun select(select: Rex.Op.Select?): RexOpSubqueryBuilder = this.apply {
        this.select = select
    }

    internal fun coercion(coercion: Rex.Op.Subquery.Coercion?): RexOpSubqueryBuilder = this.apply {
        this.coercion = coercion
    }

    internal fun build(): Rex.Op.Subquery = Rex.Op.Subquery(select = select!!, coercion = coercion!!)
}

internal class RexOpSelectBuilder(
    internal var `constructor`: Rex? = null,
    internal var rel: Rel? = null,
) {
    internal fun `constructor`(`constructor`: Rex?): RexOpSelectBuilder = this.apply {
        this.`constructor` = `constructor`
    }

    internal fun rel(rel: Rel?): RexOpSelectBuilder = this.apply {
        this.rel = rel
    }

    internal fun build(): Rex.Op.Select = Rex.Op.Select(constructor = constructor!!, rel = rel!!)
}

internal class RexOpTupleUnionBuilder(
    internal var args: MutableList<Rex> = mutableListOf(),
) {
    internal fun args(args: MutableList<Rex>): RexOpTupleUnionBuilder = this.apply {
        this.args = args
    }

    internal fun build(): Rex.Op.TupleUnion = Rex.Op.TupleUnion(args = args)
}

internal class RexOpErrBuilder(
    internal var message: String? = null,
) {
    internal fun message(message: String?): RexOpErrBuilder = this.apply {
        this.message = message
    }

    internal fun build(): Rex.Op.Err = Rex.Op.Err(message = message!!)
}

internal class RelBuilder(
    internal var type: Rel.Type? = null,
    internal var op: Rel.Op? = null,
) {
    internal fun type(type: Rel.Type?): RelBuilder = this.apply {
        this.type = type
    }

    internal fun op(op: Rel.Op?): RelBuilder = this.apply {
        this.op = op
    }

    internal fun build(): Rel = Rel(type = type!!, op = op!!)
}

internal class RelTypeBuilder(
    internal var schema: MutableList<Rel.Binding> = mutableListOf(),
    internal var props: MutableSet<Rel.Prop> = mutableSetOf(),
) {
    internal fun schema(schema: MutableList<Rel.Binding>): RelTypeBuilder = this.apply {
        this.schema = schema
    }

    internal fun props(props: MutableSet<Rel.Prop>): RelTypeBuilder = this.apply {
        this.props = props
    }

    internal fun build(): Rel.Type = Rel.Type(schema = schema, props = props)
}

internal class RelOpScanBuilder(
    internal var rex: Rex? = null,
) {
    internal fun rex(rex: Rex?): RelOpScanBuilder = this.apply {
        this.rex = rex
    }

    internal fun build(): Rel.Op.Scan = Rel.Op.Scan(rex = rex!!)
}

internal class RelOpScanIndexedBuilder(
    internal var rex: Rex? = null,
) {
    internal fun rex(rex: Rex?): RelOpScanIndexedBuilder = this.apply {
        this.rex = rex
    }

    internal fun build(): Rel.Op.ScanIndexed = Rel.Op.ScanIndexed(rex = rex!!)
}

internal class RelOpUnpivotBuilder(
    internal var rex: Rex? = null,
) {
    internal fun rex(rex: Rex?): RelOpUnpivotBuilder = this.apply {
        this.rex = rex
    }

    internal fun build(): Rel.Op.Unpivot = Rel.Op.Unpivot(rex = rex!!)
}

internal class RelOpDistinctBuilder(
    internal var input: Rel? = null,
) {
    internal fun input(input: Rel?): RelOpDistinctBuilder = this.apply {
        this.input = input
    }

    internal fun build(): Rel.Op.Distinct = Rel.Op.Distinct(input = input!!)
}

internal class RelOpFilterBuilder(
    internal var input: Rel? = null,
    internal var predicate: Rex? = null,
) {
    internal fun input(input: Rel?): RelOpFilterBuilder = this.apply {
        this.input = input
    }

    internal fun predicate(predicate: Rex?): RelOpFilterBuilder = this.apply {
        this.predicate = predicate
    }

    internal fun build(): Rel.Op.Filter = Rel.Op.Filter(input = input!!, predicate = predicate!!)
}

internal class RelOpSortBuilder(
    internal var input: Rel? = null,
    internal var specs: MutableList<Rel.Op.Sort.Spec> = mutableListOf(),
) {
    internal fun input(input: Rel?): RelOpSortBuilder = this.apply {
        this.input = input
    }

    internal fun specs(specs: MutableList<Rel.Op.Sort.Spec>): RelOpSortBuilder = this.apply {
        this.specs = specs
    }

    internal fun build(): Rel.Op.Sort = Rel.Op.Sort(input = input!!, specs = specs)
}

internal class RelOpSortSpecBuilder(
    internal var rex: Rex? = null,
    internal var order: Rel.Op.Sort.Order? = null,
) {
    internal fun rex(rex: Rex?): RelOpSortSpecBuilder = this.apply {
        this.rex = rex
    }

    internal fun order(order: Rel.Op.Sort.Order?): RelOpSortSpecBuilder = this.apply {
        this.order = order
    }

    internal fun build(): Rel.Op.Sort.Spec = Rel.Op.Sort.Spec(rex = rex!!, order = order!!)
}

internal class RelOpUnionBuilder(
    internal var lhs: Rel? = null,
    internal var rhs: Rel? = null,
) {
    internal fun lhs(lhs: Rel?): RelOpUnionBuilder = this.apply {
        this.lhs = lhs
    }

    internal fun rhs(rhs: Rel?): RelOpUnionBuilder = this.apply {
        this.rhs = rhs
    }

    internal fun build(): Rel.Op.Union = Rel.Op.Union(lhs = lhs!!, rhs = rhs!!)
}

internal class RelOpIntersectBuilder(
    internal var lhs: Rel? = null,
    internal var rhs: Rel? = null,
) {
    internal fun lhs(lhs: Rel?): RelOpIntersectBuilder = this.apply {
        this.lhs = lhs
    }

    internal fun rhs(rhs: Rel?): RelOpIntersectBuilder = this.apply {
        this.rhs = rhs
    }

    internal fun build(): Rel.Op.Intersect = Rel.Op.Intersect(lhs = lhs!!, rhs = rhs!!)
}

internal class RelOpExceptBuilder(
    internal var lhs: Rel? = null,
    internal var rhs: Rel? = null,
) {
    internal fun lhs(lhs: Rel?): RelOpExceptBuilder = this.apply {
        this.lhs = lhs
    }

    internal fun rhs(rhs: Rel?): RelOpExceptBuilder = this.apply {
        this.rhs = rhs
    }

    internal fun build(): Rel.Op.Except = Rel.Op.Except(lhs = lhs!!, rhs = rhs!!)
}

internal class RelOpLimitBuilder(
    internal var input: Rel? = null,
    internal var limit: Rex? = null,
) {
    internal fun input(input: Rel?): RelOpLimitBuilder = this.apply {
        this.input = input
    }

    internal fun limit(limit: Rex?): RelOpLimitBuilder = this.apply {
        this.limit = limit
    }

    internal fun build(): Rel.Op.Limit = Rel.Op.Limit(input = input!!, limit = limit!!)
}

internal class RelOpOffsetBuilder(
    internal var input: Rel? = null,
    internal var offset: Rex? = null,
) {
    internal fun input(input: Rel?): RelOpOffsetBuilder = this.apply {
        this.input = input
    }

    internal fun offset(offset: Rex?): RelOpOffsetBuilder = this.apply {
        this.offset = offset
    }

    internal fun build(): Rel.Op.Offset = Rel.Op.Offset(input = input!!, offset = offset!!)
}

internal class RelOpProjectBuilder(
    internal var input: Rel? = null,
    internal var projections: MutableList<Rex> = mutableListOf(),
) {
    internal fun input(input: Rel?): RelOpProjectBuilder = this.apply {
        this.input = input
    }

    internal fun projections(projections: MutableList<Rex>): RelOpProjectBuilder = this.apply {
        this.projections = projections
    }

    internal fun build(): Rel.Op.Project = Rel.Op.Project(input = input!!, projections = projections)
}

internal class RelOpJoinBuilder(
    internal var lhs: Rel? = null,
    internal var rhs: Rel? = null,
    internal var rex: Rex? = null,
    internal var type: Rel.Op.Join.Type? = null,
) {
    internal fun lhs(lhs: Rel?): RelOpJoinBuilder = this.apply {
        this.lhs = lhs
    }

    internal fun rhs(rhs: Rel?): RelOpJoinBuilder = this.apply {
        this.rhs = rhs
    }

    internal fun rex(rex: Rex?): RelOpJoinBuilder = this.apply {
        this.rex = rex
    }

    internal fun type(type: Rel.Op.Join.Type?): RelOpJoinBuilder = this.apply {
        this.type = type
    }

    internal fun build(): Rel.Op.Join = Rel.Op.Join(
        lhs = lhs!!, rhs = rhs!!, rex = rex!!,
        type =
        type!!
    )
}

internal class RelOpAggregateBuilder(
    internal var input: Rel? = null,
    internal var strategy: Rel.Op.Aggregate.Strategy? = null,
    internal var calls: MutableList<Rel.Op.Aggregate.Call> = mutableListOf(),
    internal var groups: MutableList<Rex> = mutableListOf(),
) {
    internal fun input(input: Rel?): RelOpAggregateBuilder = this.apply {
        this.input = input
    }

    internal fun strategy(strategy: Rel.Op.Aggregate.Strategy?): RelOpAggregateBuilder = this.apply {
        this.strategy = strategy
    }

    internal fun calls(calls: MutableList<Rel.Op.Aggregate.Call>): RelOpAggregateBuilder = this.apply {
        this.calls = calls
    }

    internal fun groups(groups: MutableList<Rex>): RelOpAggregateBuilder = this.apply {
        this.groups = groups
    }

    internal fun build(): Rel.Op.Aggregate = Rel.Op.Aggregate(
        input = input!!, strategy = strategy!!,
        calls = calls, groups = groups
    )
}

internal class RelOpAggregateCallBuilder(
    internal var agg: Agg? = null,
    internal var args: MutableList<Rex> = mutableListOf(),
) {
    internal fun agg(agg: Agg?): RelOpAggregateCallBuilder = this.apply {
        this.agg = agg
    }

    internal fun args(args: MutableList<Rex>): RelOpAggregateCallBuilder = this.apply {
        this.args = args
    }

    internal fun build(): Rel.Op.Aggregate.Call = Rel.Op.Aggregate.Call(agg = agg!!, args = args)
}

internal class RelOpExcludeBuilder(
    internal var input: Rel? = null,
    internal var items: MutableList<Rel.Op.Exclude.Item> = mutableListOf(),
) {
    internal fun input(input: Rel?): RelOpExcludeBuilder = this.apply {
        this.input = input
    }

    internal fun items(items: MutableList<Rel.Op.Exclude.Item>): RelOpExcludeBuilder = this.apply {
        this.items = items
    }

    internal fun build(): Rel.Op.Exclude = Rel.Op.Exclude(input = input!!, items = items)
}

internal class RelOpExcludeItemBuilder(
    internal var root: Identifier.Symbol? = null,
    internal var steps: MutableList<Rel.Op.Exclude.Step> = mutableListOf(),
) {
    internal fun root(root: Identifier.Symbol?): RelOpExcludeItemBuilder = this.apply {
        this.root = root
    }

    internal fun steps(steps: MutableList<Rel.Op.Exclude.Step>): RelOpExcludeItemBuilder = this.apply {
        this.steps = steps
    }

    internal fun build(): Rel.Op.Exclude.Item = Rel.Op.Exclude.Item(root = root!!, steps = steps)
}

internal class RelOpExcludeStepAttrBuilder(
    internal var symbol: Identifier.Symbol? = null,
) {
    internal fun symbol(symbol: Identifier.Symbol?): RelOpExcludeStepAttrBuilder = this.apply {
        this.symbol = symbol
    }

    internal fun build(): Rel.Op.Exclude.Step.Attr = Rel.Op.Exclude.Step.Attr(symbol = symbol!!)
}

internal class RelOpExcludeStepPosBuilder(
    internal var index: Int? = null,
) {
    internal fun index(index: Int?): RelOpExcludeStepPosBuilder = this.apply {
        this.index = index
    }

    internal fun build(): Rel.Op.Exclude.Step.Pos = Rel.Op.Exclude.Step.Pos(index = index!!)
}

internal class RelOpExcludeStepStructWildcardBuilder() {
    internal fun build(): Rel.Op.Exclude.Step.StructWildcard = Rel.Op.Exclude.Step.StructWildcard()
}

internal class RelOpExcludeStepCollectionWildcardBuilder() {
    internal fun build(): Rel.Op.Exclude.Step.CollectionWildcard =
        Rel.Op.Exclude.Step.CollectionWildcard()
}

internal class RelOpErrBuilder(
    internal var message: String? = null,
) {
    internal fun message(message: String?): RelOpErrBuilder = this.apply {
        this.message = message
    }

    internal fun build(): Rel.Op.Err = Rel.Op.Err(message = message!!)
}

internal class RelBindingBuilder(
    internal var name: String? = null,
    internal var type: StaticType? = null,
) {
    internal fun name(name: String?): RelBindingBuilder = this.apply {
        this.name = name
    }

    internal fun type(type: StaticType?): RelBindingBuilder = this.apply {
        this.type = type
    }

    internal fun build(): Rel.Binding = Rel.Binding(name = name!!, type = type!!)
}

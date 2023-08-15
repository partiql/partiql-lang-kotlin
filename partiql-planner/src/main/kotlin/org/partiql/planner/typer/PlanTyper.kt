package org.partiql.planner.typer

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.util.PlanRewriter
import org.partiql.planner.Env
import org.partiql.planner.PlanningProblemDetails
import org.partiql.planner.ResolutionStrategy
import org.partiql.planner.TypeEnv
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

internal class PlanTyper(
    private val env: Env,
    private val onProblem: ProblemCallback,
) {

    /**
     * Rewrite the statement with inferred types and resolved variables
     */
    public fun resolve(statement: Statement): Statement {
        if (statement !is Statement.Query) {
            throw IllegalArgumentException("PartiQLPlanner only supports Query statements")
        }
        // root TypeEnv has no bindings
        val typeEnv = TypeEnv(
            schema = emptyList(),
            strategy = ResolutionStrategy.GLOBAL,
        )
        val root = statement.root.type(typeEnv)
        return Plan.statementQuery(root)
    }

    /**
     * Types the relational operators of a query expression.
     *
     * @property outer represents the outer TypeEnv of a query expression — only used by scan variable resolution.
     */
    private inner class RelTyper(private val outer: TypeEnv) : PlanRewriter<Rel.Type?>() {

        override fun visitRel(node: Rel, ctx: Rel.Type?) = super.visitRelOp(node.op, node.type) as Rel

        /**
         * The output schema of a `rel.op.scan` is the single value binding.
         */
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel = Plan.create {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val type = ctx!!.copyWithSchema(listOf(rex.type))
            // rewrite
            val op = relOpScan(rex)
            rel(type, op)
        }

        /**
         * The output schema of a `rel.op.scan_index` is the value binding and index binding.
         */
        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type?): Rel = Plan.create {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val valueT = rex.type
            val indexT = StaticType.INT
            val type = ctx!!.copyWithSchema(listOf(valueT, indexT))
            // rewrite
            val op = relOpScan(rex)
            rel(type, op)
        }

        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Unpivot")
        }

        override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Distinct")
        }

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): Rel = Plan.create {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val predicate = node.predicate.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpFilter(input, predicate)
            rel(type, op)
        }

        override fun visitRelOpSort(node: Rel.Op.Sort, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Sort")
        }

        override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: Rel.Type?): Rel {
            TODO("Type RelOp SortSpec")
        }

        override fun visitRelOpUnion(node: Rel.Op.Union, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Union")
        }

        override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Intersect")
        }

        override fun visitRelOpExcept(node: Rel.Op.Except, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Except")
        }

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type?) = Plan.create {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type limit expression using outer scope with global resolution
            val typeEnv = outer.global()
            val limit = node.limit.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpLimit(input, limit)
            rel(type, op)
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type?) = Plan.create {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type offset expression using outer scope with global resolution
            val typeEnv = outer.global()
            val offset = node.offset.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpOffset(input, offset)
            rel(type, op)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): Rel = Plan.create {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val projections = node.projections.map { it.type(typeEnv) }
            // compute output schema
            val schema = projections.map { it.type }
            val type = input.type.copyWithSchema(schema)
            // rewrite
            val op = relOpProject(input, projections)
            rel(type, op)
        }

        override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Join")
        }

        override fun visitRelOpJoinTypeCross(node: Rel.Op.Join.Type.Cross, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Cross")
        }

        override fun visitRelOpJoinTypeEqui(node: Rel.Op.Join.Type.Equi, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Equi")
        }

        override fun visitRelOpJoinTypeTheta(node: Rel.Op.Join.Type.Theta, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Theta")
        }

        override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Aggregate")
        }

        override fun visitRelOpAggregateAgg(node: Rel.Op.Aggregate.Agg, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Agg")
        }

        override fun visitRelBinding(node: Rel.Binding, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Binding")
        }
    }

    /**
     * Types a PartiQL expression tree. For now, we ignore the pre-existing type. We assume all existing types
     * are simply the `any`, so we keep the new type. Ideally we can programmatically calculate the most specific type.
     *
     * @property locals TypeEnv in which this rex tree is evaluated.
     */
    private inner class RexTyper(private val locals: TypeEnv) : PlanRewriter<StaticType?>() {

        override fun visitRex(node: Rex, ctx: StaticType?): Rex = super.visitRexOp(node.op, node.type) as Rex

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType?): Rex = Plan.create {
            val type = node.value.type.toStaticType()
            rex(type, node)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: StaticType?): Rex = Plan.create {
            assert(node.ref < locals.schema.size) { "Invalid resolved variable (var ${node.ref}) for $locals" }
            val type = locals.schema[node.ref].type
            rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: StaticType?): Rex = Plan.create {
            val path = node.identifier.toBindingPath()
            val resolvedType = env.resolve(path, locals, node.scope)
            if (resolvedType == null) {
                handleUndefinedVariable(path.steps.last())
                return rex(StaticType.ANY, node)
            }
            val type = resolvedType.type
            val op = when (resolvedType.scope) {
                ResolutionStrategy.LOCAL -> rexOpVarResolved(resolvedType.ordinal)
                ResolutionStrategy.GLOBAL -> rexOpGlobal(resolvedType.ordinal)
            }
            rex(type, op)
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCallArgValue(node: Rex.Op.Call.Arg.Value, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCallArgType(node: Rex.Op.Call.Arg.Type, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCollToScalar(node: Rex.Op.CollToScalar, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCollToScalarSubquery(node: Rex.Op.CollToScalar.Subquery, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType?): Rex {
            TODO("Type RexOp")
        }
    }

    // Helpers

    private fun Rel.type(typeEnv: TypeEnv): Rel = RelTyper(typeEnv).visitRel(this, null)

    private fun Rex.type(typeEnv: TypeEnv) = RexTyper(typeEnv).visitRex(this, null)

    /**
     * I found decorating the tree with the binding names (for resolution) was easier than associating introduced
     * bindings with a node via an id->list<string> map. ONLY because right now I don't think we have a good way
     * of managing ids when trees are rewritten.
     *
     * We need a good answer for these questions before going for it:
     * - If you copy, should the id should come along for the ride?
     * - If someone writes their own pass and forgets to copy the id, then resolution could break.
     *
     * We may be able to eliminate this issue by keeping everything internal and running the typing pass first.
     * This is simple enough for now.
     */
    private fun Rel.Type.copyWithSchema(types: List<StaticType>): Rel.Type {
        assert(types.size == schema.size) { "Illegal copy, types size does not matching bindings list size" }
        return this.copy(
            schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) }
        )
    }

    private fun Identifier.toBindingPath() = when (this) {
        is Identifier.Qualified -> this.toBindingPath()
        is Identifier.Symbol -> BindingPath(listOf(this.toBindingName()))
    }

    private fun Identifier.Qualified.toBindingPath() = BindingPath(
        steps = steps.map { it.toBindingName() }
    )

    private fun Identifier.Symbol.toBindingName() = BindingName(
        name = symbol,
        bindingCase = when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> BindingCase.SENSITIVE
            Identifier.CaseSensitivity.INSENSITIVE -> BindingCase.INSENSITIVE
        }
    )

    // ERRORS

    private fun handleUndefinedVariable(name: BindingName) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UndefinedVariable(name.name, name.bindingCase == BindingCase.SENSITIVE)
            )
        )
    }
}

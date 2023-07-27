package org.partiql.planner.passes

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Type
import org.partiql.plan.builder.PlanFactory
import org.partiql.plan.util.PlanRewriter
import org.partiql.planner.PartiQLPlannerContext
import org.partiql.planner.PartiQLPlannerPass
import org.partiql.planner.errors.PartiQLPlannerErrorHandler
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

/**
 * This pass is responsible for typing a plan.
 */
internal class PlanTyperPass(
    private val context: PartiQLPlannerContext,
    override val errorHandler: PartiQLPlannerErrorHandler
) : PartiQLPlannerPass {

    private val factory: PlanFactory = Plan

    override fun apply(plan: PartiQLPlan): PartiQLPlan {
        TODO("Not yet implemented")
    }

    private inline fun <T : PlanNode> plan(block: PlanFactory.() -> T): T = factory.block()

    /**
     * We use an outer product type with an `op` union type field to recreate inheritance, see rex/rel.
     * 
     * - When descending the tree, we carry the `type` field of a rex/rel node into the rex.op/rel.op nodes.
     * - Additionally, we carry the current TypeEnv for local variable resolution.
     */
    private data class Ctx<T>(val env: TypeEnv, val ctx: T? = null)

    /**
     * TypeEnv represents the environment in which we type expressions and resolve variables while planning.
     *
     * @property schema
     * @property strategy
     */
    private class TypeEnv(
        val schema: List<Rel.Binding>,
        val strategy: ResolutionStrategy,
    ) {

        /**
         * Return a copy with GLOBAL lookup strategy
         */
        fun global() = TypeEnv(schema, ResolutionStrategy.GLOBAL)

        /**
         * Return a copy with LOCAL lookup strategy
         */
        fun local() = TypeEnv(schema, ResolutionStrategy.LOCAL)

        /**
         * Debug string
         */
        override fun toString() = buildString {
            append("(")
            append("strategy=$strategy")
            append(", ")
            val bindings = "< " + schema.joinToString { "${it.name}: ${it.type.annotation}" } + " >"
            append("bindings=$bindings")
            append(")")
        }
    }

    /**
     * Variable resolution strategies — https://partiql.org/assets/PartiQL-Specification.pdf#page=35
     *
     * | Value      | Strategy              | Scoping Rules |
     * |------------+-----------------------+---------------|
     * | LOCAL      | local-first lookup    | Rules 1, 2    |
     * | GLOBAL     | global-first lookup   | Rule 3        |
     */
    private enum class ResolutionStrategy {
        LOCAL,
        GLOBAL,
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
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel = plan {
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
        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type?): Rel = plan {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val valueT = rex.type
            val indexT = context.resolveType(StaticType.INT)
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

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): Rel = plan {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val env = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val predicate = node.predicate.type(env)
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

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Limit")
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Offset")
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): Rel = plan {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val env = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val projections = node.projections.map { it.type(env) }
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
     * @property env TypeEnv in which this rex tree is evaluated.
     */
    private inner class RexTyper(private val env: TypeEnv) : PlanRewriter<Type.Ref?>() {

        override fun visitRex(node: Rex, ctx: Type.Ref?): Rex = super.visitRex(node, node.type) as Rex

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Type.Ref?): Rex = plan {
            val type = context.resolveType(node.value.type)
            rex(type, node)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: Type.Ref?): Rex = plan {
            assert(node.ref < env.schema.size) { "Invalid resolved variable (var ${node.ref}) for $env" }
            val type = env.schema[node.ref].type
            rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: Type.Ref?): Rex {

            TODO("Type RexOp")
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPath(node: Rex.Op.Path, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCall(node: Rex.Op.Call, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCallArgValue(node: Rex.Op.Call.Arg.Value, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCallArgType(node: Rex.Op.Call.Arg.Type, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCollToScalar(node: Rex.Op.CollToScalar, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpCollToScalarSubquery(node: Rex.Op.CollToScalar.Subquery, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Type.Ref?): Rex {
            TODO("Type RexOp")
        }
    }

    // Helpers

    private fun Rel.type(env: TypeEnv): Rel = RelTyper(env).visitRel(this, null)

    private fun Rex.type(env: TypeEnv) = RexTyper(env).visitRex(this, null)

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
    private fun Rel.Type.copyWithSchema(types: List<Type.Ref>): Rel.Type {
        assert(types.size == schema.size) { "Illegal copy, types size does not matching bindings list size" }
        return this.copy(
            schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) }
        )
    }

    // /**
    //  * Ctx represents whether we want a global-first (true) or local-first lookup (false).
    //  */
    // private inner class GlobalResolver : PlanRewriter<Boolean>() {
    //
    //     // scope of operator variables are by default locals
    //     override fun visitRelOp(node: Rel.Op, env: Boolean) = super.visitRelOp(node, false)
    //
    //     // scope of scan variables are by default globals
    //     override fun visitRelOpScan(node: Rel.Op.Scan, env: Boolean) = super.visitRelOpScan(node, true)
    //
    //     override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, env: Boolean): Rel {
    //         if (!env || node.scope == Rex.Op.Var.Scope.LOCAL) {
    //             TODO("Type RexOp")
    //         }
    //         // rewrite as resolved global if possible
    //         return when (val global = context.resolveGlobal(node.identifier)) {
    //             null -> node
    //             else -> global
    //         }
    //     }
    // }
}

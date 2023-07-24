package org.partiql.planner.passes

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.util.PlanRewriter
import org.partiql.planner.PartiQLPlannerEnv
import org.partiql.planner.PartiQLPlannerPass
import org.partiql.planner.errors.PartiQLPlannerErrorHandler

/**
 * This pass is responsible for resolving any known globals
 */
internal class ResolveGlobalsInScanPass(private val env: PartiQLPlannerEnv) : PartiQLPlannerPass {

    override fun apply(plan: PartiQLPlan, errorHandler: PartiQLPlannerErrorHandler): PartiQLPlan {
        TODO("Not yet implemented")
    }

    /**
     * Ctx represents whether we want a global-first (true) or local-first lookup (false).
     */
    private inner class GlobalResolver : PlanRewriter<Boolean>() {

        // scope of operator variables are by default locals
        override fun visitRelOp(node: Rel.Op, ctx: Boolean) = super.visitRelOp(node, false)

        // scope of scan variables are by default globals
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Boolean) = super.visitRelOpScan(node, true)

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: Boolean): PlanNode {
            if (!ctx || node.scope == Rex.Op.Var.Scope.LOCAL) {
                return super.visitRexOpVarUnresolved(node, ctx)
            }
            // rewrite as resolved global if possible
            return when (val global = env.resolveGlobal(node.identifier)) {
                null -> node
                else -> global
            }
        }
    }
}

package org.partiql.lang.planner.transforms.optimizations

import org.partiql.errors.ProblemHandler
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.PartiQLPhysicalPass
import org.partiql.lang.planner.transforms.isLitTrue

/** Creates a pass that removes any `(filter ...)` where the predicate is simply `(lit true)`. */
fun createRemoveUselessFiltersPass(): PartiQLPhysicalPass =
    RemoveUselessFiltersPass()

private class RemoveUselessFiltersPass : PartiQLPhysicalPass {
    override fun apply(plan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan =
        object : PartiqlPhysical.VisitorTransform() {
            override fun transformBexprFilter(node: PartiqlPhysical.Bexpr.Filter): PartiqlPhysical.Bexpr {
                val rewritten = super.transformBexprFilter(node) as PartiqlPhysical.Bexpr.Filter
                return if (node.predicate.isLitTrue()) {
                    rewritten.source
                } else {
                    rewritten
                }
            }
        }.transformPlan(plan)
}

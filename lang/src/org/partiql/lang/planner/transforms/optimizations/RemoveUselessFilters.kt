package org.partiql.lang.planner.transforms.optimizations

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiqlPhysicalPass
import org.partiql.lang.planner.transforms.isLitTrue

/** Creates a pass that removes any `(filter ...)` where the predicate is simply `(lit true)`. */
fun createRemoveUselessFiltersPass(): PartiqlPhysicalPass =
    RemoveUselessFiltersPass()

private class RemoveUselessFiltersPass : PartiqlPhysicalPass {
    override val passName: String get() = "remove_useless_filters"

    override fun rewrite(inputPlan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan =
        object : PartiqlPhysical.VisitorTransform() {
            override fun transformBexprFilter(node: PartiqlPhysical.Bexpr.Filter): PartiqlPhysical.Bexpr {
                val rewritten = super.transformBexprFilter(node) as PartiqlPhysical.Bexpr.Filter
                return if (node.predicate.isLitTrue()) {
                    rewritten.source
                } else {
                    rewritten
                }
            }
        }.transformPlan(inputPlan)
}

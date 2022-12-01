package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.ionBool
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiQLPlannerPass
import org.partiql.lang.planner.transforms.isLitTrue

/**
 * Creates an instance of [PartiQLPlannerPass.Physical] that removes useless "AND" expressions by, i.e.:
 *
 * - `(and (lit true) (lit true))` -> `(lit true)`
 * - `(and (lit true) <expr>))` -> `<expr>`
 * - `(and <expr> (lit true)))` -> `<expr>`
 *
 * If more than one non-lit true remains, the `AND` expression is not useless, but the expression
 * is returned with all `(lit true)` operands removed.
 *
 * Note that in the future, it may be possible to do this with a more general constant folding rewrite but that is
 * currently out of scope.
 */
fun createRemoveUselessAndsPass(): PartiQLPlannerPass.Physical =
    RemoveUselessAndsPass()

private class RemoveUselessAndsPass : PartiQLPlannerPass.Physical {
    // override val passName: String get() = "remove_useless_ands"

    override fun apply(plan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan =
        object : PartiqlPhysical.VisitorTransform() {
            override fun transformExprAnd(node: PartiqlPhysical.Expr.And): PartiqlPhysical.Expr {
                // Recursing here transforms all child nodes, which is what we really want.
                val rewritten = super.transformExprAnd(node)
                if (rewritten !is PartiqlPhysical.Expr.And) {
                    return rewritten
                }

                val nonLitTrueOperands = rewritten.operands.filter { !it.isLitTrue() }
                return when (nonLitTrueOperands.size) {
                    // (and (lit true) (lit true)) -> (lit true)
                    0 -> PartiqlPhysical.build { lit(ionBool(true)) }
                    // (and (lit true) <expr>)) -> <expr>
                    // (and <expr> (lit true))) -> <expr>
                    // etc.
                    1 -> nonLitTrueOperands.single()
                    // For any other combo, simply filter out the non-(lit true) operands
                    else -> PartiqlPhysical.build { and(nonLitTrueOperands) }
                }
            }
        }.transformPlan(plan)
}

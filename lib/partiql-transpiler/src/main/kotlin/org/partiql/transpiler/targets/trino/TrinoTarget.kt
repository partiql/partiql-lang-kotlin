package org.partiql.transpiler.targets.trino

import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.util.PlanRewriter
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.TranspilerProblem
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.transpiler.sql.SqlTarget
import org.partiql.types.SingleType

/**
 * Experimental Trino SQL transpilation target.
 */
public object TrinoTarget : SqlTarget() {

    override val target: String = "Trino"

    override val version: String = "3"

    /**
     * Wire the Trino call rewrite rules.
     */
    override fun getCalls(onProblem: ProblemCallback): SqlCalls = TrinoCalls(onProblem)

    /**
     * At this point, no plan rewriting.
     */
    override fun rewrite(plan: PartiQLPlan, onProblem: ProblemCallback) = TrinoRewriter(onProblem).visitPartiQLPlan(plan, Unit) as PartiQLPlan

    private class TrinoRewriter(val onProblem: ProblemCallback) : PlanRewriter<Unit>() {
        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit): PlanNode {
            // Make sure that the output type is homogeneous
            node.projections.forEachIndexed { index, projection ->
                val type = projection.type.asNonNullable().flatten()
                if (type !is SingleType) {
                    onProblem(
                        TranspilerProblem(
                            TranspilerProblem.Level.ERROR,
                            "Projection item (index $index) is heterogeneous (${type.allTypes.joinToString(",")}) and cannot be coerced to a single type."
                        )
                    )
                }
            }
            return super.visitRelOpProject(node, ctx)
        }
    }
}

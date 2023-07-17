package org.partiql.planner.impl

import org.partiql.plan.PartiQLVersion
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.impl.transforms.AstNormalize
import org.partiql.planner.impl.transforms.AstToPlan
import org.partiql.ast.Statement as AstStatement

/**
 * Default logical planner.
 */
class PartiQLPlannerDefault : PartiQLPlanner {

    private val version = PartiQLVersion.VERSION_0_1

    override fun plan(statement: AstStatement): PartiQLPlanner.Result {
        // 0. Initialize the environment
        val env = PartiQLPlannerEnv()

        // 1. Normalize
        val ast = AstNormalize.apply(statement)

        // 2. AST to Rel/Rex
        val root = AstToPlan.apply(ast, env)

        // 3. Resolve
        // --

        // 4. Apply planner passes
        // --

        // Wrap in PartiQLPlan struct
        return PartiQLPlanner.Result(
            plan = Plan.partiQLPlan(
                version = version,
                statement = root,
            )
        )
    }
}

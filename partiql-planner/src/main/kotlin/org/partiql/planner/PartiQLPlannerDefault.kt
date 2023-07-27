package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.ast.normalize.normalize
import org.partiql.plan.PartiQLVersion
import org.partiql.plan.Plan
import org.partiql.planner.transforms.AstToPlan

/**
 * Default logical planner.
 */
class PartiQLPlannerDefault : PartiQLPlanner {

    private val version = PartiQLVersion.VERSION_0_1

    override fun plan(session: PartiQLPlanner.Session, statement: Statement): PartiQLPlanner.Result {
        // 0. Initialize the environment
        val env = PartiQLPlannerContext(session)

        // 1. Normalize
        val ast = statement.normalize()

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
                globals = env.globals(),
                statement = root,
            )
        )
    }
}

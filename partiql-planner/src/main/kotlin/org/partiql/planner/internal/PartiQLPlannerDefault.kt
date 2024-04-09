package org.partiql.planner.internal

import org.partiql.ast.Statement
import org.partiql.ast.normalize.normalize
import org.partiql.errors.ProblemCallback
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerPass
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.planner.internal.typer.PlanTyper

/**
 * Default PartiQL logical query planner.
 */
internal class PartiQLPlannerDefault(
    private val passes: List<PartiQLPlannerPass>,
    private val flags: Set<PlannerFlag>
) : PartiQLPlanner {

    override fun plan(
        statement: Statement,
        session: PartiQLPlanner.Session,
        onProblem: ProblemCallback,
    ): PartiQLPlanner.Result {

        // 0. Initialize the planning environment
        val env = Env(session)

        // 1. Normalize
        val ast = statement.normalize()

        // 2. AST to Rel/Rex
        val root = AstToPlan.apply(ast, env)

        // 3. Resolve variables
        val typer = PlanTyper(env)
        val typed = typer.resolve(root)
        val internal = org.partiql.planner.internal.ir.PartiQLPlan(typed)

        // 4. Assert plan has been resolved — translating to public API
        var plan = PlanTransform(flags).transform(internal, onProblem)

        // 5. Apply all passes
        for (pass in passes) {
            plan = pass.apply(plan, onProblem)
        }

        return PartiQLPlanner.Result(plan, emptyList())
    }
}

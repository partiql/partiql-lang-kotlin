package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.ast.normalize.normalize
import org.partiql.errors.ProblemCallback
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.PartiQLVersion
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.spi.Plugin

/**
 * Default PartiQL logical query planner.
 */
internal class PartiQLPlannerDefault(
    private val headers: List<Header>,
    private val plugins: List<Plugin>,
    private val passes: List<PartiQLPlannerPass>,
) : PartiQLPlanner {

    override fun plan(
        statement: Statement,
        session: PartiQLPlanner.Session,
        onProblem: ProblemCallback,
    ): PartiQLPlanner.Result {
        // 0. Initialize the planning environment
        val env = Env(headers, plugins, session)

        // 1. Normalize
        val ast = statement.normalize()

        // 2. AST to Rel/Rex
        val root = AstToPlan.apply(ast, env)

        // 3. Resolve variables
        val typer = PlanTyper(env, onProblem)
        val internal = org.partiql.planner.internal.ir.PartiQLPlan(
            version = PartiQLVersion.VERSION_0_1,
            globals = env.globals,
            statement = typer.resolve(root),
        )

        // 4. Assert plan has been resolved — translating to public API
        var plan = PlanTransform.visitPartiQLPlan(internal, onProblem)

        // 5. Apply all passes
        for (pass in passes) {
            plan = pass.apply(plan, onProblem)
        }

        return PartiQLPlanner.Result(plan, emptyList())
    }
}

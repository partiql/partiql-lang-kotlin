package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.ast.normalize.normalize
import org.partiql.errors.ProblemCallback
import org.partiql.plan.PartiQLVersion
import org.partiql.plan.Plan
import org.partiql.planner.transforms.AstToPlan
import org.partiql.planner.typer.ConstantFolder
import org.partiql.planner.typer.PlanTyper
import org.partiql.spi.Plugin
import org.partiql.types.TypingMode

/**
 * Default PartiQL logical query planner.
 */
internal class PartiQLPlannerDefault(
    private val plugins: List<Plugin>,
    private val passes: List<PartiQLPlannerPass>,
    private val mode: TypingMode,
) : PartiQLPlanner {

    private val version = PartiQLVersion.VERSION_0_1

    // For now, only have the default header
    private val header = Header.partiql()

    override fun plan(statement: Statement, session: PartiQLPlanner.Session, onProblem: ProblemCallback): PartiQLPlanner.Result {
        // 0. Initialize the planning environment
        val env = Env(header, mode, plugins, session)

        // 1. Normalize
        val ast = statement.normalize()

        // 2. AST to Rel/Rex
        val root = AstToPlan.apply(ast, env)

        // 3. Resolve variables
        val typer = PlanTyper(env, onProblem)
        var planStmt = typer.resolve(root)

        // 4. Fold and re-resolve
        planStmt = fold(planStmt, env, onProblem)

        var plan = Plan.partiQLPlan(
            version = version,
            globals = env.globals,
            statement = planStmt,
        )

        // 5. Apply all passes
        for (pass in passes) {
            plan = pass.apply(plan, onProblem)
        }

        return PartiQLPlanner.Result(plan, emptyList())
    }

    private fun fold(stmt: org.partiql.plan.Statement, env: Env, onProblem: ProblemCallback): org.partiql.plan.Statement {
        val foldedPlan = ConstantFolder.fold(stmt)
        val typer = PlanTyper(env, onProblem)
        return typer.resolve(foldedPlan)
    }
}

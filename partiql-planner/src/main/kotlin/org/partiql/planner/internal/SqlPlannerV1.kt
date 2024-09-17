package org.partiql.planner.internal

import org.partiql.ast.Statement
import org.partiql.ast.normalize.normalize
import org.partiql.errors.ProblemCallback
import org.partiql.plan.v1.PartiQLPlan
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.PlanTransformV1
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.spi.catalog.Session

/**
 * Default PartiQL logical query planner.
 *
 * TODO TEMPORARY WHILE WE SWITCH OUT THE PUBLIC PLAN
 */
public object SqlPlannerV1 {

    // private val flags = setOf<PlannerFlag>(PlannerFlag.SIGNAL_MODE)
    private val flags = setOf<PlannerFlag>()

    public fun plan(
        statement: Statement,
        session: Session,
        onProblem: ProblemCallback = {},
    ): PartiQLPlan {

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
        return PlanTransformV1(flags).transform(internal, onProblem)
    }
}

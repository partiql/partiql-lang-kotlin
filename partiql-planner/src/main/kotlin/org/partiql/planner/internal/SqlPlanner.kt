package org.partiql.planner.internal

import org.partiql.ast.Statement
import org.partiql.plan.Action
import org.partiql.plan.Operators
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlanner.Result
import org.partiql.planner.PartiQLPlannerPass
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.NormalizeFromSource
import org.partiql.planner.internal.transforms.NormalizeGroupBy
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType

/**
 * Default PartiQL logical query planner.
 */
internal class SqlPlanner(
    private var passes: List<PartiQLPlannerPass>,
    private var flags: Set<PlannerFlag>,
) : PartiQLPlanner {

    /**
     * Then default planner logic.
     */
    override fun plan(statement: Statement, session: Session, ctx: Context): Result {
        try {
            // 0. Initialize the planning environment
            val env = Env(session, ctx.errorListener)

            // 1. Normalize
            val ast = statement.normalize()

            // 2. AST to Rel/Rex
            val root = AstToPlan.apply(ast, env)

            // 3. Resolve variables
            val typer = PlanTyper(env, ctx)
            val typed = typer.resolve(root)
            val internal = org.partiql.planner.internal.ir.PartiQLPlan(typed)

            // 4. Assert plan has been resolved — translating to public API
            var plan = PlanTransform(flags).transform(internal, ctx.errorListener)

            // 5. Apply all passes
            for (pass in passes) {
                plan = pass.apply(plan, ctx)
            }
            return Result(plan)
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            return catchAll(ctx, t)
        }
    }

    /**
     * AST normalization
     */
    private fun Statement.normalize(): Statement {
        // could be a fold, but this is nice for setting breakpoints
        var ast = this
        ast = NormalizeFromSource.apply(ast)
        ast = NormalizeGroupBy.apply(ast)
        return ast
    }

    /**
     * Create a plan with a query action and error node.
     *
     * @param t
     * @return
     */
    private fun catchAll(ctx: Context, t: Throwable): Result {
        val error = PError.INTERNAL_ERROR(PErrorKind.SEMANTIC(), null, t)
        ctx.errorListener.report(error)
        val query = Action.Query { Operators.STANDARD.error(PType.dynamic()) }
        val plan = Plan { query }
        return Result(plan)
    }
}

package org.partiql.planner.internal

import org.partiql.ast.Statement
import org.partiql.plan.Operation
import org.partiql.plan.Plan
import org.partiql.plan.builder.Operators
import org.partiql.plan.rex.Rex
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerPass
import org.partiql.planner.internal.normalize.normalize
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PErrorListenerException
import org.partiql.types.PType

/**
 * Default PartiQL logical query planner.
 */
internal class SqlPlanner(
    private var passes: List<PartiQLPlannerPass>,
    private var flags: Set<PlannerFlag>,
) : PartiQLPlanner {

    public override fun plan(
        statement: Statement,
        session: Session,
        ctx: Context,
    ): PartiQLPlanner.Result {
        try {
            // 0. Initialize the planning environment
            val env = Env(session)

            // 1. Normalize
            val ast = statement.normalize()

            // 2. AST to Rel/Rex
            val root = AstToPlan.apply(ast, env)

            // 3. Resolve variables
            val typer = PlanTyper(env, ctx)
            val typed = typer.resolve(root)
            val internal = org.partiql.planner.internal.ir.PartiQLPlan(typed)

            // 4. Assert plan has been resolved — translating to public API
            var plan = PlanTransform(flags).transform(internal, ctx.getErrorListener())

            // 5. Apply all passes
            for (pass in passes) {
                plan = pass.apply(plan, ctx)
            }
            return PartiQLPlanner.Result(plan)
        } catch (e: PErrorListenerException) {
            throw e
        } catch (t: Throwable) {
            val error = PError.INTERNAL_ERROR(PErrorKind.SEMANTIC(), null, t)
            ctx.errorListener.report(error)
            val plan = object : Plan {
                override fun getOperation(): Operation {
                    return object : Operation.Query {
                        override fun getRex(): Rex {
                            return Operators.STANDARD.rexError(PType.dynamic())
                        }
                    }
                }
            }
            return PartiQLPlanner.Result(plan)
        }
    }
}

package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.plan.PartiQLPlan

/**
 * PartiQLPlanner is responsible for transforming an AST into a logical query plan.
 */
public interface PartiQLPlanner {

    public fun plan(statement: Statement): Result

    public class Result(
        val plan: PartiQLPlan,
    )
}

package org.partiql.planner.validate

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.planner.metadata.Session
import org.partiql.planner.metadata.System

/**
 * SqlAnalyzer is responsible for semantic analysis/validation of the internal, algebraic IR.
 *
 * Responsibilities:
 *  - name resolution (variables, tables, routines)
 *  - type-checking
 *  - type-coercion
 */
internal class SqlValidator(
    private val system: System,
    private val session: Session,
    private val sessionLegacy: PartiQLPlanner.Session,
) {

    fun validate(statement: Statement): Statement {
        // LEGACY
        val env = Env(sessionLegacy)
        val typer = PlanTyper(env)
        val typed = typer.resolve(statement)
        // Placeholder for the SQL analysis phase.
        return typed
    }
}

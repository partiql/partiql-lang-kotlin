package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalogs
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.typer.CompilerType

/**
 * [Env] is similar to the database type environment from the PartiQL Specification. This includes resolution of
 * database binding values and scoped functions.
 *
 * See TypeEnv for the variables type environment.
 *
 * TODO: function resolution between scalar functions and aggregations.
 *
 * @property session
 */
internal class Env(
    private val catalogs: Catalogs,
    private val session: Session,
) {

    /**
     *
     * TODO handle missing table error.
     *
     * Convert any remaining binding names (tail) to a path expression.
     */
    fun getTable(identifier: Identifier): Rex? {
        TODO("Env.getTable not implemented")
    }

    fun getRoutine(identifier: Identifier, args: List<Rex>): Rex? {
        TODO("Env.getRoutine not implemented")
    }

    fun resolveCast(input: Rex, target: CompilerType): Rex.Op.Cast.Resolved? {
        val operand = input.type
        val cast = CastTable.partiql.get(operand, target) ?: return null
        return rexOpCastResolved(cast, input)
    }
}

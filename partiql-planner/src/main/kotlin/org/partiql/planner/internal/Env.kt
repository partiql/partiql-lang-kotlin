package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalogs
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.ir.rexOpVarGlobal
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

    private val default = catalogs.default()

    /**
     * TODO fallback to matching root for a catalog name if it exists.
     *
     * Convert any remaining binding names (tail) to a path expression.
     */
    fun getTable(identifier: Identifier): Rex? {
        val handle = default.getTableHandle(session, identifier)
        if (handle == null) {
            // error table not found?
            return null
        }
        val ref = Ref.Table(
            catalog = default.getName(),
            name = Name.of(handle.table.getName()),
            type = CompilerType(handle.table.getSchema())
        )
        return Rex(ref.type, rexOpVarGlobal(ref))
    }

    fun getRoutine(identifier: Identifier, args: List<Rex>): Rex? {
        // TODO
        return null
    }

    fun resolveCast(input: Rex, target: CompilerType): Rex.Op.Cast.Resolved? {
        val operand = input.type
        val cast = CastTable.partiql.get(operand, target) ?: return null
        return rexOpCastResolved(cast, input)
    }
}

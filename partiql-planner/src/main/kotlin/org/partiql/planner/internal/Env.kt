package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalog
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
import org.partiql.planner.internal.typer.TypeEnv.Companion.toPath

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
     * Current [Catalog] implementation; error if missing from the [Catalogs] provider.
     */
    private val default: Catalog = catalogs.get(session.getCatalog()) ?: error("Default catalog does not exist")

    /**
     * Catalog lookup...
     */
    fun getTable(identifier: Identifier): Rex? {
        // lookup at current catalog and current namespace
        var catalog = default
        val path = resolve(identifier)
        var handle = catalog.getTableHandle(session, path)
        if (handle == null && identifier.hasQualifier()) {
            // lookup to see if qualifier
            val head = identifier.first()
            val tail = Identifier.of(identifier.drop(1))
            catalog = catalogs.get(head.getText(), ignoreCase = head.isRegular()) ?: return null
            handle = catalog.getTableHandle(session, tail)
        }
        // NOT FOUND!
        if (handle == null) {
            return null
        }
        // Make a reference and return a global variable expression.
        val refCatalog = catalog.getName()
        val refName = handle.name
        val refType = CompilerType(handle.table.getSchema())
        val ref = Ref.Table(refCatalog, refName, refType)

        // Convert any remaining identifier parts to a path expression
        val root = Rex(ref.type, rexOpVarGlobal(ref))
        val tail = calculateMatched(path, handle.name)
        return if (tail.isEmpty()) root else root.toPath(tail)
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

    // Helpers

    /**
     * Prepends the current session namespace to the identifier; named like Path.resolve() from java io.
     */
    private fun resolve(identifier: Identifier): Identifier {
        val namespace = session.getNamespace()
        return if (namespace.isEmpty()) {
            // no need to create another object
            identifier
        } else {
            // prepend the namespace
            namespace.asIdentifier().append(identifier)
        }
    }

    /**
     * Returns a list of the unmatched parts of the identifier given the matched name.
     */
    private fun calculateMatched(path: Identifier, name: Name): List<Identifier.Part> {
        val lhs = name.toList()
        val rhs = path.toList()
        return rhs.take(rhs.size - lhs.size)
    }
}

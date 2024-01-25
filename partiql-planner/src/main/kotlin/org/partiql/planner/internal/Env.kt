package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpGlobal
import org.partiql.planner.internal.typer.TypeEnv.Companion.toPath
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.StaticType

/**
 * [Env] is similar to the database type environment from the PartiQL Specification. This includes resolution of
 * database binding values and scoped functions.
 *
 * See [TypeEnv] for the variables type environment.
 *
 * @property session
 */
internal class Env(private val session: PartiQLPlanner.Session) {

    /**
     * Maintain a list of all resolved catalog symbols (objects and functions).
     */
    private val symbols: Symbols = Symbols.empty()

    /**
     * Convert the symbols structure into a list of [Catalog] for shipping in the plan.
     */
    internal fun catalogs(): List<Catalog> = symbols.build()

    /**
     * Current catalog [ConnectorMetadata]. Error if missing from the session.
     */
    private val catalog: ConnectorMetadata = session.catalogs[session.currentCatalog]
        ?: error("Session is missing ConnectorMetadata for current catalog ${session.currentCatalog}")

    /**
     * A [PathResolver] for database objects.
     */
    private val objects: PathResolverObj = PathResolverObj(catalog, session)

    /**
     * This function looks up a global [BindingPath], returning a global reference expression.
     *
     * Convert any remaining binding names (tail) to a path expression.
     *
     * @param path
     * @return
     */
    fun resolveObj(path: BindingPath): Rex? {
        val match = objects.lookup(path) ?: return null
        // Insert into symbols, producing a reference
        val ref = symbols.insert(match.item)
        // Rewrite as an untyped path expression.
        val root = rex(StaticType.ANY, rexOpGlobal(ref))
        val tail = path.steps.drop(match.depth)
        return if (tail.isEmpty()) root else root.toPath(tail)
    }
}

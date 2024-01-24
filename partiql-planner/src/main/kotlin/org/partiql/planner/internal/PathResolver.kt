package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata

/**
 * This is the base behavior for name resolution.
 *
 * Let N be the number of steps in a given path.
 *
 * 1. If N = 1
 *     (a) Lookup at <catalog>.<schema>.<path>  (relative in catalog)
 * 2. If N = 2
 *     (a) Lookup at <catalog>.<schema>.<path>  (relative in catalog)
 *     (b) Lookup at <catalog>.<path>           (absolute in catalog)
 * 3. If N > 2
 *     (a) Lookup at <catalog>.<schema>.<path>                          (relative in catalog)
 *     (b) Lookup at <catalog>.<path>                                   (absolute in catalog)
 *     (c) Lookup as an absolute where the first step is the catalog.   (absolute in system)
 *
 * @param T
 * @property catalog
 * @property session
 */
internal abstract class PathResolver<T>(
    private val catalog: ConnectorMetadata,
    private val session: PartiQLPlanner.Session,
) {

    /**
     *
     */
    private val schema = session.currentDirectory.map { it.toBindingName() }

    /**
     * A [PathResolver] should override this one method for which [ConnectorMetadata] API to call.
     *
     * @param path  The absolute path within a catalog.
     * @return
     */
    abstract fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle<T>?

    /**
     * Resolution rules for a given name.
     *
     * @param path  This represents the exact path
     * @return
     */
    internal fun resolve(path: BindingPath): PathEntry<T>? {
        val n = path.steps.size
        val absPath = BindingPath(schema + path.steps)
        return when (n) {
            0 -> null
            1 -> get(absPath)
            2 -> get(absPath) ?: get(path)
            else -> get(absPath) ?: get(path) ?: lookup(path)
        }
    }

    /**
     * This gets the path in the current catalog.
     *
     * @param path
     * @return
     */
    private fun get(path: BindingPath): PathEntry<T>? =
        get(catalog, path)?.let { PathEntry(session.currentCatalog, it) }

    /**
     * This looks for an absolute path in the current system, using the session to lookup catalogs.
     *
     * @param path
     */
    private fun lookup(path: BindingPath): PathEntry<T>? {
        val head = path.steps.first()
        val tail = BindingPath(path.steps.drop(1))
        for ((catalog, metadata) in session.catalogs) {
            if (head.matches(catalog)) {
                return get(metadata, tail)?.let { PathEntry(catalog, it) }
            }
        }
        return null
    }

    private fun String.toBindingName() = BindingName(this, BindingCase.SENSITIVE)
}

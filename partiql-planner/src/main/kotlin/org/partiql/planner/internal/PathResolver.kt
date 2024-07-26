package org.partiql.planner.internal

import org.partiql.planner.catalog.Session
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata

/**
 * TODO PATH RESOLVER WITH BE REPLACED BY THE org.partiql.planner.catalog HOWEVER THIS FACTORING IS TEMPORARY
 *  TO MAKE THE TRANSITION OF THE SESSION A SMALLER AND MORE INCREMENTAL CHANGE.
 *
 *
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
    private val catalogs: Map<String, ConnectorMetadata>,
    private val session: Session,
) {

    /**
     * The session's current directory represented as [BindingName] steps.
     */
    open val schema = session.getNamespace().map { it.toBindingName() }

    /**
     * A [PathResolver] should override this one method for which [ConnectorMetadata] API to call.
     *
     * @param metadata  ConnectorMetadata to resolve this catalog entity in.
     * @param path      The catalog absolute path.
     * @return
     */
    abstract fun get(metadata: ConnectorMetadata, path: BindingPath): ConnectorHandle<T>?

    /**
     * Lookup a `path` following the scoping rules in the class javadoc.
     *
     * Returns a pair of the
     *
     * @param path  This represents the exact path
     * @return
     */
    internal fun lookup(path: BindingPath): PathItem<T>? {
        val n = path.steps.size
        val m = schema.size
        return if (m > 0) {
            val absPath = BindingPath(schema + path.steps)
            when (n) {
                0 -> return null
                1 -> return get(absPath)
                2 -> return get(absPath) ?: get(path)
                else -> return get(absPath) ?: get(path) ?: search(path)
            }
        } else {
            // no need to prepend <schema> path as it's empty
            when (n) {
                0 -> null
                1 -> get(path)
                2 -> get(path)
                else -> get(path) ?: search(path)
            }
        }
    }

    /**
     * This gets the path in the current catalog.
     *
     * @param path   Catalog absolute path.
     * @return
     */
    private fun get(path: BindingPath): PathItem<T>? {
        val handle = get(catalog, path) ?: return null
        return PathItem(session.getCatalog(), path, handle)
    }

    /**
     * This searches with a system absolute path, using the session to lookup catalogs.
     *
     * @param path  System absolute path.
     */
    private fun search(path: BindingPath): PathItem<T>? {
        var match: Map.Entry<String, ConnectorMetadata>? = null
        val first: BindingName = path.steps.first()
        for (catalog in catalogs) {
            if (first.matches(catalog.key)) {
                if (match != null) {
                    // TODO root was already matched, emit ambiguous error
                    return null
                }
                match = catalog
            }
        }
        if (match == null) {
            return null
        }
        // Lookup in the unambiguously matched catalog, calculating the depth matched.
        val absPath = BindingPath(path.steps.drop(1))
        val catalog = match.key
        val metadata = match.value
        val handle = get(metadata, absPath) ?: return null
        return PathItem(catalog, absPath, handle)
    }

    private fun String.toBindingName() = BindingName(this, BindingCase.SENSITIVE)
}

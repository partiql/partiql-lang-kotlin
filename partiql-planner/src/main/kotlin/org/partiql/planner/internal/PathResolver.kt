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
     * The session's current directory represented as [BindingName] steps.
     */
    private val schema = session.currentDirectory.map { it.toBindingName() }

    /**
     * A [PathResolver] should override this one method for which [ConnectorMetadata] API to call.
     *
     * @param path  The catalog absolute path.
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
    internal fun lookup(path: BindingPath): PathMatch<T>? {
        val n = path.steps.size
        val absPath = BindingPath(schema + path.steps)
        return when (n) {
            0 -> null
            1 -> get(absPath, n)
            2 -> get(absPath, n) ?: get(path, n)
            else -> get(absPath, n) ?: get(path, n) ?: search(path, n)
        }
    }

    /**
     * This gets the path in the current catalog.
     *
     * @param absPath   Catalog absolute path.
     * @param n         Original path step size, used in depth calculation.
     * @return
     */
    private fun get(absPath: BindingPath, n: Int): PathMatch<T>? {
        val handle = get(catalog, absPath) ?: return null
        val depth = depth(n, absPath.steps.size, handle.path.size)
        val item = PathItem(session.currentCatalog, handle)
        return PathMatch(depth, item)
    }

    /**
     * This searches with a system absolute path, using the session to lookup catalogs.
     *
     * @param path  System absolute path.
     * @param n     Original path step size, used in depth calculation.
     */
    private fun search(path: BindingPath, n: Int): PathMatch<T>? {
        var match: Map.Entry<String, ConnectorMetadata>? = null
        val first: BindingName = path.steps.first()
        for (catalog in session.catalogs) {
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
        val depth = depth(n, absPath.steps.size, handle.path.size)
        val item = PathItem(catalog, handle)
        return PathMatch(depth, item)
    }

    private fun String.toBindingName() = BindingName(this, BindingCase.SENSITIVE)

    /**
     * Logic for determining how many BindingNames were “matched” by the ConnectorMetadata
     *
     * 1. Matched = RelativePath - Not Found
     * 2. Not Found = Input CatalogPath - Output CatalogPath
     * 3. Matched = RelativePath - (Input CatalogPath - Output CatalogPath)
     * 4. Matched = RelativePath + Output CatalogPath - Input CatalogPath
     *
     * Ask johqunn@ as this is too clever for me.
     */
    private fun depth(original: Int, input: Int, output: Int): Int = original + output - input
}

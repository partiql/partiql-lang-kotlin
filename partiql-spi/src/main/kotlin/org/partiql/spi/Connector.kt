package org.partiql.spi

import org.partiql.spi.catalog.Catalog

/**
 * A Connector is responsible for instantiating [Catalog] implementations.
 *
 * @see Catalog
 */
public interface Connector {

    /**
     * Marker interface for an arbitrary context argument; marker over generics due to type erasure of generics.
     */
    public interface Context

    /**
     * Get (or instantiate) a [Catalog] with the given name.
     *
     * @param name      Catalog name
     * @return
     */
    public fun getCatalog(name: String): Catalog

    /**
     * Get (or instantiate) a [Catalog] with the given name and additional context.
     *
     * Example:
     *
     * ```
     * override fun getCatalog(context: Context): Catalog {
     *     if (context !is MyContext) {
     *        throw IllegalArgumentException("Unsupported context type: $context")
     *     }
     *     return getCatalog(context)
     * }
     *
     * private fun getCatalog(context: MyContext): Catalog {
     *      // ... get catalog from context
     * }
     * ```
     *
     * @param name      Catalog name
     * @param context   Context is an arbitrary object
     * @return
     */
    public fun getCatalog(name: String, context: Context): Catalog
}

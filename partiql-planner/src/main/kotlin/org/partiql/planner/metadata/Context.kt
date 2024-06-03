package org.partiql.planner.metadata

/**
 * The PartiQL-system root. Like a Calcite CatalogReader.
 */
public interface Context {

    /**
     * Returns the namespace with the given name, or null if no such namespace exists.
     */
    public fun getCatalog(name: String): Namespace?

    /**
     * Returns a list of all top-level namespaces.
     */
    public fun listCatalogs(): Collection<String>

    /**
     * Default [Context] implementation.
     */
    public class Base private constructor(private val catalogs: Map<String, Namespace>) : Context {
        override fun getCatalog(name: String): Namespace? = catalogs[name]
        override fun listCatalogs(): Collection<String> = catalogs.keys
    }
}

package org.partiql.planner.metadata

/**
 * The PartiQL-system root; like a Calcite CatalogReader.
 */
public interface System {

    /**
     * Returns the namespace with the given name, or null if no such namespace exists.
     */
    public fun getCatalog(name: String): Namespace?

    /**
     * Returns a list of all top-level namespaces.
     */
    public fun listCatalogs(): Collection<String>

    /**
     * Get a system function's variants by name.
     */
    public fun getRoutines(name: String): List<Routine>
}

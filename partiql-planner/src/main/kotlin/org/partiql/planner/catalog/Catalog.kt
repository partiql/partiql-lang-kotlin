package org.partiql.planner.catalog

/**
 * Catalog interface for access to tables and routines.
 *
 * Related
 *  - Iceberg — https://github.com/apache/iceberg/blob/main/api/src/main/java/org/apache/iceberg/catalog/Catalog.java
 *  - Calcite — https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/schema/Schema.java
 */
public interface Catalog {

    /**
     * Returns the catalog name.
     */
    public fun getName(): String

    /**
     * Get a table by name.
     *
     * @param name  The case-sensitive [Table] name.
     * @return The [Table] or null if not found.
     */
    public fun getTable(name: Name): Table? = null

    /**
     * List top-level tables.
     */
    public fun listTables(): Collection<Name> = listTables(Namespace.root())

    /**
     * List all tables under this namespace.
     *
     * @param namespace
     */
    public fun listTables(namespace: Namespace): Collection<Name> = emptyList()

    /**
     * List top-level namespaces from the catalog.
     */
    public fun listNamespaces(): Collection<Namespace> = listNamespaces(Namespace.root())

    /**
     * List all child namespaces from the namespace.
     *
     * @param namespace
     */
    public fun listNamespaces(namespace: Namespace): Collection<Namespace> = emptyList()

    /**
     * Get a routine's variants by name.
     *
     * @param name  The case-sensitive [Routine] name.
     * @return A collection of all [Routine]s in the current namespace with this name.
     */
    public fun getRoutines(name: Name): Collection<Routine> = emptyList()
}

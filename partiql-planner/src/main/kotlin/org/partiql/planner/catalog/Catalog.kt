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
     */
    public fun getTable(session: Session, name: Name): Table? = null

    /**
     * Given an [Identifier], returns a [Table.Handle] that corresponds to the longest-available requested path.
     *
     * For example, given a table named "Table" located within Catalog "AWS" and Namespace "a".b"."c", a user could
     * call [getTableHandle] with the identifier "a"."b"."c"."Table". The returned [Table.Handle] will contain the table
     * representation and the matching path: "a"."b"."c"."Table"
     *
     * As another example, consider a table within a [Namespace] that may be a struct with nested attributes.
     * A user could call [getTableHandle] with the identifier "a"."b"."c"."Table"."x". In the Namespace, only table
     * "Table" exists. Therefore, this method will return a [Table.Handle] with the "Table" representation and the
     * matching path: "a"."b"."c"."Table".
     *
     * IMPORTANT: The returned [Table.Handle.namespace] must be correct for correct evaluation.
     *
     * If the [Identifier] does not correspond to an existing [Table], implementers should return null.
     */
    public fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? = null

    /**
     * List top-level tables.
     */
    public fun listTables(session: Session): Collection<Name> = listTables(session, Namespace.root())

    /**
     * List all tables under this namespace.
     */
    public fun listTables(session: Session, namespace: Namespace): Collection<Name> = emptyList()

    /**
     * List top-level namespaces from the catalog.
     */
    public fun listNamespaces(session: Session): Collection<Namespace> = listNamespaces(session, Namespace.root())

    /**
     * List all child namespaces from the namespace.
     *
     * @param namespace
     */
    public fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> = emptyList()

    /**
     * Get a routine's variants by name.
     *
     * @param name  The case-sensitive [Routine] name.
     * @return A collection of all [Routine]s in the current namespace with this name.
     */
    public fun getRoutines(session: Session, name: Name): Collection<Routine> = emptyList()
}

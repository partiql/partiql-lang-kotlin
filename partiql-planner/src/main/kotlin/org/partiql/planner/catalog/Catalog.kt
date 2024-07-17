package org.partiql.planner.catalog

import org.partiql.types.PType

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
     * Creates a table with the given name (possibly in a namespace).
     */
    public fun createTable(session: Session, name: Name, schema: PType)

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

    /**
     * Factory methods.
     */
    public companion object {

        /**
         * Returns a default [Catalog] implementation based upon an in-memory tree.
         *
         * @param name The name of the catalog.
         */
        @JvmStatic
        public fun standard(name: String): Catalog = Standard(name)
    }

    /**
     * A default [Catalog] implementation based upon an in-memory tree.
     */
    private class Standard(val name: String) : Catalog {

        private val root: Tree = Tree(null, mutableMapOf())

        private class Tree(
            private val table: Table?,
            private val children: MutableMap<String, Tree>,
        ) {
            fun contains(name: String) = children.contains(name)
            fun get(name: String): Tree? = children[name]
            fun getOrPut(name: String): Tree = children.getOrPut(name) { Tree(null, mutableMapOf()) }
        }

        override fun getName(): String = name

        override fun getTable(session: Session, name: Name): Table? {
            return null
        }

        override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
            if (identifier.hasQualifier()) {
                error("Catalog does not support qualified table names")
            }
            var match: Table? = null
            val id = identifier.getIdentifier()
            for (table in tree.values) {
                if (id.matches(table.getName())) {
                    if (match == null) {
                        match = table
                    } else {
                        error("Ambiguous table name: $name")
                    }
                }
            }
            return match
        }

        override fun createTable(session: Session, name: Name, schema: PType) {
            TODO("Not yet implemented")
        }

        // TODO
        override fun listTables(session: Session, namespace: Namespace): Collection<Name> {
            return emptyList()
        }

        // TODO
        override fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> {
            return emptyList()
        }

        // TODO
        override fun getRoutines(session: Session, name: Name): Collection<Routine> {
            return emptyList()
        }
    }
}

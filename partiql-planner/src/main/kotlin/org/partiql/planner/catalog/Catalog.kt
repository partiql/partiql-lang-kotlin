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
     *
     * @param name  The case-sensitive [Table] name.
     * @return The [Table] or null if not found.
     */
    public fun getTable(session: Session, name: Name): Table? = null

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
     * Factory methods and builder.
     */
    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Java-style builder for a default [Catalog] implementation.
     *
     */
    public class Builder {

        private var name: String? = null
        private var tables = mutableMapOf<String, Table>()

        public fun name(name: String): Builder {
            this.name = name
            return this
        }

        public fun createTable(name: String, schema: PType): Builder {
            this.tables[name] = Table.of(name, schema)
            return this
        }

        public fun createTable(name: Name, schema: PType): Builder {
            if (name.hasNamespace()) {
                error("Table name must not have a namespace: $name")
            }
            this.tables[name.getName()] = Table.of(name.getName(), schema)
            return this
        }

        public fun build(): Catalog {

            val name = this.name ?: throw IllegalArgumentException("Catalog name must be provided")

            return object : Catalog {

                override fun getName(): String = name

                override fun getTable(session: Session, name: Name): Table? {
                    if (name.hasNamespace()) {
                        return null
                    }
                    return tables[name.getName()]
                }

                override fun getTable(session: Session, identifier: Identifier): Table? {
                    if (identifier.hasQualifier()) {
                        return null
                    }
                    var match: Table? = null
                    val id = identifier.getIdentifier()
                    for (table in tables.values) {
                        if (id.matches(table.getName())) {
                           if (match == null)  {
                               match = table
                           } else {
                               error("Ambiguous table name: $name")
                           }
                        }
                    }
                    return match
                }

                override fun listTables(session: Session): Collection<Name> {
                    return tables.values.map { Name.of(it.getName()) }
                }

                override fun listTables(session: Session, namespace: Namespace): Collection<Name> {
                    if (!namespace.isEmpty()) {
                       return emptyList()
                    }
                    return tables.values.map { Name.of(it.getName()) }
                }

                override fun listNamespaces(session: Session): Collection<Namespace> {
                    return emptyList()
                }

                override fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> {
                    return emptyList()
                }

                override fun getRoutines(session: Session, name: Name): Collection<Routine> = emptyList()
            }
        }
    }
}

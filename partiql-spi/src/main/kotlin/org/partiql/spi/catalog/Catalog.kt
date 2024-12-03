package org.partiql.spi.catalog

import org.partiql.spi.catalog.impl.StandardCatalog
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Function

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
    public fun getTable(session: Session, name: Name): Table? {
        return null
    }

    /**
     * Get a table by identifier.
     *
     * !! IMPORTANT !!
     *
     * The returned [Table] MUST be matched following the PartiQL name resolution rules, in short:
     *
     *  1. If the identifier has multiple matches, choose the LONGEST matching name.
     *  2. If the identifier is ambiguous (multiple matches with same length), throw an ambiguous identifier error.
     *  3. If there are NO matches, return null.
     *
     * Example,
     *
     *  1. Consider a table with name `a.b.c.Example` -> Namespace=["a","b","c"], Table="Example"
     *  2. Invoke getTable("a"."b"."c"."Example"."x")
     *  3. The implementation MUST match "a"."b"."c"."Example" to a.b.c.Example (note "x" does not match a table)
     */
    public fun getTable(session: Session, identifier: Identifier): Table? {
        return null
    }

    /**
     * Returns a collection of scalar functions in this catalog with the given name, or an empty list if none.
     */
    public fun getFunctions(session: Session, name: String): Collection<Function> {
        return emptyList()
    }

    /**
     * Returns a collection of aggregation functions in this catalog with the given name, or an empty list if none.
     */
    public fun getAggregations(session: Session, name: String): Collection<Aggregation> {
        return emptyList()
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Lombok java-style builder
     */
    public class Builder internal constructor() {

        private var name: String? = null
        private var tables: MutableMap<Name, Table> = mutableMapOf()

        public fun name(name: String): Builder {
            this.name = name
            return this
        }

        public fun define(table: Table): Builder {
            tables[table.getName()] = table
            return this
        }

        public fun build(): Catalog {
            // Validate builder parameters
            val name = this.name ?: throw IllegalStateException("Catalog name cannot be null")
            return StandardCatalog(name, tables)
        }
    }
}

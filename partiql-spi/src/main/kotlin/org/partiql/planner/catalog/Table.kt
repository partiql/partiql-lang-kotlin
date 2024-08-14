package org.partiql.planner.catalog

import org.partiql.types.PType

/**
 * In PartiQL, a [Table] can take on any type and is not necessarily rows+columns.
 */
public interface Table {

    /**
     * Handle holds both a table and its resolved name within its respective catalog.
     *
     * Note: This replaces ConnectorObjectHandle from versions < 1.0
     */
    public class Handle(
        @JvmField public val name: Name,
        @JvmField public val table: Table,
    )

    /**
     * The table's name.
     */
    public fun getName(): String

    /**
     * The table's schema.
     */
    public fun getSchema(): PType = PType.dynamic()

    /**
     * Factory methods and builder.
     */
    public companion object {

        /**
         * Create a simple table with a name and schema.
         */
        @JvmStatic
        public fun of(name: String, schema: PType = PType.dynamic()): Table = object : Table {
            override fun getName(): String = name
            override fun getSchema(): PType = schema
        }

        /**
         * Returns the Java-style builder.
         */
        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Java-style builder for a default Table implementation.
     */
    public class Builder {

        private var name: String? = null
        private var schema: PType = PType.dynamic()

        public fun name(name: String): Builder {
            this.name = name
            return this
        }

        public fun schema(schema: PType): Builder {
            this.schema = schema
            return this
        }

        public fun build(): Table {
            // Validate builder parameters
            val name = this.name ?: throw IllegalStateException("Table name cannot be null")
            // Default implementation
            return object : Table {
                override fun getName(): String = name
                override fun getSchema(): PType = schema
            }
        }
    }
}

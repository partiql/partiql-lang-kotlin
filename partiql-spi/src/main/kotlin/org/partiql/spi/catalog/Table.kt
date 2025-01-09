package org.partiql.spi.catalog

import org.partiql.spi.catalog.impl.StandardTable
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * In PartiQL, a [Table] can take on any type and is not necessarily a collection of rows.
 */
public interface Table {

    /**
     * The table's name.
     */
    public fun getName(): Name

    /**
     * The table's schema.
     */
    public fun getSchema(): PType = PType.dynamic()

    /**
     * The table's data.
     */
    public fun getDatum(): Datum = Datum.nullValue()

    /**
     * Factory methods and builder.
     */
    public companion object {

        @JvmStatic
        public fun empty(name: String): Table = StandardTable(
            name = Name.of(name),
            schema = PType.dynamic(),
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with dynamic schema.
         */
        @JvmStatic
        public fun empty(name: Name): Table = StandardTable(
            name = name,
            schema = PType.dynamic(),
            datum = Datum.nullValue(),
        )

        @JvmStatic
        public fun empty(name: String, schema: PType): Table = StandardTable(
            name = Name.of(name),
            schema = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with known schema.
         */
        @JvmStatic
        public fun empty(name: Name, schema: PType): Table = StandardTable(
            name = name,
            schema = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create a table from a Datum with dynamic schema.
         */
        @JvmStatic
        public fun standard(name: Name, datum: Datum): Table = StandardTable(
            name = name,
            schema = PType.dynamic(),
            datum = datum,
        )

        /**
         * Create a table from a Datum with known schema.
         */
        @JvmStatic
        public fun standard(name: Name, schema: PType, datum: Datum): Table = StandardTable(
            name = name,
            schema = schema,
            datum = datum,
        )

        /**
         * Returns the Java-style builder.
         */
        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Lombok java-style builder for a default Table implementation.
     */
    public class Builder {

        private var name: String? = null
        private var schema: PType = PType.dynamic()
        private var datum: Datum = Datum.nullValue()

        public fun name(name: String): Builder {
            this.name = name
            return this
        }

        public fun schema(schema: PType): Builder {
            this.schema = schema
            return this
        }

        public fun datum(datum: Datum): Builder {
            this.datum = datum
            return this
        }

        public fun build(): Table {
            // Validate builder parameters
            val name = this.name ?: throw IllegalStateException("Table name cannot be null")
            return StandardTable(Name.of(name), schema, datum)
        }
    }
}

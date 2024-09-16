package org.partiql.spi.catalog

import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * In PartiQL, a [Table] can take on any type and is not necessarily a collection of rows.
 */
public interface Table {

    /**
     * Handle holds both a table and its resolved name within its respective catalog.
     *
     * TODO DELETE ME AS A TABLE ALREADY CONTAINS ITS NAME.
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
        public fun empty(name: String): Table = _Table(
            name = Name.of(name),
            schema = PType.dynamic(),
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with dynamic schema.
         */
        @JvmStatic
        public fun empty(name: Name): Table = _Table(
            name = name,
            schema = PType.dynamic(),
            datum = Datum.nullValue(),
        )

        @JvmStatic
        public fun empty(name: String, schema: PType): Table = _Table(
            name = Name.of(name),
            schema = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with known schema.
         */
        @JvmStatic
        public fun empty(name: Name, schema: PType): Table = _Table(
            name = name,
            schema = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create a table from a Datum with dynamic schema.
         */
        @JvmStatic
        public fun of(name: Name, datum: Datum): Table = _Table(
            name = name,
            schema = PType.dynamic(),
            datum = datum,
        )

        /**
         * Create a table from a Datum with known schema.
         */
        @JvmStatic
        public fun of(name: Name, datum: Datum, schema: PType): Table = _Table(
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
     * Java-style builder for a default Table implementation.
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
            return _Table(Name.of(name), schema, datum)
        }
    }

    /**
     * An internal, standard table implementation backed by simple fields.
     *
     * @constructor
     * All arguments default constructor.
     *
     * @param name
     * @param schema
     * @param datum
     */
    @Suppress("ClassName")
    private class _Table(name: Name, schema: PType, datum: Datum) : Table {

        // DO NOT USE FINAL
        private var _name = name
        private var _schema = schema
        private var _datum = datum

        override fun getName(): Name = _name
        override fun getSchema(): PType = _schema
        override fun getDatum(): Datum = _datum
    }
}

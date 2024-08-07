package org.partiql.plugins.memory

import org.partiql.eval.value.Datum
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Table
import org.partiql.spi.connector.ConnectorBinding
import org.partiql.types.PType

public class MemoryTable private constructor(
    private val name: Name,
    private val schema: PType,
    private val datum: Datum,
) : Table, ConnectorBinding {

    override fun getName(): Name = name
    override fun getSchema(): PType = schema
    override fun getDatum(): Datum = datum

    public companion object {

        @JvmStatic
        public fun empty(name: String): MemoryTable = MemoryTable(
            name = Name.of(name),
            schema = PType.typeDynamic(),
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with dynamic schema.
         */
        @JvmStatic
        public fun empty(name: Name): MemoryTable = MemoryTable(
            name = name,
            schema = PType.typeDynamic(),
            datum = Datum.nullValue(),
        )

        @JvmStatic
        public fun empty(name: String, schema: PType): MemoryTable = MemoryTable(
            name = Name.of(name),
            schema = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with known schema.
         */
        @JvmStatic
        public fun empty(name: Name, schema: PType): MemoryTable = MemoryTable(
            name = name,
            schema = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create a table from a Datum with dynamic schema.
         */
        @JvmStatic
        public fun of(name: Name, datum: Datum): MemoryTable = MemoryTable(
            name = name,
            schema = PType.typeDynamic(),
            datum = datum,
        )

        /**
         * Create a table from a Datum with known schema.
         */
        @JvmStatic
        public fun of(name: Name, datum: Datum, schema: PType): MemoryTable = MemoryTable(
            name = name,
            schema = schema,
            datum = datum,
        )
    }
}

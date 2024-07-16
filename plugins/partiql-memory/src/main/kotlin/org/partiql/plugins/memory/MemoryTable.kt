package org.partiql.plugins.memory

import org.partiql.eval.bindings.Binding
import org.partiql.eval.value.Datum
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Table
import org.partiql.types.PType

public class MemoryTable private constructor(
    private val name: Name,
    private val type: PType,
    private val datum: Datum,
) : Table, Binding {

    override fun getName(): Name = name
    override fun getSchema(): PType = type
    override fun getDatum(): Datum = datum

    public companion object {

        /**
         * Create an empty table with dynamic schema.
         */
        @JvmStatic
        public fun empty(name: Name): MemoryTable = MemoryTable(
            name = name,
            type = PType.typeDynamic(),
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with known schema.
         */
        @JvmStatic
        public fun empty(name: Name, schema: PType): MemoryTable = MemoryTable(
            name = name,
            type = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create a table from a Datum with dynamic schema.
         */
        @JvmStatic
        public fun of(name: Name, value: Datum): MemoryTable = MemoryTable(
            name = name,
            type = PType.typeDynamic(),
            datum = value,
        )

        /**
         * Create a table from a Datum with known schema.
         */
        @JvmStatic
        public fun of(name: Name, value: Datum, schema: PType): MemoryTable = MemoryTable(
            name = name,
            type = schema,
            datum = value,
        )
    }
}

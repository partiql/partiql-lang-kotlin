package org.partiql.plugins.memory

import org.partiql.eval.bindings.Binding
import org.partiql.eval.value.Datum
import org.partiql.planner.metadata.Table
import org.partiql.types.PType

public class MemoryTable private constructor(
    private val type: PType,
    private val datum: Datum,
) : Table, Binding {

    override fun getKind(): Table.Kind = Table.Kind.TABLE
    override fun getSchema(): PType = type
    override fun getDatum(): Datum = datum

    public companion object {

        /**
         * Create an empty table with dynamic schema.
         */
        @JvmStatic
        public fun empty(): MemoryTable = MemoryTable(
            type = PType.typeDynamic(),
            datum = Datum.nullValue(),
        )

        /**
         * Create an empty table with known schema.
         */
        @JvmStatic
        public fun empty(schema: PType): MemoryTable = MemoryTable(
            type = schema,
            datum = Datum.nullValue(),
        )

        /**
         * Create a table from a Datum with dynamic schema.
         */
        @JvmStatic
        public fun of(value: Datum): MemoryTable = MemoryTable(
            type = PType.typeDynamic(),
            datum = value,
        )

        /**
         * Create a table from a Datum with known schema.
         */
        @JvmStatic
        public fun of(value: Datum, schema: PType): MemoryTable = MemoryTable(
            type = schema,
            datum = value,
        )
    }
}

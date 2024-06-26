package org.partiql.plugins.memory

import com.amazon.ionelement.api.IonElement
import org.partiql.planner.metadata.Table
import org.partiql.types.PType

public class MemoryTable private constructor(
    private val type: PType,
    private val data: IonElement?,
) : Table {

    override fun getKind(): Table.Kind = Table.Kind.TABLE

    override fun getSchema(): PType = type

    public companion object {

        /**
         * Create an empty table with dynamic schema.
         */
        @JvmStatic
        public fun empty(): MemoryTable = MemoryTable(
            type = PType.typeDynamic(),
            data = null,
        )

        /**
         * Create an empty table with known schema.
         */
        @JvmStatic
        public fun empty(schema: PType): MemoryTable = MemoryTable(
            type = schema,
            data = null,
        )

        /**
         * Create a table from an IonElement with dynamic schema.
         */
        @JvmStatic
        public fun of(value: IonElement): MemoryTable = MemoryTable(
            type = PType.typeDynamic(),
            data = value,
        )

        /**
         * Create a table from an IonElement with known schema.
         */
        @JvmStatic
        public fun of(value: IonElement, schema: PType): MemoryTable = MemoryTable(
            type = schema,
            data = value,
        )
    }
}

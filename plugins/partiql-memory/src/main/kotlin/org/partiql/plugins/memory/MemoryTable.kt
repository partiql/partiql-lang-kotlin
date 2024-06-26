package org.partiql.plugins.memory

import org.partiql.planner.metadata.Table
import org.partiql.types.PType

public class MemoryTable private constructor(private val schema: PType) : Table {

    override fun getKind(): Table.Kind = Table.Kind.TABLE

    override fun getSchema(): PType = schema

    public companion object {

        @JvmStatic
        public fun of(type: PType): MemoryTable = MemoryTable(type)
    }
}

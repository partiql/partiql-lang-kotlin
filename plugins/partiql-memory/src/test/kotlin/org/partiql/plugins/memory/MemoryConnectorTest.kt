package org.partiql.plugins.memory

import org.junit.jupiter.api.Test

class MemoryConnectorTest {

    @Test
    fun sanity() {
        val connector = MemoryConnector.builder()
            .name("default")
            .createTable(MemoryTable.empty("a"))
            .createTable(MemoryTable.empty("b"))
            .createTable(MemoryTable.empty("c"))
            .build()
        val catalog = connector.getCatalog()
        assert(catalog.listTables().size == 3)
        assert(catalog.listNamespaces().size == 0)
    }
}

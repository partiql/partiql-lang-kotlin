package org.partiql.plugins.memory

import org.junit.jupiter.api.Test
import org.partiql.spi.catalog.Session

class MemoryConnectorTest {

    @Test
    fun sanity() {
        val session = Session.empty("")
        val connector = MemoryConnector.builder()
            .name("default")
            .define(MemoryTable.empty("a"))
            .define(MemoryTable.empty("b"))
            .define(MemoryTable.empty("c"))
            .build()
        val catalog = connector.getCatalog()
        assert(catalog.listTables(session).size == 3)
        assert(catalog.listNamespaces(session).size == 0)
    }
}

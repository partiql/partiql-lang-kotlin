package org.partiql.plugins.memory

import org.junit.jupiter.api.Test
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session

class MemoryConnectorTest {

    @Test
    fun sanity() {
        val session = Session.empty()
        val catalog = MemoryCatalog.builder()
            .name("default")
            .define(MemoryTable.empty("a"))
            .define(MemoryTable.empty("b"))
            .define(MemoryTable.empty("c"))
            .build()
        assert(catalog.getTable(session, Name.of("a")) != null)
        assert(catalog.getTable(session, Name.of("b")) != null)
        assert(catalog.getTable(session, Name.of("c")) != null)
    }
}

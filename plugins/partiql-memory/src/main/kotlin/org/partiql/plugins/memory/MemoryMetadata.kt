package org.partiql.plugins.memory

import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata

internal class MemoryMetadata(private val catalog: MemoryCatalog) : ConnectorMetadata {

    override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
        return catalog.find(path)
    }
}

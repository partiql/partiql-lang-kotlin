package org.partiql.plugins.memory

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType

class MemoryConnector(
    val catalog: MemoryCatalog
) : Connector {

    companion object {
        const val CONNECTOR_NAME = "memory"
    }

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = Metadata()

    class Factory(private val provider: MemoryCatalog.Provider) : Connector.Factory {
        override fun getName(): String = CONNECTOR_NAME

        override fun create(catalogName: String, config: StructElement): Connector {
            val catalog = provider[catalogName]
            return MemoryConnector(catalog)
        }
    }

    inner class Metadata : ConnectorMetadata {

        override fun getObjectType(session: ConnectorSession, handle: ConnectorObjectHandle): StaticType? {
            val obj = handle.value as MemoryObject
            return obj.type
        }

        override fun getObjectHandle(session: ConnectorSession, path: BindingPath): ConnectorObjectHandle? {
            val value = catalog.lookup(path) ?: return null
            return ConnectorObjectHandle(
                absolutePath = ConnectorObjectPath(value.path),
                value = value,
            )
        }
    }
}

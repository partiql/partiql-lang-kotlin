package org.partiql.plugins.memory

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType

class MemoryConnector private constructor(
    val catalog: MemoryCatalog,
    val bindings: MemoryBindings,
) : Connector {

    companion object {
        const val CONNECTOR_NAME = "memory"
    }

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = Metadata()

    override fun getBindings(): ConnectorBindings = bindings

    class Factory(
        private val provider: MemoryCatalog.Provider,
        private val data: StructElement?,
    ) : Connector.Factory {

        override val name: String = CONNECTOR_NAME

        override fun create(catalogName: String, config: StructElement): Connector {
            val catalog = provider[catalogName]
            val bindings = if (data != null) MemoryBindings.load(catalog, data) else MemoryBindings.empty
            return MemoryConnector(catalog, bindings)
        }
    }

    inner class Metadata : ConnectorMetadata {

        override fun getObjectType(session: ConnectorSession, handle: ConnectorObjectHandle): StaticType {
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

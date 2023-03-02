package org.partiql.cli.plugin.localdb2

import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession

class LocalConnector2 : Connector {
    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = LocalConnectorMetadata2()

    class Factory : Connector.Factory {
        override fun getName(): String = "localdb2"
        override fun create(): Connector = LocalConnector2()
    }
}

package org.partiql.cli.plugin.localdb

import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession

class LocalConnector : Connector {
    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = LocalConnectorMetadata()

    class Factory : Connector.Factory {
        override fun getName(): String = "localdb"
        override fun create(): Connector = LocalConnector()
    }
}

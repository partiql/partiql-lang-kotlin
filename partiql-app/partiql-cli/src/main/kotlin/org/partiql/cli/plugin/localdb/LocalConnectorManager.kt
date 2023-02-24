package org.partiql.cli.plugin.localdb

import org.partiql.spi.connector.ConnectorManager
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession

class LocalConnectorManager : ConnectorManager {
    override fun getMetadata(session: ConnectorSession): ConnectorMetadata {
        return LocalConnectorMetadata()
    }
}

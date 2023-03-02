package org.partiql.cli.plugin.localdb2

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector

class LocalPlugin2 : Plugin {
    override fun getConnectorFactories(): List<Connector.Factory> = listOf(LocalConnector2.Factory())
}

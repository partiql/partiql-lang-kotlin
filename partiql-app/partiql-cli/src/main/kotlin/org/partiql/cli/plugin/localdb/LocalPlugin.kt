package org.partiql.cli.plugin.localdb

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector

class LocalPlugin : Plugin {
    override fun getConnectorFactories(): List<Connector.Factory> = listOf(LocalConnector.Factory())
}

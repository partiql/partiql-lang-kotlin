package org.partiql.lang.plugin

import org.partiql.spi.connector.ConnectorManagerFactory

public class PluginManager(
    public val connectorFactories: List<ConnectorManagerFactory>
) {
    public fun installPlugin(): Unit = TODO()
}

package org.partiql.spi.connector

import org.partiql.spi.connector.Connector

public class PluginManager(
    public val connectorFactories: List<Connector.Factory>
) {
    public fun installPlugin(): Unit = TODO()
}

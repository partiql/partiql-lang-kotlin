package org.partiql.lang.plugin

import org.partiql.spi.connector.Connector

public class PluginManager(
    public val connectorFactories: List<Connector.Factory>
) {
    public fun installPlugin(): Unit = TODO()
}

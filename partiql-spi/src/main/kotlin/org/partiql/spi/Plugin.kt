package org.partiql.spi

import org.partiql.spi.connector.Connector

public interface Plugin {
    public fun getConnectorFactories(): List<Connector.Factory>
}

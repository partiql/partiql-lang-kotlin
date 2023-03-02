package org.partiql.spi

import org.partiql.spi.connector.Connector2

public interface Plugin2 {
    public fun getConnectorFactories(): List<Connector2.Factory>
}

package org.partiql.lang.plugin

import org.partiql.spi.connector.ConnectorManager

public class Catalog(
    public val connectorName: String,
    public val connectorManager: ConnectorManager
)

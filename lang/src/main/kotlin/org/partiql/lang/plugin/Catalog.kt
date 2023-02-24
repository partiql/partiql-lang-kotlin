package org.partiql.lang.plugin

import org.partiql.spi.connector.Connector

public class Catalog(
    public val connectorName: String,
    public val connector: Connector
)

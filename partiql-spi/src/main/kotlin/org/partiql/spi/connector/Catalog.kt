package org.partiql.spi.connector

public class Catalog(
    public val connectorName: String,
    public val connector: Connector
)
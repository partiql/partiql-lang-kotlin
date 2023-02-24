package org.partiql.spi.connector

public interface ConnectorManager {
    public fun getMetadata(session: ConnectorSession): ConnectorMetadata
}

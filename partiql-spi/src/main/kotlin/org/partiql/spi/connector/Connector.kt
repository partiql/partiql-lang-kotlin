package org.partiql.spi.connector

public interface Connector {
    public fun getMetadata(session: ConnectorSession): ConnectorMetadata

    public interface Factory {
        public fun getName(): String
        public fun create(): Connector
    }
}

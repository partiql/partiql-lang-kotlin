package org.partiql.spi.connector

public interface Connector2 {
    public fun getMetadata(session: ConnectorSession): ConnectorMetadata2

    public interface Factory {
        public fun getName(): String
        public fun create(): Connector2
    }
}

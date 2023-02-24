package org.partiql.spi.connector

interface ConnectorManagerFactory {
    public fun getName(): String
    public fun create(): ConnectorManager
}

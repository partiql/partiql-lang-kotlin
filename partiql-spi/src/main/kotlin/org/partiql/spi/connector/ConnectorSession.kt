package org.partiql.spi.connector

public interface ConnectorSession {
    public fun getQueryId(): String
    public fun getUserId(): String
}

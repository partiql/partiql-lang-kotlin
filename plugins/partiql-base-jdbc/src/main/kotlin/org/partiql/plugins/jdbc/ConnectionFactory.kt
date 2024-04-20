package org.partiql.plugins.jdbc

import org.partiql.spi.connector.ConnectorSession
import java.sql.Connection
import java.sql.Driver
import java.sql.SQLException
import java.util.*

public interface ConnectionFactory : AutoCloseable {
    @Throws(SQLException::class)
    public fun openConnection(session: ConnectorSession?): Connection?
}


public class DriverConnectionFactory(
    public val driver: Driver,
    public val connectionUrl: String,
) : ConnectionFactory {

    override fun openConnection(session: ConnectorSession?): Connection? {
        return driver.connect(connectionUrl, null)
    }

    override fun close() {}
}


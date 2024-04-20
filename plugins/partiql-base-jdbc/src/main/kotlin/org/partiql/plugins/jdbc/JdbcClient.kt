package org.partiql.plugins.jdbc

import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import java.sql.Connection
import java.sql.ResultSetMetaData

/**
 * This interface serves as a contract for interacting with Jdbc data source.
 */
public interface JdbcClient {

    public fun getJdbcSchema(session: ConnectorSession) : Set<String>

    public fun getJdbcTable(session: ConnectorSession, catalog: String?, schema: String?, tableName: String?): JdbcTable?

    public fun getJdbcColumns(session: ConnectorSession, connection: Connection, metadata: ResultSetMetaData): List<JdbcColumn>

    public fun toColumnMapping(session: ConnectorSession, connection: Connection, jdbcType: JdbcType): ColumnMapping

    public fun getBinding(session: ConnectorSession) : ConnectorBindings
}

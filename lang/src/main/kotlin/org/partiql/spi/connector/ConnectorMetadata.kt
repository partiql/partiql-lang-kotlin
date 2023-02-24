package org.partiql.spi.connector

import org.partiql.lang.eval.BindingName
import org.partiql.spi.sources.TableSchema

public interface ConnectorMetadata {
    public fun schemaExists(session: ConnectorSession, name: BindingName): Boolean
    public fun getTableSchema(session: ConnectorSession, handle: ConnectorTableHandle): TableSchema?
    public fun getTableHandle(session: ConnectorSession, schema: BindingName, table: BindingName): ConnectorTableHandle?
}

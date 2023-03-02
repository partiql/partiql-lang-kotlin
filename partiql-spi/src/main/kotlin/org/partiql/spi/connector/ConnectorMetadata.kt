package org.partiql.spi.connector

import org.partiql.catalog.Catalog
import org.partiql.spi.BindingName
import org.partiql.spi.sources.TableSchema

public interface ConnectorMetadata {
    public fun getObjectHandle(session: ConnectorSession, objectName: BindingName): ConnectorTableHandle?
    public fun schemaExists(session: ConnectorSession, name: BindingName): Boolean
    public fun getTableSchema(session: ConnectorSession, handle: ConnectorTableHandle): TableSchema?
    public fun getTableHandle(session: ConnectorSession, schema: BindingName?, table: BindingName): ConnectorTableHandle?
}

package org.partiql.spi.connector

import org.partiql.spi.BindingName
import org.partiql.spi.sources.TableSchema
import org.partiql.catalog.Catalog

public interface ConnectorMetadata2 {
    public fun getCatalog(session: ConnectorSession): Catalog
    public fun schemaExists(session: ConnectorSession, name: BindingName): Boolean
    public fun getTableSchema(session: ConnectorSession, handle: ConnectorTableHandle): TableSchema?
    public fun getTableHandle(session: ConnectorSession, schema: BindingName?, table: BindingName): ConnectorTableHandle?
}

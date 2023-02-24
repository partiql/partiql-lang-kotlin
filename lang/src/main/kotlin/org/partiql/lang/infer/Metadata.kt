package org.partiql.lang.infer

import org.partiql.spi.sources.TableSchema

public interface Metadata {
    public fun catalogExists(session: Session, catalogName: String): Boolean
    public fun schemaExists(session: Session, catalogName: String, schemaName: String): Boolean
    public fun getTableHandle(session: Session, tableName: QualifiedObjectName): TableHandle?
    public fun getTableSchema(session: Session, handle: TableHandle): TableSchema
}

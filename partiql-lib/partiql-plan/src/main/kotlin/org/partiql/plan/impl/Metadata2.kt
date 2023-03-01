package org.partiql.plan.impl

import org.partiql.plan.PlannerSession
import org.partiql.plan.PlannerSession2
import org.partiql.plan.TableHandle
import org.partiql.catalog.Catalog
import org.partiql.spi.sources.TableSchema

internal interface Metadata2 {
    public fun getCatalog(session: PlannerSession2): Catalog
    public fun schemaExists(session: PlannerSession, catalogName: String, schemaName: String): Boolean
    public fun getTableHandle(session: PlannerSession, tableName: QualifiedObjectName): TableHandle?
    public fun getTableSchema(session: PlannerSession, handle: TableHandle): TableSchema
}

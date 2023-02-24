package org.partiql.plan.impl

import org.partiql.plan.PlannerSession
import org.partiql.plan.TableHandle
import org.partiql.spi.sources.TableSchema

internal interface Metadata {
    public fun catalogExists(session: PlannerSession, catalogName: String): Boolean
    public fun schemaExists(session: PlannerSession, catalogName: String, schemaName: String): Boolean
    public fun getTableHandle(session: PlannerSession, tableName: QualifiedObjectName): TableHandle?
    public fun getTableSchema(session: PlannerSession, handle: TableHandle): TableSchema
}

package org.partiql.plan.impl

import org.partiql.plan.PlannerSession2
import org.partiql.plan.TableHandle
import org.partiql.catalog.Catalog
import org.partiql.plan.passes.TableHandle2
import org.partiql.spi.sources.TableSchema

internal interface Metadata2 {
    public fun getCatalog(session: PlannerSession2): Catalog
    public fun getTableHandle(session: PlannerSession2, name: org.partiql.lang.eval.BindingName): TableHandle2?
    public fun getTableSchema(session: PlannerSession2, handle: TableHandle2): TableSchema
}

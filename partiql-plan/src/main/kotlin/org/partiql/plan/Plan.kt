package org.partiql.plan

import org.partiql.planner.catalog.Catalog

public interface Plan {

    public fun getVersion(): Version

    public fun getCatalogs(): List<Catalog>

    public fun getStatement(): Statement
}

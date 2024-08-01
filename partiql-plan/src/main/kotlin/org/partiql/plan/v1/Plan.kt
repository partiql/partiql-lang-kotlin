package org.partiql.plan.v1

import org.partiql.plan.Catalog

public interface Plan {

    public fun getVersion(): Version

    public fun getCatalogs(): List<Catalog>

    public fun getStatement(): Statement
}

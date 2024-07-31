package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
interface RelSortSpec {

    /**
     * TODO REPLACE WITH COLUMN INDEX
     */
    public fun getRex(): Rex

    public fun getOrder(): Order

    public fun getNulls(): Nulls

    public enum class Order {
        ASC,
        DESC,
        OTHER,
    }

    public enum class Nulls {
        FIRST,
        LAST,
        OTHER,
    }
}

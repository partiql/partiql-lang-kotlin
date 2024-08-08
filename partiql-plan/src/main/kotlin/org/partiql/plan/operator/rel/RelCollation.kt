package org.partiql.plan.operator.rel

import org.partiql.plan.operator.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelCollation {

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

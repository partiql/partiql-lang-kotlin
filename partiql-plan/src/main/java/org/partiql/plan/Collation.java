package org.partiql.plan

import org.partiql.plan.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface Collation {

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

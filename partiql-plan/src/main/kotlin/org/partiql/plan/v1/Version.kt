package org.partiql.plan.v1

/**
 * Marker interface for some version structure.
 */
interface Version {

    /**
     * The only required method is toString.
     */
    public override fun toString(): String
}

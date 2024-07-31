package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelExcept : Rel {

    public fun isAll(): Boolean

    public fun getLeft(): Rel

    public fun getRight(): Rel
}

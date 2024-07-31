package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelDistinct : Rel {

    public fun getInput(): Rel
}

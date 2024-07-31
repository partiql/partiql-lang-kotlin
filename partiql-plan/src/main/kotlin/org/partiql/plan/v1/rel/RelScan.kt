package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelScan : Rel {

    public fun getInput(): Rel
}

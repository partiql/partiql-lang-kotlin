package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelScanIndexed : Rel {

    public fun getInput(): Rel
}

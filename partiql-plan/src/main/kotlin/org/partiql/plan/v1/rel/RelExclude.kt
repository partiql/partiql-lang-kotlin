package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelExclude : Rel {

    public fun getInput(): Rel

    public fun getPaths(): List<RelExcludePath>
}

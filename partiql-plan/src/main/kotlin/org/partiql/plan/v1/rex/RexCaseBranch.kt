package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexCaseBranch {

    public fun getCondition(): Rex

    public fun getResult(): Rex
}

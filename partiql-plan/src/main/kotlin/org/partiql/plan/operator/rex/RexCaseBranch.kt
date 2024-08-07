package org.partiql.plan.operator.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexCaseBranch {

    public fun getCondition(): Rex

    public fun getResult(): Rex
}

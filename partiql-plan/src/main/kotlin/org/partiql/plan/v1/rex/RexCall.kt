package org.partiql.plan.v1.rex

/**
 * Scalar function calls.
 */
public interface RexCall : Rex {

    /**
     * Returns the function to invoke.
     *
     * @return
     */
    public fun getFunction(): String

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>
}

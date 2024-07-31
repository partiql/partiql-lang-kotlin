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

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexCall(this, ctx)
}

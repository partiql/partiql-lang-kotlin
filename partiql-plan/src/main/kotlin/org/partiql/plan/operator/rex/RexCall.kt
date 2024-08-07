package org.partiql.plan.operator.rex

import org.partiql.types.PType

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

    override fun getOperands(): List<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCall(this, ctx)

    /**
     * Default [RexCall] implementation meant for extension.
     */
    abstract class Base(function: String, args: List<Rex>) : RexCall {

        // DO NOT USE FINAL
        private var _function = function
        private var _args = args

        final override fun getFunction(): String = _function

        final override fun getArgs(): List<Rex> = _args

        override fun getType(): PType {
            TODO("Function .getType()")
        }

        override fun getOperands(): List<Rex> = _args
    }
}

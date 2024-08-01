package org.partiql.plan.v1.rex

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

    public override fun getOperands(): List<Rex> = getArgs()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCall(this, ctx)

    /**
     * Default [RexCall] implementation meant for extension.
     */
    public abstract class Base(function: String, args: List<Rex>) : RexCall {

        // DO NOT USE FINAL
        private var _function = function
        private var _args = args

        public final override fun getFunction(): String = _function

        public final override fun getArgs(): List<Rex> = _args

        public override fun getType(): PType {
            TODO("Function .getType()")
        }

        public override fun getOperands(): List<Rex> = _args
    }
}

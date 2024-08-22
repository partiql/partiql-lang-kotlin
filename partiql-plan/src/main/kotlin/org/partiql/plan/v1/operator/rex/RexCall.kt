package org.partiql.plan.v1.operator.rex

import org.partiql.spi.fn.Fn
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
    public fun getFunction(): Fn

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCall(this, ctx)
}

/**
 * Default [RexCall] implementation meant for extension.
 */
internal class RexCallImpl(function: Fn, args: List<Rex>) : RexCall {

    // DO NOT USE FINAL
    private var _function = function
    private var _args = args

    override fun getFunction(): Fn = _function

    override fun getArgs(): List<Rex> = _args

    override fun getType(): PType {
        TODO("Function .getType()")
    }

    override fun getChildren(): Collection<Rex> = _args
}

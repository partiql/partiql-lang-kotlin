package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.spi.function.Function

/**
 * Logical operator for a scalar function call.
 */
public interface RexCall : Rex {

    /**
     * Returns the function to invoke.
     */
    public fun getFunction(): Function.Instance

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitCall(this, ctx)
}

/**
 * Default [RexCall] implementation meant for extension.
 */
internal class RexCallImpl(function: Function.Instance, args: List<Rex>) : RexCall {

    // DO NOT USE FINAL
    private var _function: Function.Instance = function
    private var _args: List<Rex> = args
    private var _type: RexType = RexType(function.returns)

    override fun getFunction(): Function.Instance = _function

    override fun getArgs(): List<Rex> = _args

    override fun getType(): RexType = _type

    override fun getChildren(): Collection<Rex> = _args
}

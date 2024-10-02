package org.partiql.plan.rex

import org.partiql.spi.function.Function
import org.partiql.types.PType

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

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCall(this, ctx)
}

/**
 * Default [RexCall] implementation meant for extension.
 *
 * DO NOT USE FINAL
 */
internal class RexCallImpl(
    private var function: Function.Instance,
    private var args: List<Rex>,
) : RexCall {

    override fun getFunction(): Function.Instance = function

    override fun getArgs(): List<Rex> = args

    override fun getType(): PType = function.returns

    override fun getChildren(): Collection<Rex> = args
}

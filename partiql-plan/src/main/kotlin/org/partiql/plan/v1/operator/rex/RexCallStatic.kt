package org.partiql.plan.v1.operator.rex

import org.partiql.spi.function.Function
import org.partiql.types.PType

/**
 * Logical operator for a scalar function call.
 */
public interface RexCallStatic : Rex {

    /**
     * Returns the function to invoke.
     *
     * @return
     */
    public fun getFunction(): Function

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCallStatic(this, ctx)
}

/**
 * Default [RexCallStatic] implementation meant for extension.
 */
internal class RexCallStaticImpl(function: Function, args: List<Rex>) : RexCallStatic {

    // DO NOT USE FINAL
    private var _function = function
    private var _args = args

    override fun getFunction(): Function = _function

    override fun getArgs(): List<Rex> = _args

    override fun getType(): PType {
        TODO("Function .getType()")
    }

    override fun getChildren(): Collection<Rex> = _args
}

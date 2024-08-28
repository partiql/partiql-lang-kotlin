package org.partiql.plan.v1.operator.rex

import org.partiql.spi.fn.Fn
import org.partiql.types.PType

/**
 * Logical operator for a dynamic scalar function call.
 */
public interface RexCallDynamic : Rex {

    /**
     * Returns the function to invoke.
     *
     * @return
     */
    public fun getFunctions(): List<Fn>

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCallDynamic(this, ctx)
}

/**
 * Default [RexCallDynamic] implementation meant for extension.
 */
internal class RexCallDynamicImpl(functions: List<Fn>, args: List<Rex>) : RexCallDynamic {

    // DO NOT USE FINAL
    private var _functions = functions
    private var _args = args

    override fun getFunctions(): List<Fn> = _functions

    override fun getArgs(): List<Rex> = _args

    override fun getType(): PType {
        TODO("Function .getType()")
    }

    override fun getChildren(): Collection<Rex> = _args
}

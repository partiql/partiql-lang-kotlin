package org.partiql.plan.rex

import org.partiql.spi.function.Function

/**
 * Logical operator for a dynamic dispatch call.
 */
public interface RexCallDynamic : Rex {

    /**
     * Dynamic function name.
     */
    public fun getName(): String

    /**
     * Returns the functions to dispatch to.
     */
    public fun getFunctions(): List<Function.Instance>

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
internal class RexCallDynamicImpl(
    private var name: String,
    private var functions: List<Function.Instance>,
    private var args: List<Rex>,
) : RexCallDynamic {

    // DO NOT USE FINAL
    private var _type: RexType = RexType.dynamic()

    override fun getName(): String = name

    override fun getFunctions(): List<Function.Instance> = functions

    override fun getArgs(): List<Rex> = args

    override fun getType(): RexType = _type

    override fun getChildren(): Collection<Rex> = args
}

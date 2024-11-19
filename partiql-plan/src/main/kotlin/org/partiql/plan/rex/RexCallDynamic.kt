package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.spi.function.Function
import org.partiql.types.PType

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
    public fun getFunctions(): List<Function>

    /**
     * Returns the list of function arguments.
     */
    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitCallDynamic(this, ctx)
}

/**
 * Default [RexCallDynamic] implementation meant for extension.
 */
internal class RexCallDynamicImpl(
    private var name: String,
    private var functions: List<Function>,
    private var args: List<Rex>,
    type: PType = PType.dynamic()
) : RexCallDynamic {

    // DO NOT USE FINAL
    private var _type: RexType = RexType(type)

    override fun getName(): String = name

    override fun getFunctions(): List<Function> = functions

    override fun getArgs(): List<Rex> = args

    override fun getType(): RexType = _type

    override fun getChildren(): Collection<Rex> = args
}

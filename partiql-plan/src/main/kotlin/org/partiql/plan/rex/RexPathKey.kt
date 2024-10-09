package org.partiql.plan.rex

/**
 * Logical operator for path lookup by key.
 */
public interface RexPathKey : Rex {

    public fun getOperand(): Rex

    public fun getKey(): Rex

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitPathKey(this, ctx)
}

/**
 * Standard internal implementation for [RexPathKey].
 */
internal class RexPathKeyImpl(operand: Rex, key: Rex, type: RexType) : RexPathKey {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _key = key
    private var _type = type

    override fun getOperand() = _operand

    override fun getKey() = _key

    override fun getType(): RexType = _type

    override fun getChildren(): Collection<Rex> = listOf(_operand, _key)
}

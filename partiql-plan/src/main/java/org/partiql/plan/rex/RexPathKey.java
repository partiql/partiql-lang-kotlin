package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * Logical operator for path lookup by key.
 */
public interface RexPathKey : Rex {

    public fun getOperand(): Rex

    public fun getKey(): Rex

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitPathKey(this, ctx)
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

   
}

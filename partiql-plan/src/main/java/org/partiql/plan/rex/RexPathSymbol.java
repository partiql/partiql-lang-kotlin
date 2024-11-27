package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * Logical operator for path lookup by symbol.
 */
public interface RexPathSymbol : Rex {

    public fun getOperand(): Rex

    public fun getSymbol(): String

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitPathSymbol(this, ctx)
}

/**
 * Standard internal implementation for [RexPathSymbol].
 */
internal class RexPathSymbolImpl(operand: Rex, symbol: String, type: RexType) : RexPathSymbol {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _symbol = symbol
    private var _type = type

    override fun getOperand() = _operand

    override fun getSymbol() = _symbol

    override fun getType(): RexType = _type

   
}

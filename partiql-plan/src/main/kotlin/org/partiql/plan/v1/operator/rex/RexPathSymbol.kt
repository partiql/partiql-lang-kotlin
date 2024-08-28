package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * Logical operator for path lookup by symbol.
 */
public interface RexPathSymbol : Rex {

    public fun getOperand(): Rex

    public fun getSymbol(): String

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitPathSymbol(this, ctx)
}

/**
 * Standard internal implementation for [RexPathSymbol].
 */
internal class RexPathSymbolImpl(root: Rex, symbol: String) : RexPathSymbol {

    // DO NOT USE FINAL
    private var _root = root
    private var _symbol = symbol

    override fun getOperand() = _root

    override fun getSymbol() = _symbol

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> = listOf(_root)
}

package org.partiql.plan.rex

/**
 * Logical path index operator.
 */
public interface RexPathIndex : Rex {

    public fun getOperand(): Rex

    public fun getIndex(): Rex

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitPathIndex(this, ctx)
}

/**
 * Standard internal implementation for [RexPathIndex].
 */
internal class RexPathIndexImpl(operand: Rex, index: Rex) : RexPathIndex {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _index = index

    override fun getOperand() = _operand

    override fun getIndex() = _index

    override fun getType(): RexType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> = listOf(_operand, _index)
}

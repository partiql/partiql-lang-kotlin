package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

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
internal class RexPathKeyImpl(root: Rex, key: Rex) : RexPathKey {

    // DO NOT USE FINAL
    private var _root = root
    private var _key = key

    override fun getOperand() = _root

    override fun getKey() = _key

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> = listOf(_root, _key)
}

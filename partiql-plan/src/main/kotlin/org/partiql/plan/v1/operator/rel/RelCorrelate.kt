package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema

/**
 * Logical operator for nested-loop joins (correlated subqueries // lateral joins).
 */
public interface RelCorrelate : Rel {

    public fun getLeft(): Rel

    public fun getRight(): Rel

    public fun getJoinType(): RelJoinType

    override fun getChildren(): Collection<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitCorrelate(this, ctx)
}

/**
 * Default [RelCorrelate] implementation.
 */
internal class RelCorrelateImpl(left: Rel, right: Rel, joinType: RelJoinType) : RelCorrelate {

    // DO NOT USE FINAL
    private var _left = left
    private var _right = right
    private var _joinType = joinType

    private var _children: List<Rel>? = null

    override fun getLeft(): Rel = _left

    override fun getRight(): Rel = _right

    override fun getJoinType(): RelJoinType = _joinType

    override fun getChildren(): Collection<Rel> {
        if (_children == null) {
            _children = listOf(_left, _right)
        }
        return _children!!
    }

    override fun getSchema(): Schema {
        TODO("Not yet implemented")
    }

    override fun isOrdered(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelCorrelate) return false
        if (_left != other.getLeft()) return false
        if (_right != other.getRight()) return false
        if (_joinType != other.getJoinType()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _left.hashCode()
        result = 31 * result + _right.hashCode()
        result = 31 * result + _joinType.hashCode()
        return result
    }
}

package org.partiql.plan.rel

/**
 * Logical `INTERSECT [ALL|DISTINCT]` operator for set (or multiset) intersection.
 */
public interface RelIntersect : Rel {

    public fun isAll(): Boolean

    public fun getLeft(): Rel

    public fun getRight(): Rel

    override fun getChildren(): Collection<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitIntersect(this, ctx)
}

/**
 * Default [RelIntersect] implementation.
 */
internal class RelIntersectImpl(left: Rel, right: Rel, isAll: Boolean) :
    RelIntersect {

    // DO NOT USE FINAL
    private var _isAll = isAll
    private var _left = left
    private var _right = right
    private var _children: List<Rel>? = null

    override fun isAll(): Boolean = _isAll

    override fun getLeft(): Rel = _left

    override fun getRight(): Rel = _right

    override fun getChildren(): Collection<Rel> {
        if (_children == null) {
            _children = listOf(_left, _right)
        }
        return _children!!
    }

    override fun getSchema(): org.partiql.plan.Schema {
        TODO("Not yet implemented")
    }

    override fun isOrdered(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelIntersect) return false
        if (_isAll != other.isAll()) return false
        if (_left != other.getLeft()) return false
        if (_right != other.getRight()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = _isAll.hashCode()
        result = 31 * result + _left.hashCode()
        result = 31 * result + _right.hashCode()
        return result
    }
}

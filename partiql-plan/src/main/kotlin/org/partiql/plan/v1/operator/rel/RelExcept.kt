package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema

/**
 * Logical `EXCEPT [ALL|DISTINCT]` operator for set (or multiset) difference.
 */
public interface RelExcept : Rel {

    public fun isAll(): Boolean

    public fun getLeft(): Rel

    public fun getRight(): Rel

    override fun getInputs(): List<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitExcept(this, ctx)
}

/**
 * Default [RelExcept] implementation.
 */
internal class RelExceptImpl(left: Rel, right: Rel, isAll: Boolean) : RelExcept {

    // DO NOT USE FINAL
    private var _isAll = isAll
    private var _left = left
    private var _right = right
    private var _inputs: List<Rel>? = null

    override fun isAll(): Boolean = _isAll

    override fun getLeft(): Rel = _left

    override fun getRight(): Rel = _right

    override fun getInputs(): List<Rel> {
        if (_inputs == null) {
            _inputs = listOf(_left, _right)
        }
        return _inputs!!
    }

    override fun getSchema(): Schema {
        TODO("Not yet implemented")
    }

    override fun isOrdered(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelExcept) return false
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

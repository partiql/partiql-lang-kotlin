package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelIntersect

/**
 * Default [RelIntersect] implementation.
 */
internal class RelIntersectImpl(left: Rel, right: Rel, isAll: Boolean) : RelIntersect {

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
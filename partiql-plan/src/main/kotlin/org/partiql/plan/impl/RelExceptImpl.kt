package org.partiql.plan.rel

/**
 * Default [RelExcept] implementation.
 */
internal class RelExceptImpl(left: Rel, right: Rel, isAll: Boolean) :
    RelExcept {

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

    override fun getType(): RelType {
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
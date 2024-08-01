package org.partiql.plan.v1.rel

/**
 * Logical `UNION [ALL|DISTINCT]` operator for set (or multiset) union.
 */
public interface RelUnion : Rel {

    public fun isAll(): Boolean

    public fun getLeft(): Rel

    public fun getRight(): Rel

    override fun getInputs(): List<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitUnion(this, ctx)

    /**
     * Default [RelUnion] implementation meant for extension.
     */
    public abstract class Base(left: Rel, right: Rel, all: Boolean) : RelUnion {

        /**
         * If neither ALL nor DISTINCT is specified, then DISTINCT is implicit (all = false).
         */
        public constructor(left: Rel, right: Rel) : this(left, right, false)

        // DO NOT USE FINAL
        private var _isAll = false
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

        override fun isOrdered(): Boolean = false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RelUnion) return false
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
}

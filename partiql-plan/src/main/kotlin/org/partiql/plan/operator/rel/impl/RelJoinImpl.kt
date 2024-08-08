package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelJoin
import org.partiql.plan.operator.rel.RelJoinType
import org.partiql.plan.operator.rex.Rex

/**
 * Default [RelJoin] implementation.
 */
internal class RelJoinImpl(left: Rel, right: Rel, condition: Rex?, type: RelJoinType) : RelJoin {

    // DO NOT USE FINAL
    private var _left = left
    private var _right = right
    private var _condition = condition
    private var _type = type

    private var _inputs: List<Rel>? = null

    override fun getLeft(): Rel = _left

    override fun getRight(): Rel = _right

    override fun getCondition(): Rex? = _condition

    override fun getType(): RelJoinType = _type

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
        if (other !is RelJoin) return false
        if (_left != other.getLeft()) return false
        if (_right != other.getRight()) return false
        if (_condition != other.getCondition()) return false
        if (_type != other.getType()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _left.hashCode()
        result = 31 * result + _right.hashCode()
        result = 31 * result + _condition.hashCode()
        result = 31 * result + _type.hashCode()
        return result
    }
}

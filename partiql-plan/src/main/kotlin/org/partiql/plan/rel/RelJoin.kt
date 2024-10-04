package org.partiql.plan.rel

import org.partiql.plan.JoinType
import org.partiql.plan.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelJoin : Rel {

    public fun getLeft(): Rel

    // TODO REMOVE ME TEMPORARY – https://github.com/partiql/partiql-lang-kotlin/issues/1575
    public fun getLeftSchema(): RelType?

    public fun getRight(): Rel

    // TODO REMOVE ME TEMPORARY – https://github.com/partiql/partiql-lang-kotlin/issues/1575
    public fun getRightSchema(): RelType?

    public fun getCondition(): Rex?

    public fun getJoinType(): JoinType

    override fun getChildren(): Collection<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitJoin(this, ctx)
}

/**
 * Default [RelJoin] implementation.
 */
internal class RelJoinImpl(
    left: Rel,
    right: Rel,
    condition: Rex?,
    joinType: JoinType,
    leftSchema: RelType?,
    rightSchema: RelType?,
) : RelJoin {

    // DO NOT USE FINAL
    private var _left = left
    private var _right = right
    private var _condition = condition
    private var _joinType = joinType
    private var _leftSchema = leftSchema
    private var _rightSchema = rightSchema

    private var _children: List<Rel>? = null

    override fun getLeft(): Rel = _left

    override fun getRight(): Rel = _right

    override fun getCondition(): Rex? = _condition

    override fun getJoinType(): JoinType = _joinType

    override fun getLeftSchema(): RelType? = _leftSchema

    override fun getRightSchema(): RelType? = _rightSchema

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
        if (other !is RelJoin) return false
        if (_left != other.getLeft()) return false
        if (_right != other.getRight()) return false
        if (_condition != other.getCondition()) return false
        if (_joinType != other.getJoinType()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _left.hashCode()
        result = 31 * result + _right.hashCode()
        result = 31 * result + _condition.hashCode()
        result = 31 * result + _joinType.hashCode()
        return result
    }
}

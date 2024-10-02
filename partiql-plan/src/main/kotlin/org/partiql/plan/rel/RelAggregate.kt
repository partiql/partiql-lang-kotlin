package org.partiql.plan.rel

import org.partiql.plan.AggregateCall
import org.partiql.plan.rex.Rex

/**
 * TODO GROUP STRATEGY
 * TODO GROUP BY
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): List<AggregateCall>

    public fun getGroups(): List<Rex>

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitAggregate(this, ctx)
}

/**
 * Default [RelAggregate] implementation.
 */
internal class RelAggregateImpl(
    input: Rel,
    calls: List<AggregateCall>,
    groups: List<Rex>,
) :
    RelAggregate {

    // DO NOT USE FINAL
    private var _input = input
    private var _calls = calls
    private var _groups = groups

    private var _children: List<Rel>? = null

    override fun getInput(): Rel = _input

    override fun getCalls(): List<AggregateCall> = _calls

    override fun getGroups(): List<Rex> = _groups

    override fun getChildren(): Collection<Rel> {
        if (_children == null) {
            _children = listOf(_input)
        }
        return _children!!
    }

    override fun getSchema(): org.partiql.plan.Schema {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelAggregate) return false
        if (_input != other.getInput()) return false
        if (_calls != other.getCalls()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _calls.hashCode()
        return result
    }
}

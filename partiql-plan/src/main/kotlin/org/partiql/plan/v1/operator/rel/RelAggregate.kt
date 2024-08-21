package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.operator.rex.Rex

/**
 * TODO GROUP STRATEGY
 * TODO GROUP BY
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): List<RelAggregateCall>

    public fun getGroups(): List<Rex>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitAggregate(this, ctx)
}

/**
 * Default [RelAggregate] implementation.
 */
internal class RelAggregateImpl(input: Rel, calls: List<RelAggregateCall>, groups: List<Rex>) : RelAggregate {

    // DO NOT USE FINAL
    private var _input = input
    private var _calls = calls
    private var _groups = groups

    private var _inputs: List<Rel>? = null

    override fun getInput(): Rel = _input

    override fun getCalls(): List<RelAggregateCall> = _calls

    override fun getGroups(): List<Rex> = _groups

    override fun getInputs(): List<Rel> {
        if (_inputs == null) {
            _inputs = listOf(_input)
        }
        return _inputs!!
    }

    override fun getSchema(): Schema {
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

package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelAggregate
import org.partiql.plan.operator.rel.RelAggregateCall

/**
 * Default [RelAggregate] implementation.
 */
internal class RelAggregateImpl(input: Rel, calls: List<RelAggregateCall>) : RelAggregate {

    private var _input = input
    private var _calls = calls

    private var _inputs: List<Rel>? = null

    override fun getInput(): Rel = _input

    override fun getCalls(): List<RelAggregateCall> = _calls

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

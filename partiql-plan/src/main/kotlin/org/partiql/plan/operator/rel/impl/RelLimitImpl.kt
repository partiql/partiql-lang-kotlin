package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelLimit
import org.partiql.plan.operator.rex.Rex

/**
 * Default [RelLimit] implementation.
 */
internal class RelLimitImpl(input: Rel, limit: Rex) : RelLimit {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _limit: Rex = limit

    override fun getInput(): Rel = _input

    override fun getLimit(): Rex = _limit

    override fun getInputs(): List<Rel> = listOf(_input)

    override fun getSchema(): Schema = _input.getSchema()

    override fun isOrdered(): Boolean = _input.isOrdered()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelLimit) return false
        if (_input != other.getInput()) return false
        if (_limit != other.getLimit()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _limit.hashCode()
        return result
    }
}

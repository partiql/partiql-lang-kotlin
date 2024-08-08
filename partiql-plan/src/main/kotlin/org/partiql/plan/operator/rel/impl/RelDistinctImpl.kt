package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelDistinct

/**
 * Default [RelDistinct] implementation.
 */
internal class RelDistinctImpl(input: Rel) : RelDistinct {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _inputs: List<Rel>? = null
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getInputs(): List<Rel> {
        if (_inputs == null) {
            _inputs = listOf(_input)
        }
        return _inputs!!
    }

    override fun getSchema(): Schema = _input.getSchema()

    override fun isOrdered(): Boolean = _ordered

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RelDistinct) return false
        return _input == other.getInput()
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }
}

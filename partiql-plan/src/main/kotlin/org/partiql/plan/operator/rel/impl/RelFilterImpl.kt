package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelFilter
import org.partiql.plan.operator.rex.Rex

/**
 * Default [RelFilter] implementation.
 */
internal class RelFilterImpl(input: Rel, predicate: Rex) : RelFilter {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _inputs: List<Rel>? = null
    private var _predicate: Rex = predicate
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getInputs(): List<Rel> {
        if (_inputs == null) {
            _inputs = listOf(_input)
        }
        return _inputs!!
    }

    override fun getPredicate(): Rex = _predicate

    override fun getSchema(): Schema = _input.getSchema()

    override fun isOrdered(): Boolean = _ordered

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelFilter) return false
        if (_input != other.getInput()) return false
        if (_predicate != other.getPredicate()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _predicate.hashCode()
        return result
    }
}

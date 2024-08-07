package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelCollation
import org.partiql.plan.operator.rel.RelSort

/**
 * Default [RelSort] implementation.
 */
internal class RelSortImpl(input: Rel, collations: List<RelCollation>) : RelSort {

    // DO NOT USE FINAL
    private var _input = input
    private var _collations = collations

    private var _inputs: List<Rel>? = null

    override fun getInput(): Rel = _input

    override fun getCollations(): List<RelCollation> = _collations

    override fun getSchema(): Schema = _input.getSchema()

    override fun getInputs(): List<Rel> {
        if (_inputs == null) {
            _inputs = listOf(_input)
        }
        return _inputs!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelSort) return false
        if (_input != other.getInput()) return false
        if (_collations != other.getCollations()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _collations.hashCode()
        return result
    }
}
package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.RelUnpivot
import org.partiql.plan.operator.rex.Rex

/**
 * Default [RelUnpivot] implementation.
 */
internal class RelUnpivotImpl(input: Rex) : RelUnpivot {

    // DO NOT USE FINAL
    private var _input: Rex = input

    override fun getInput(): Rex = _input

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RelUnpivot) return false
        return _input == other.getInput()
    }

    override fun getSchema(): Schema {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }
}

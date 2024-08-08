package org.partiql.plan.operator.rel.impl

import org.partiql.plan.Schema
import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rel.RelProject
import org.partiql.plan.operator.rex.Rex

/**
 * Default [RelProject] implementation.
 */
public class RelProjectImpl(input: Rel, projections: List<Rex>) : RelProject {

    // DO NOT USE FINAL
    private var _input = input
    private var _projections = projections

    override fun getInput(): Rel = _input

    override fun getProjections(): List<Rex> = _projections

    override fun getSchema(): Schema {
        TODO("Not yet implemented")
    }

    override fun getInputs(): List<Rel> = listOf(_input)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelProject) return false
        if (_input != other.getInput()) return false
        if (_projections != other.getProjections()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _projections.hashCode()
        return result
    }
}

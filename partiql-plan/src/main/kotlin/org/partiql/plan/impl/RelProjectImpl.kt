package org.partiql.plan.rel

import org.partiql.plan.rex.Rex

/**
 * Default [RelProject] implementation.
 */
public class RelProjectImpl(input: Rel, projections: List<Rex>) : RelProject {

    // DO NOT USE FINAL
    private var _input = input
    private var _projections = projections

    override fun getInput(): Rel = _input

    override fun getProjections(): List<Rex> = _projections

    override fun getType(): RelType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rel> = listOf(_input)

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
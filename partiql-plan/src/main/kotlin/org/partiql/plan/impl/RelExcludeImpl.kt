package org.partiql.plan.rel

import org.partiql.plan.Exclusion

/**
 * Default [RelExclude] implementation.
 */
internal class RelExcludeImpl(input: Rel, exclusions: List<Exclusion>) : RelExclude {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _exclusions: List<Exclusion> = exclusions
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getChildren(): Collection<Rel> = listOf(_input)

    override fun getExclusions(): List<Exclusion> = _exclusions

    override fun isOrdered(): Boolean = _ordered

    override fun getType(): RelType {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelExclude) return false
        if (_input != other.getInput()) return false
        if (_exclusions != other.getExclusions()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _exclusions.hashCode()
        return result
    }
}
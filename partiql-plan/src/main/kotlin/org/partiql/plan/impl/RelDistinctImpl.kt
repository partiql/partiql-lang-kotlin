package org.partiql.plan.rel

/**
 * Default [RelDistinct] implementation.
 */
internal class RelDistinctImpl(input: Rel) : RelDistinct {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _children: List<Rel>? = null
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getChildren(): Collection<Rel> {
        if (_children == null) {
            _children = listOf(_input)
        }
        return _children!!
    }

    override fun getType(): RelType = _input.getType()

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
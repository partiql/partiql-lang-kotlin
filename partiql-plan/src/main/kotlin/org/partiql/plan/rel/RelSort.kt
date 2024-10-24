package org.partiql.plan.rel

import org.partiql.plan.Collation
import org.partiql.plan.Visitor

/**
 * Logical sort operator.
 */
public interface RelSort : Rel {

    public fun getInput(): Rel

    public fun getCollations(): List<Collation>

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun getType(): RelType = getInput().getType()

    override fun isOrdered(): Boolean = true

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitSort(this, ctx)
}

/**
 * Default [RelSort] implementation.
 */
internal class RelSortImpl(input: Rel, collations: List<Collation>) : RelSort {

    // DO NOT USE FINAL
    private var _input = input
    private var _collations = collations

    private var _children: List<Rel>? = null

    override fun getInput(): Rel = _input

    override fun getCollations(): List<Collation> = _collations

    override fun getType(): RelType = _input.getType()

    override fun getChildren(): Collection<Rel> {
        if (_children == null) {
            _children = listOf(_input)
        }
        return _children!!
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

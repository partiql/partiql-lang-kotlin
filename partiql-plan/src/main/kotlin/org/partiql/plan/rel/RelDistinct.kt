package org.partiql.plan.rel

/**
 * Logical `DISTINCT` operator.
 */
public interface RelDistinct : Rel {

    public fun getInput(): Rel

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun getSchema(): org.partiql.plan.Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitDistinct(this, ctx)
}

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

    override fun getSchema(): org.partiql.plan.Schema = _input.getSchema()

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

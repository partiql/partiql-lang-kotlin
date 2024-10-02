package org.partiql.plan.rel

import org.partiql.plan.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelUnpivot : Rel {

    public fun getInput(): Rex

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitUnpivot(this, ctx)
}

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

    override fun getSchema(): org.partiql.plan.Schema {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }
}

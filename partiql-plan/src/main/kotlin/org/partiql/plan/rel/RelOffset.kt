package org.partiql.plan.rel

import org.partiql.plan.rex.Rex

/**
 * Logical `OFFSET` operator.
 */
public interface RelOffset : Rel {

    public fun getInput(): Rel

    public fun getOffset(): Rex

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun getSchema(): org.partiql.plan.Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitOffset(this, ctx)
}

/**
 * Default [RelOffset] implementation.
 */
internal class RelOffsetImpl(input: Rel, offset: Rex) : RelOffset {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _offset: Rex = offset

    override fun getInput(): Rel = _input

    override fun getOffset(): Rex = _offset

    override fun getChildren(): Collection<Rel> = listOf(_input)

    override fun getSchema(): org.partiql.plan.Schema = _input.getSchema()

    override fun isOrdered(): Boolean = _input.isOrdered()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelOffset) return false
        if (_input != other.getInput()) return false
        if (_offset != other.getOffset()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _offset.hashCode()
        return result
    }
}

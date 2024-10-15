package org.partiql.plan.rel

import org.partiql.plan.Visitor
import org.partiql.plan.rex.Rex

/**
 * Logical `LIMIT` operator.
 */
public interface RelLimit : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun getType(): RelType = getInput().getType()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitLimit(this, ctx)
}

/**
 * Default [RelLimit] implementation.
 */
internal class RelLimitImpl(input: Rel, limit: Rex) : RelLimit {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _limit: Rex = limit

    override fun getInput(): Rel = _input

    override fun getLimit(): Rex = _limit

    override fun getChildren(): Collection<Rel> = listOf(_input)

    override fun getType(): RelType = _input.getType()

    override fun isOrdered(): Boolean = _input.isOrdered()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelLimit) return false
        if (_input != other.getInput()) return false
        if (_limit != other.getLimit()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _limit.hashCode()
        return result
    }
}

package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.operator.rex.Rex

/**
 * Logical `LIMIT` operator.
 */
public interface RelLimit : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitLimit(this, ctx)
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

    override fun getInputs(): List<Rel> = listOf(_input)

    override fun getSchema(): Schema = _input.getSchema()

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

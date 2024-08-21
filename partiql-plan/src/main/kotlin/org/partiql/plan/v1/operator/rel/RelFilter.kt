package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.operator.rex.Rex

/**
 * Logical filter operation for the WHERE and HAVING clauses.
 */
public interface RelFilter : Rel {

    public fun getInput(): Rel

    public fun getPredicate(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitFilter(this, ctx)
}

/**
 * Default [RelFilter] implementation.
 */
internal class RelFilterImpl(input: Rel, predicate: Rex) : RelFilter {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _inputs: List<Rel>? = null
    private var _predicate: Rex = predicate
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getInputs(): List<Rel> {
        if (_inputs == null) {
            _inputs = listOf(_input)
        }
        return _inputs!!
    }

    override fun getPredicate(): Rex = _predicate

    override fun getSchema(): Schema = _input.getSchema()

    override fun isOrdered(): Boolean = _ordered

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelFilter) return false
        if (_input != other.getInput()) return false
        if (_predicate != other.getPredicate()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _predicate.hashCode()
        return result
    }
}

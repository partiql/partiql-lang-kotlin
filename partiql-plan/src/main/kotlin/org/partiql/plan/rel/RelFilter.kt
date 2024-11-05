package org.partiql.plan.rel

import org.partiql.plan.Visitor
import org.partiql.plan.rex.Rex

/**
 * Logical filter operation for the WHERE and HAVING clauses.
 *
 *   arg 0 – rel input
 *   arg 1 - rex predicate
 */
public interface RelFilter : Rel {

    public fun getInput(): Rel

    public fun getPredicate(): Rex

    override fun getChildren(): Collection<Rel> = listOf(getInput())

    override fun getType(): RelType = getInput().getType()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R =
        visitor.visitFilter(this, ctx)
}

/**
 * Default [RelFilter] implementation.
 */
internal class RelFilterImpl(input: Rel, predicate: Rex) : RelFilter {

    // DO NOT USE FINAL
    private var _input: Rel = input
    private var _children: List<Rel>? = null
    private var _predicate: Rex = predicate
    private var _ordered: Boolean = input.isOrdered()

    override fun getInput(): Rel = _input

    override fun getChildren(): Collection<Rel> {
        if (_children == null) {
            _children = listOf(_input)
        }
        return _children!!
    }

    override fun getPredicate(): Rex = _predicate

    override fun getType(): RelType = _input.getType()

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

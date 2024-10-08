package org.partiql.plan.rex

import org.partiql.plan.rel.Rel

public interface RexSubqueryIn : Rex {

    public fun getArgs(): List<Rex>

    public fun getRel(): Rel

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubqueryIn(this, ctx)
}

/**
 * Logical operator for SQL subquery comparisons.
 *
 *  - <comparison predicate> for subqueries.
 *  - <quantified comparison predicate>.
 */
internal class RexSubqueryInImpl(args: List<Rex>, rel: Rel) : RexSubqueryIn {

    private var _args = args
    private var _rel = rel

    override fun getType(): RexType = TODO("Not yet implemented")

    override fun getArgs(): List<Rex> = _args

    override fun getRel(): Rel = _rel

    override fun getChildren(): Collection<Rex> = _args

    // TODO hashcode/equals?
}

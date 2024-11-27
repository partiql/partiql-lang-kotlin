package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.plan.rel.Rel

public interface RexSubqueryIn : Rex {

    public fun getArgs(): List<Rex>

    public fun getRel(): Rel

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitSubqueryIn(this, ctx)
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

   

    // TODO hashcode/equals?
}

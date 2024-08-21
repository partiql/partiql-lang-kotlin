package org.partiql.plan.v1.operator.rex

import org.partiql.plan.v1.operator.rel.Rel
import org.partiql.types.PType

public interface RexSubqueryIn : Rex {

    public fun getArgs(): List<Rex>

    public fun getRel(): Rel

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubqueryIn(this, ctx)
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

    override fun getType(): PType = TODO("Not yet implemented")

    override fun getArgs(): List<Rex> = _args

    override fun getRel(): Rel = _rel

    override fun getOperands(): List<Rex> = _args

    // TODO hashcode/equals?
}

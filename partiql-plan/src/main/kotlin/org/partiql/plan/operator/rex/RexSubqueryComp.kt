package org.partiql.plan.operator.rex

import org.partiql.plan.operator.rel.Rel
import org.partiql.plan.operator.rex.RexSubqueryComp.Comp
import org.partiql.plan.operator.rex.RexSubqueryComp.Quantifier
import org.partiql.types.PType

/**
 * Logical operator for SQL subquery comparisons.
 *
 *  - <comparison predicate> for subqueries.
 *  - <quantified comparison predicate>.
 */
public interface RexSubqueryComp : Rex {

    public fun getArgs(): List<Rex>

    public fun getComp(): Comp

    public fun getQuantifier(): Quantifier?

    public fun getRel(): Rel

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubqueryComp(this, ctx)

    /**
     * SQL <comp op> for use in the <quantified comparison predicate>.
     */
    public enum class Comp {
        EQ,
        NE,
        LT,
        LE,
        GT,
        GE,
        OTHER;
    }

    /**
     * SQL <quantifier> for use in the <quantified comparison predicate>.
     */
    public enum class Quantifier {
        ANY,
        ALL,
        SOME,
        OTHER;
    }
}

/**
 * Logical operator for SQL subquery comparisons.
 *
 *  - <comparison predicate> for subqueries.
 *  - <quantified comparison predicate>.
 */
internal class RexSubqueryCompImpl(
    args: List<Rex>,
    comp: Comp,
    quantifier: Quantifier?,
    rel: Rel,
) : RexSubqueryComp {

    private var _args = args
    private var _comp = comp
    private var _quantifier = quantifier
    private var _rel = rel

    override fun getType(): PType = TODO("Not yet implemented")

    override fun getArgs(): List<Rex> = _args

    override fun getComp(): Comp = _comp

    override fun getQuantifier(): Quantifier? = _quantifier

    override fun getRel(): Rel = _rel

    override fun getOperands(): List<Rex> = _args

    // TODO hashcode/equals?
}

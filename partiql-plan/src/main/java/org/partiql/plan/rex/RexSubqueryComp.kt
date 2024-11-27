package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.plan.rel.Rel
import org.partiql.plan.rex.RexSubqueryComp.Comp
import org.partiql.plan.rex.RexSubqueryComp.Quantifier

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

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitSubqueryComp(this, ctx)

    /**
     * SQL <comp op> for use in the <quantified comparison predicate>.
     *
     * TODO transition to 1.0 enums.
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
     *
     * TODO transition to 1.0 enums.
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

    override fun getType(): RexType = TODO("Not yet implemented")

    override fun getArgs(): List<Rex> = _args

    override fun getComp(): Comp = _comp

    override fun getQuantifier(): Quantifier? = _quantifier

    override fun getRel(): Rel = _rel

    override fun getChildren(): Collection<Rex> = _args

    // TODO hashcode/equals?
}

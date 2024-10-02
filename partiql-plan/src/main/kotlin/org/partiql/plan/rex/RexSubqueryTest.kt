package org.partiql.plan.rex

import org.partiql.plan.rex.RexSubqueryTest.Test
import org.partiql.types.PType

/**
 * Logical expression for subquery tests EXISTS and UNIQUE.
 *
 *  - <exists predicate>    "Specify a test for a non-empty set."
 *  - <unique predicate>    "Specify a test for the absence of duplicate rows."
 */
public interface RexSubqueryTest : Rex {

    public fun getTest(): Test

    public fun getRel(): org.partiql.plan.rel.Rel

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubqueryTest(this, ctx)

    /**
     * EXISTS and UNIQUE are defined by SQL.
     *
     * TODO use 1.0 enum modeling.
     */
    public enum class Test {
        EXISTS,
        UNIQUE,
        OTHER;
    }
}

/**
 * Logical operator for SQL subquery comparisons.
 *
 *  - <comparison predicate> for subqueries.
 *  - <quantified comparison predicate>.
 */
internal class RexSubqueryTestImpl(test: Test, rel: org.partiql.plan.rel.Rel) : RexSubqueryTest {

    private var _test = test
    private var _rel = rel

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun getTest(): Test = _test

    override fun getRel(): org.partiql.plan.rel.Rel = _rel

    override fun getChildren(): Collection<Rex> = emptyList()

    // TODO hashcode/equals?
}

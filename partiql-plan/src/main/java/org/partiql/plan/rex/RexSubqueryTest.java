package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical subquery test expression abstract base class.
 * <br>
 * <pre>
 *  - <exists predicate>    "Specify a test for a non-empty set."
 *  - <unique predicate>    "Specify a test for the absence of duplicate rows."
 * </pre>
 */
public abstract class RexSubqueryTest extends RexBase {

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return subquery test
     */
    @NotNull
    public abstract Test getTest();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.bool());
    }

    @Override
    protected List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSubqueryTest(this, ctx);
    }

    /**
     * EXISTS and UNIQUE are defined by SQL.
     * <p>
     * TODO use 1.0 enum modeling.
     */
    public enum Test {
        EXISTS,
        UNIQUE,
        OTHER;
    }
}

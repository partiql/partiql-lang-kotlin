package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;
import org.partiql.spi.Enum;
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
     */
    public static class Test extends Enum {

        public static final int UKNOWNN = 0;
        public static final int EXISTS = 1;
        public static final int UNIQUE = 2;

        private Test(int code) {
            super(code);
        }

        @NotNull
        public static Test EXISTS() {
            return new Test(EXISTS);
        }

        @NotNull
        public static Test UNIQUE() {
            return new Test(UNIQUE);
        }
    }
}

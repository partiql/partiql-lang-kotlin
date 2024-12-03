package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
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
     * @return new RexSubqueryTest instance
     */
    @NotNull
    public static RexSubqueryTest create(@NotNull Rel input, @NotNull Test test) {
        return new Impl(input, test);
    }

    /**
     * @return input rel (operand 0)
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
        return RexType.of(PType.bool());
    }

    @Override
    protected List<Operator> operands() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitSubqueryTest(this, ctx);
    }

    /**
     * EXISTS and UNIQUE are defined by SQL.
     */
    public static class Test extends Enum {

        public static final int UNKNOWN = 0;
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

    private static class Impl extends RexSubqueryTest {

        private final Rel input;
        private final Test test;

        private Impl(Rel input, Test test) {
            this.input = input;
            this.test = test;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Test getTest() {
            return test;
        }
    }
}

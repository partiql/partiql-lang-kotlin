package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical subquery test expression abstract base class. This is used for the EXISTS and UNIQUE predicates.
 * <ul>
 * <li>EXISTS: "Specify a test for a non-empty set." ({@code <exists predicate>})</li>
 * <li>UNIQUE: "Specify a test for the absence of duplicate rows." ({@code <unique predicate>})</li>
 * </ul>
 * @see Test
 */
public abstract class RexSubqueryTest extends RexBase {

    /**
     * Creates a new RexSubqueryTest instance.
     * @param input input rel (operand 0)
     * @param test subquery test
     * @return new RexSubqueryTest instance
     */
    @NotNull
    public static RexSubqueryTest create(@NotNull Rel input, @NotNull Test test) {
        return new Impl(input, test);
    }

    /**
     * Gets the input rel.
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Gets the subquery test.
     * @return subquery test
     */
    @NotNull
    public abstract Test getTest();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.bool());
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
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

        public static final int EXISTS = 1;
        public static final int UNIQUE = 2;

        private Test(int code) {
            super(code);
        }

        @NotNull
        @Override
        public String name() throws UnsupportedCodeException {
            int code = code();
            switch (code) {
                case EXISTS:
                    return "EXISTS";
                case UNIQUE:
                    return "UNIQUE";
                default:
                    throw new UnsupportedCodeException(code);
            }
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

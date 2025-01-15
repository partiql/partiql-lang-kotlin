package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical subquery in expression abstract base class. This is for the {@code IN} predicate.
 */
public abstract class RexSubqueryIn extends RexBase {

    /**
     * Creates a new RexSubqueryIn instance.
     * @param input input rel (operand 0)
     * @param args  collection comparison arguments (not operands).
     * @return new RexSubqueryIn instance
     */
    @NotNull
    public static RexSubqueryIn create(@NotNull Rel input, @NotNull List<Rex> args) {
        return new Impl(input, args);
    }

    /**
     * Get the input rel (operand 0).
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Gets the collection comparison arguments (not operands).
     * @return collection comparison arguments (not operands).
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.bool());
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitSubqueryIn(this, ctx);
    }

    private static class Impl extends RexSubqueryIn {

        private final Rel input;
        private final List<Rex> args;

        private Impl(Rel input, List<Rex> args) {
            this.input = input;
            this.args = args;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}

package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical subquery in expression abstract base class.
 */
public abstract class RexSubqueryIn extends RexBase {

    /**
     * @return new RexSubqueryIn instance
     */
    @NotNull
    public static RexSubqueryIn create(@NotNull Rel input, @NotNull List<Rex> args) {
        return new Impl(input, args);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return collection comparison arguments (not operands).
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @Override
    protected final RexType type() {
        return RexType.of(PType.bool());
    }

    @Override
    protected final List<Operator> operands() {
        Rel c0 = getInput();
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

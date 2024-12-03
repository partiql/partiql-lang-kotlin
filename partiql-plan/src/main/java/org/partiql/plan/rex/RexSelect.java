package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical select expression abstract base class.
 */
public abstract class RexSelect extends RexBase {

    /**
     * @return new RexSelect instance
     */
    @NotNull
    public static RexSelect create(@NotNull Rel input, @NotNull Rex constructor) {
        return new Impl(input, constructor);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return constructor rex (operand 1)
     */
    public abstract Rex getConstructor();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.bag());
    }

    @NotNull
    @Override
    protected final List<Operator> operands() {
        Rel c0 = getInput();
        Rex c1 = getConstructor();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelect(this, ctx);
    }

    private static class Impl extends RexSelect {

        private final Rel input;
        private final Rex constructor;

        private Impl(Rel input, Rex constructor) {
            this.input = input;
            this.constructor = constructor;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Rex getConstructor() {
            return constructor;
        }
    }
}

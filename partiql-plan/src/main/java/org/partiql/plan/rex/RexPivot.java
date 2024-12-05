package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical pivot expression abstract base class.
 */
public abstract class RexPivot extends RexBase {

    /**
     * @return new RexPivot instance
     */
    @NotNull
    public static RexPivot create(@NotNull Rel input, @NotNull Rex key, @NotNull Rex value) {
        return new Impl(input, key, value);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return key rex (operand 1)
     */
    @NotNull
    public abstract Rex getKey();

    /**
     * @return value rex (operand 2)
     */
    @NotNull
    public abstract Rex getValue();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.struct());
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitPivot(this, ctx);
    }

    private static class Impl extends RexPivot {

        private final Rel input;
        private final Rex key;
        private final Rex value;

        private Impl(Rel input, Rex key, Rex value) {
            this.input = input;
            this.key = key;
            this.value = value;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Rex getKey() {
            return key;
        }

        @NotNull
        @Override
        public Rex getValue() {
            return value;
        }
    }
}

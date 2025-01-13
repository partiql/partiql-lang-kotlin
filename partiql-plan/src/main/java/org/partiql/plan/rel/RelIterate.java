package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v> AT <i>`.
 */
public abstract class RelIterate extends RelBase {

    /**
     * Creates a new {@link RelIterate} instance.
     *
     * @param rex input rex (operand 0)
     * @return new {@link RelIterate} instance
     */
    @NotNull
    public static RelIterate create(@NotNull Rex rex) {
        return new Impl(rex);
    }

    /**
     * Gets the input rex.
     * @return input rex (operand 0)
     */
    @NotNull
    public abstract Rex getRex();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getRex());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitIterate(this, ctx);
    }

    @NotNull
    public abstract RelIterate copy(@NotNull Rex rex);

    private static class Impl extends RelIterate {

        private final Rex rex;

        private Impl(Rex rex) {
            this.rex = rex;
        }

        @NotNull
        @Override
        public Rex getRex() {
            return rex;
        }

        @NotNull
        @Override
        public RelIterate copy(@NotNull Rex rex) {
            return new Impl(rex);
        }
    }
}

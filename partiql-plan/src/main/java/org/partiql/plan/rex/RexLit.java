package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.value.Datum;

import java.util.List;

/**
 * Literal value expression abstract base class.
 */
public abstract class RexLit extends RexBase {

    /**
     * @return new RexLit instance
     */
    @NotNull
    public static RexLit create(@NotNull Datum value) {
        return new Impl(value);
    }

    @NotNull
    public abstract Datum getDatum();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(getDatum().getType());
    }

    @Override
    protected List<Operator> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitLit(this, ctx);
    }

    private static class Impl extends RexLit {

        private final Datum value;

        private Impl(Datum value) {
            this.value = value;
        }

        @NotNull
        @Override
        public Datum getDatum() {
            return value;
        }
    }
}

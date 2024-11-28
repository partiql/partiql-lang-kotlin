package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical nullif expression abstraction base class.
 */
public abstract class RexNullIf extends RexBase {

    /**
     * @return new RexNullIf instance
     */
    @NotNull
    public static RexNullIf create(@NotNull Rex v1, @NotNull Rex v2) {
        return new Impl(v1, v2);
    }

    /**
     * @return v1 rex (child 0)
     */
    @NotNull
    public abstract Rex getV1();

    /**
     * @return v2 rex (child 1)
     */
    @NotNull
    public abstract Rex getV2();

    @NotNull
    @Override
    protected final RexType type() {
        return getV1().getType();
    }

    @Override
    protected final List<Operator> children() {
        Rex c0 = getV1();
        Rex c1 = getV2();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitNullIf(this, ctx);
    }

    private static class Impl extends RexNullIf {

        private final Rex v1;
        private final Rex v2;

        private Impl(Rex v1, Rex v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @NotNull
        @Override
        public Rex getV1() {
            return v1;
        }

        @NotNull
        @Override
        public Rex getV2() {
            return v2;
        }
    }
}

package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical bag expression abstract base class.
 */
public abstract class RexBag extends RexBase {

    /**
     * @return new RexBag instance
     */
    @NotNull
    public static RexBag create(@NotNull List<Rex> values) {
        return new Impl(values);
    }

    /**
     * @return the values of the array, also the children.
     */
    @NotNull
    public abstract List<Rex> getValues();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.bag());
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        List<Rex> varargs = getValues();
        return List.copyOf(varargs);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitBag(this, ctx);
    }

    private static class Impl extends RexBag {

        private final List<Rex> values;

        private Impl(List<Rex> values) {
            this.values = values;
        }

        @NotNull
        @Override
        public List<Rex> getValues() {
            return values;
        }
    }
}

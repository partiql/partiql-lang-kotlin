package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.types.PType;

import java.util.Collection;
import java.util.List;

/**
 * Logical bag expression abstract base class.
 */
public abstract class RexBag extends RexBase {

    /**
     * @return new RexBag instance
     */
    @NotNull
    public static RexBag create(@NotNull Collection<Rex> values) {
        return new Impl(values);
    }

    /**
     * @return the values of the bag, also the operands (unordered).
     */
    @NotNull
    public abstract Collection<Rex> getValues();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.bag());
    }

    @NotNull
    @Override
    protected final List<Operator> operands() {
        List<Rex> varargs = getValues().stream().toList();
        return List.copyOf(varargs);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitBag(this, ctx);
    }

    private static class Impl extends RexBag {

        private final Collection<Rex> values;

        private Impl(Collection<Rex> values) {
            this.values = values;
        }

        @NotNull
        @Override
        public Collection<Rex> getValues() {
            return values;
        }
    }
}

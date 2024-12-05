package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical array expression abstract base class.
 */
public abstract class RexArray extends RexBase {

    /**
     * @return new RexArray instance
     */
    @NotNull
    public static RexArray create(@NotNull List<Rex> values) {
        return new Impl(values);
    }

    /**
     * @return the values of the array, also the operands.
     */
    @NotNull
    public abstract List<Rex> getValues();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.array());
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.vararg(getValues());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitArray(this, ctx);
    }

    private static class Impl extends RexArray {

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

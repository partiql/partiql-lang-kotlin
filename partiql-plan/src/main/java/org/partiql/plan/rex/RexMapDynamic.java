package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical map expression with dynamic typing (types deferred to evaluation).
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
public abstract class RexMapDynamic extends RexBase {

    /**
     * Creates a new dynamic map expression.
     * @param entries list of map entries (key-value pairs)
     * @return new RexMapDynamic instance
     */
    @NotNull
    public static RexMapDynamic create(@NotNull List<RexMap.Entry> entries) {
        return new Impl(entries);
    }

    /**
     * Gets the map entries.
     * @return list of map entries
     */
    @NotNull
    public abstract List<RexMap.Entry> getEntries();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.dynamic());
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitMapDynamic(this, ctx);
    }

    private static class Impl extends RexMapDynamic {

        @NotNull
        private final List<RexMap.Entry> entries;

        private Impl(@NotNull List<RexMap.Entry> entries) {
            this.entries = entries;
        }

        @Override
        @NotNull
        public List<RexMap.Entry> getEntries() {
            return entries;
        }
    }
}

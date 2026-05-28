package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * A dynamic function dispatch by reference, resolved lazily at execution time.
 * <p>
 * Holds the candidate function IDs whose signatures were baked into the plan at compile time.
 * At execution, the VM resolves these IDs to actual function implementations and performs
 * runtime type-based dispatch.
 */
public abstract class RexDispatchRef extends RexBase {

    /**
     * Creates a new RexDispatchRef instance.
     *
     * @param name      the function name
     * @param catalogId the catalog identifier assigned during planning
     * @param fnIds     the candidate function identifiers within the catalog
     * @param args      the function arguments
     * @return new RexDispatchRef instance
     */
    @NotNull
    public static RexDispatchRef create(@NotNull String name, int catalogId, @NotNull List<Integer> fnIds, @NotNull List<Rex> args) {
        return new Impl(name, catalogId, fnIds, args);
    }

    /**
     * Returns the function name.
     * @return function name
     */
    @NotNull
    public abstract String getName();

    /**
     * Returns the catalog identifier.
     * @return catalog identifier
     */
    public abstract int getCatalogId();

    /**
     * Returns the candidate function identifiers within the catalog.
     * @return candidate function identifiers
     */
    @NotNull
    public abstract List<Integer> getFnIds();

    /**
     * Returns the list of function arguments.
     * @return the list of function arguments
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.dynamic());
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.vararg(getArgs());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitDispatchRef(this, ctx);
    }

    private static class Impl extends RexDispatchRef {

        private final String name;
        private final int catalogId;
        private final List<Integer> fnIds;
        private final List<Rex> args;

        private Impl(String name, int catalogId, List<Integer> fnIds, List<Rex> args) {
            this.name = name;
            this.catalogId = catalogId;
            this.fnIds = fnIds;
            this.args = args;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getCatalogId() {
            return catalogId;
        }

        @NotNull
        @Override
        public List<Integer> getFnIds() {
            return fnIds;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}

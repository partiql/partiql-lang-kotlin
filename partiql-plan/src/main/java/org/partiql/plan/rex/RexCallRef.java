package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * A statically-resolved scalar function call by reference, resolved lazily at execution time.
 */
public abstract class RexCallRef extends RexBase {

    /**
     * Creates a new RexCallRef instance.
     *
     * @param catalogId  the catalog identifier assigned during planning
     * @param fnId       the function identifier within the catalog
     * @param args       the arguments to the function
     * @param returnType the return type of the function
     * @return new RexCallRef instance
     */
    @NotNull
    public static RexCallRef create(int catalogId, int fnId, @NotNull List<Rex> args, @NotNull PType returnType) {
        return new Impl(catalogId, fnId, args, returnType);
    }

    /**
     * Returns the catalog identifier.
     * @return catalog identifier
     */
    public abstract int getCatalogId();

    /**
     * Returns the function identifier within the catalog.
     * @return function identifier
     */
    public abstract int getFnId();

    /**
     * Returns the list of function arguments.
     * @return the list of function arguments
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected List<Operand> operands() {
        Operand c0 = Operand.vararg(getArgs());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitCallRef(this, ctx);
    }

    private static class Impl extends RexCallRef {

        private final int catalogId;
        private final int fnId;
        private final List<Rex> args;
        private final PType returnType;

        private Impl(int catalogId, int fnId, List<Rex> args, PType returnType) {
            this.catalogId = catalogId;
            this.fnId = fnId;
            this.args = args;
            this.returnType = returnType;
        }

        @NotNull
        @Override
        protected RexType type() {
            return RexType.of(returnType);
        }

        @Override
        public int getCatalogId() {
            return catalogId;
        }

        @Override
        public int getFnId() {
            return fnId;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}

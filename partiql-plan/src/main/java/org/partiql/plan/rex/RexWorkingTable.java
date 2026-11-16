package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * <p>
 * <b>NOTE:</b> This is experimental and subject to change without prior notice!
 * </p>
 * <p>
 * Logical working table reference abstract base class. This is the recursive self-reference of a recursive WITH list
 * element, and it evaluates to the rows produced by the previous iteration of the enclosing
 * {@link org.partiql.plan.rel.RelRecursiveUnion} having the same id.
 * </p>
 */
public abstract class RexWorkingTable extends RexBase {

    /**
     * @param id identifier of the enclosing {@link org.partiql.plan.rel.RelRecursiveUnion}.
     * @param name the WITH list element (query) name.
     * @param type the type of the working table (a collection).
     * @return new {@link RexWorkingTable} instance
     */
    @NotNull
    public static RexWorkingTable create(int id, @NotNull String name, @NotNull PType type) {
        return new Impl(id, name, type);
    }

    /**
     * Returns the identifier of the enclosing {@link org.partiql.plan.rel.RelRecursiveUnion}.
     * @return the recursion identifier.
     */
    public abstract int getId();

    /**
     * Returns the WITH list element (query) name.
     * @return the query name.
     */
    @NotNull
    public abstract String getName();

    @NotNull
    @Override
    protected final List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitWorkingTable(this, ctx);
    }

    private static class Impl extends RexWorkingTable {

        private final int id;
        private final String name;
        private final PType type;

        private Impl(int id, String name, PType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        @NotNull
        @Override
        protected RexType type() {
            return RexType.of(type);
        }

        @Override
        public int getId() {
            return id;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }
}

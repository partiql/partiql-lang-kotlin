package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * <p>
 * <b>NOTE:</b> This is experimental and subject to change without prior notice!
 * </p>
 * <p>
 * Logical recursive union abstract base class. This is the body of a recursive WITH list element (recursive CTE),
 * {@code seed UNION [ALL] recursive}. The recursive operand references the working table via
 * {@link org.partiql.plan.rex.RexWorkingTable} with the same id.
 * </p>
 * <p>
 * Semantics (SQL:2023 Part 2, Subclause 7.17, General Rule 2.c.ii): starting from an empty working table T, T is
 * repeatedly replaced by {@code seed UNION [ALL] recursive(T)} until the cardinality of T does not grow; the result is
 * the final T. Implementations may evaluate this incrementally (evaluating the recursive operand only against the rows
 * produced by the previous iteration) when that is equivalent, e.g. when the recursive operand does not eliminate
 * duplicates.
 * </p>
 */
public abstract class RelRecursiveUnion extends RelBase {

    /**
     * @param id unique identifier of this recursion, referenced by {@link org.partiql.plan.rex.RexWorkingTable}.
     * @param name the WITH list element (query) name.
     * @param seed the non-recursive operand (operand 0).
     * @param recursive the recursive operand (operand 1).
     * @param all true if UNION ALL else UNION DISTINCT.
     * @return new {@link RelRecursiveUnion} instance
     */
    @NotNull
    public static RelRecursiveUnion create(int id, @NotNull String name, @NotNull Rel seed, @NotNull Rel recursive, boolean all) {
        return new Impl(id, name, seed, recursive, all);
    }

    /**
     * Returns the unique identifier of this recursion; this is referenced by {@link org.partiql.plan.rex.RexWorkingTable}.
     * @return the recursion identifier.
     */
    public abstract int getId();

    /**
     * Returns the WITH list element (query) name.
     * @return the query name.
     */
    @NotNull
    public abstract String getName();

    /**
     * Whether this {@link RelRecursiveUnion} set quantifier is ALL else DISTINCT.
     * @return true if ALL else DISTINCT.
     */
    public abstract boolean isAll();

    /**
     * Returns the non-recursive (seed) operand (operand 0).
     * @return seed rel (operand 0)
     */
    @NotNull
    public abstract Rel getSeed();

    /**
     * Returns the recursive operand (operand 1).
     * @return recursive rel (operand 1)
     */
    @NotNull
    public abstract Rel getRecursive();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getSeed());
        Operand c1 = Operand.single(getRecursive());
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitRecursiveUnion(this, ctx);
    }

    @NotNull
    public abstract RelRecursiveUnion copy(@NotNull Rel seed, @NotNull Rel recursive);

    private static class Impl extends RelRecursiveUnion {

        private final int id;
        private final String name;
        private final Rel seed;
        private final Rel recursive;
        private final boolean all;

        private Impl(int id, String name, Rel seed, Rel recursive, boolean all) {
            this.id = id;
            this.name = name;
            this.seed = seed;
            this.recursive = recursive;
            this.all = all;
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

        @Override
        public boolean isAll() {
            return all;
        }

        @NotNull
        @Override
        public Rel getSeed() {
            return seed;
        }

        @NotNull
        @Override
        public Rel getRecursive() {
            return recursive;
        }

        @NotNull
        @Override
        public RelRecursiveUnion copy(@NotNull Rel seed, @NotNull Rel recursive) {
            return new Impl(id, name, seed, recursive, all);
        }
    }
}

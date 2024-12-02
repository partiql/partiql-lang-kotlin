package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical case (switch) expression abstract base class.
 */
public abstract class RexCase extends RexBase {

    @NotNull
    public static RexCase create(@Nullable Rex match, @NotNull List<Branch> branches, @Nullable Rex def) {
        return new Impl(match, branches, def);
    }

    /**
     * @return the match expression, or {@code null} if none (operand 0)
     */
    @Nullable
    public abstract Rex getMatch();

    /**
     * @return the list of branches (not operands).
     */
    @NotNull
    public abstract List<Branch> getBranches();

    /**
     * @return the default expression, or {@code null} if none (operand 1)
     */
    @Nullable
    public abstract Rex getDefault();

    @NotNull
    @Override
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected List<Operator> operands() {
        Rex c0 = getMatch();
        Rex c1 = getDefault();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitCase(this, ctx);
    }

    /**
     * A branch of a case expression.
     */
    public static class Branch {

        @NotNull
        private final Rex condition;

        @NotNull
        private final Rex result;

        public Branch(@NotNull Rex condition, @NotNull Rex result) {
            this.condition = condition;
            this.result = result;
        }

        @NotNull
        public Rex getCondition() {
            return condition;
        }

        @NotNull
        public Rex getResult() {
            return result;
        }
    }

    private static class Impl extends RexCase {
        private final Rex match;
        private final List<Branch> branches;
        private final Rex def;

        private Impl(Rex match, List<Branch> branches, Rex def) {
            this.match = match;
            this.branches = branches;
            this.def = def;
        }

        @Nullable
        @Override
        public Rex getMatch() {
            return match;
        }

        @NotNull
        @Override
        public List<Branch> getBranches() {
            return branches;
        }

        @Nullable
        @Override
        public Rex getDefault() {
            return def;
        }
    }
}

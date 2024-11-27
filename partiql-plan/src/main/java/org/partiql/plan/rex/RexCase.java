package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical case (switch) expression abstract base class.
 */
public abstract class RexCase extends RexBase {

    /**
     * @return the match expression, or {@code null} if none (child 0)
     */
    @Nullable
    public abstract Rex getMatch();

    /**
     * @return the list of branches (not children).
     */
    @NotNull
    public abstract List<Branch> getBranches();

    /**
     * @return the default expression, or {@code null} if none (child 1)
     */
    @Nullable
    public abstract Rex getDefault();

    @NotNull
    @Override
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected List<Operator> children() {
        Rex c0 = getMatch();
        Rex c1 = getDefault();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
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
}
